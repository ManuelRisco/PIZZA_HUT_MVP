package com.example.services;

import com.example.dtos.BoletaEmisionDTO;
import com.example.dtos.DniLookupDTO;
import com.example.dtos.NotaCreditoEmisionDTO;
import com.example.dtos.OrderCompleteDTO;
import com.example.dtos.RucLookupDTO;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;
import com.example.models.Address;
import com.example.models.Order;
import com.example.models.Usuario;
import com.example.repositories.OrderRepository;
import com.example.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SunatService {

    private static final Logger log = LoggerFactory.getLogger(SunatService.class);
    private static final String DEFAULT_SERIE = "BBB1";
    private static final String DEFAULT_FACTURA_SERIE = "FFF1";
    private static final String DEFAULT_NC_SERIE = "FFF1";
    private static final int TIPO_COMPROBANTE_FACTURA = 1;
    private static final int TIPO_COMPROBANTE_BOLETA = 2;
    private static final int TIPO_COMPROBANTE_NOTA_CREDITO = 3;
    private static final int TIPO_NC_ANULACION_OPERACION = 1;
    private static final String UNIDAD_MEDIDA_BIEN = "NIU";
    private static final String UNIDAD_MEDIDA_SERVICIO = "ZZ";
    private static final BigDecimal IGV_DIVISOR = new BigDecimal("1.18");
    private static final DateTimeFormatter NUBEFACT_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private record RawBoletaLine(
            String unidadMedida,
            String codigo,
            String descripcion,
            int cantidad,
            BigDecimal rawLineTotal
    ) {}

    private final JdbcTemplate jdbcTemplate;
    private final OrderRepository orderRepository;
    private final UsuarioRepository usuarioRepository;
    private final AddressService addressService;
    private final ReniecService reniecService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<Integer, byte[]> pdfBytesCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, byte[]> ncPdfBytesCache = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${nubefact.api.url:}")
    private String nubefactApiUrl;

    @Value("${nubefact.api.token:}")
    private String nubefactApiToken;

    @Value("${nubefact.nc.serie:FFF1}")
    private String configuredNcSerie = DEFAULT_NC_SERIE;

    @Value("${nubefact.factura.serie:FFF1}")
    private String configuredFacturaSerie = DEFAULT_FACTURA_SERIE;

    public SunatService(JdbcTemplate jdbcTemplate,
                        OrderRepository orderRepository,
                        UsuarioRepository usuarioRepository,
                        AddressService addressService,
                        ReniecService reniecService) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderRepository = orderRepository;
        this.usuarioRepository = usuarioRepository;
        this.addressService = addressService;
        this.reniecService = reniecService;
    }

    /**
     * Emite la Boleta de Venta Electrónica en SUNAT / Nubefact usando exactamente la estructura JSON
     * requerida por Nubefact (operacion: generar_comprobante, tipo_de_comprobante: 2, serie: BBB1),
     * jalando los productos reales del pedido y guardando los enlaces PDF/XML/Hash en la orden.
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public BoletaEmisionDTO emitirBoletaParaPedido(Integer orderId, String requestedDni, String requestedNombre) {
        return emitirComprobanteParaPedido(orderId, null, requestedDni, requestedNombre, null, null);
    }

    /**
     * Emite una Boleta de Venta Electrónica (tipo 2, serie BBB1, DNI 8 dígitos) o una Factura Electrónica
     * (tipo 1, serie FFF1, RUC 11 dígitos) en SUNAT / Nubefact usando la estructura JSON oficial,
     * jalando los productos reales del pedido y guardando enlaces PDF/XML/Hash en la orden y datos en users/caché.
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public BoletaEmisionDTO emitirComprobanteParaPedido(Integer orderId,
                                                        Integer requestedTipoComprobante,
                                                        String requestedDocumento,
                                                        String requestedNombre,
                                                        String requestedDireccion,
                                                        Boolean rucVerified) {
        if (orderId == null) {
            throw new BadRequestException("El ID del pedido es obligatorio.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido #" + orderId + " no encontrado."));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("No se puede emitir un comprobante electr\u00f3nico para un pedido anulado.");
        }

        // Si ya fue emitida previamente, devolver los datos existentes
        if ("EMITIDA".equalsIgnoreCase(order.getBoletaEstado())
                && order.getBoletaSerie() != null
                && order.getBoletaNumero() != null
                && order.getBoletaPdfUrl() != null
                && !order.getBoletaPdfUrl().isBlank()) {
            return buildBoletaResultFromOrder(order);
        }

        Usuario usuario = order.getUserId() != null
                ? usuarioRepository.findById(order.getUserId().longValue()).orElse(null)
                : null;

        int resolvedTipo = resolveTipoComprobante(requestedTipoComprobante, order, requestedDocumento);
        boolean isFactura = (resolvedTipo == TIPO_COMPROBANTE_FACTURA);
        String resolvedSerie = isFactura
                ? ((configuredFacturaSerie != null && !configuredFacturaSerie.isBlank())
                    ? configuredFacturaSerie.trim().toUpperCase(Locale.ROOT)
                    : DEFAULT_FACTURA_SERIE)
                : DEFAULT_SERIE;

        String cleanDoc;
        String cleanNombre;
        String cleanDireccion = firstNonBlank(requestedDireccion, order.getBoletaClienteDireccion());

        if (isFactura) {
            cleanDoc = firstNonBlank(
                    requestedDocumento,
                    order.getBoletaClienteDni(),
                    usuario != null ? usuario.getRuc() : null
            );
            if (cleanDoc != null) {
                cleanDoc = cleanDoc.trim();
            }
            if (cleanDoc == null || !cleanDoc.matches("^(10|15|16|17|20)\\d{9}$")) {
                throw new BadRequestException("Para emitir una Factura Electr\u00f3nica FFF1 se requiere un RUC v\u00e1lido de 11 d\u00edgitos (ej. 10... o 20...).");
            }

            boolean alreadyVerifiedSameRuc = usuario != null
                    && Boolean.TRUE.equals(usuario.getRucVerified())
                    && cleanDoc.equals(usuario.getRuc());
            if (!alreadyVerifiedSameRuc && Boolean.FALSE.equals(rucVerified)) {
                throw new BadRequestException("Debe aceptar que es titular o representante autorizado del RUC y mayor de 18 a\u00f1os para emitir Factura Electr\u00f3nica.");
            }

            cleanNombre = firstNonBlank(
                    requestedNombre,
                    order.getBoletaClienteNombre(),
                    (usuario != null && cleanDoc.equals(usuario.getRuc())) ? usuario.getRucRazonSocial() : null
            );
            RucLookupDTO rucLookup = null;
            if ((cleanNombre == null || cleanNombre.isBlank() || cleanDireccion == null || cleanDireccion.isBlank())
                    && reniecService != null) {
                try {
                    rucLookup = reniecService.consultarRuc(cleanDoc);
                    if (rucLookup != null) {
                        if ((cleanNombre == null || cleanNombre.isBlank())
                                && rucLookup.razonSocial() != null && !rucLookup.razonSocial().isBlank()) {
                            cleanNombre = rucLookup.razonSocial();
                        }
                        if ((cleanDireccion == null || cleanDireccion.isBlank())
                                && rucLookup.direccion() != null && !rucLookup.direccion().isBlank()) {
                            cleanDireccion = rucLookup.direccion();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (cleanDireccion == null || cleanDireccion.isBlank()) {
                if (usuario != null && cleanDoc.equals(usuario.getRuc())
                        && usuario.getRucDireccion() != null && !usuario.getRucDireccion().isBlank()) {
                    cleanDireccion = usuario.getRucDireccion();
                } else {
                    cleanDireccion = resolveOrderShippingAddress(order);
                }
            }
            if (cleanNombre == null || cleanNombre.isBlank()) {
                cleanNombre = "EMPRESA RUC " + cleanDoc;
            }
            cleanNombre = cleanNombre.trim().toUpperCase(Locale.ROOT);
            cleanDireccion = cleanDireccion.trim().toUpperCase(Locale.ROOT);

            if (usuario != null) {
                boolean needsSave = usuario.getRuc() == null
                        || !cleanDoc.equals(usuario.getRuc())
                        || !Boolean.TRUE.equals(usuario.getRucVerified())
                        || (usuario.getRucRazonSocial() == null && cleanNombre != null);
                if (needsSave) {
                    usuario.setRuc(cleanDoc);
                    usuario.setRucRazonSocial(cleanNombre);
                    if (!"LIMA".equalsIgnoreCase(cleanDireccion)) {
                        usuario.setRucDireccion(cleanDireccion);
                    }
                    usuario.setRucVerified(true);
                    usuario.setAgeVerified(true);
                    usuarioRepository.saveAndFlush(usuario);
                }
            }
            if (reniecService != null && rucLookup == null && !cleanNombre.startsWith("EMPRESA RUC ")) {
                reniecService.saveRucToCache(new RucLookupDTO(
                        cleanDoc, cleanNombre, cleanDireccion, "ACTIVO", "HABIDO", "", "", "", "FACTURA_EMISION"
                ));
            }
        } else {
            // Resolver DNI del cliente (pedido -> solicitud -> base de datos de usuario)
            cleanDoc = firstNonBlank(
                    requestedDocumento,
                    order.getBoletaClienteDni(),
                    usuario != null ? usuario.getDni() : null
            );
            if (cleanDoc != null) {
                cleanDoc = cleanDoc.trim();
            }
            if (cleanDoc == null || !cleanDoc.matches("^\\d{8}$")) {
                throw new BadRequestException("Para emitir una Boleta de Venta Electr\u00f3nica se requiere un DNI v\u00e1lido de 8 d\u00edgitos.");
            }

            if (usuario != null && (usuario.getDni() == null || !cleanDoc.equals(usuario.getDni()))) {
                usuario.setDni(cleanDoc);
                usuario.setAgeVerified(true);
                usuarioRepository.saveAndFlush(usuario);
            }

            cleanNombre = firstNonBlank(
                    requestedNombre,
                    order.getBoletaClienteNombre()
            );
            if ((cleanNombre == null || cleanNombre.isBlank()) && reniecService != null) {
                try {
                    DniLookupDTO ren = reniecService.consultarDni(cleanDoc);
                    if (ren != null && ren.nombreCompleto() != null && !ren.nombreCompleto().isBlank()) {
                        cleanNombre = ren.nombreCompleto();
                    }
                } catch (Exception ignored) {
                }
            }
            if ((cleanNombre == null || cleanNombre.isBlank()) && usuario != null && usuario.getName() != null) {
                cleanNombre = usuario.getName();
            }
            if (cleanNombre == null || cleanNombre.isBlank()) {
                cleanNombre = "CLIENTE DNI " + cleanDoc;
            }
            cleanNombre = cleanNombre.trim().toUpperCase(Locale.ROOT);
            if (cleanDireccion == null || cleanDireccion.isBlank()) {
                cleanDireccion = resolveOrderShippingAddress(order);
            } else {
                cleanDireccion = cleanDireccion.trim().toUpperCase(Locale.ROOT);
            }
        }

        // Cargar items reales del pedido desde la base de datos
        List<OrderCompleteDTO.OrderItemCompleteDTO> orderItems = loadOrderItemsForBoleta(orderId);
        if (orderItems.isEmpty()) {
            throw new BadRequestException("El pedido #" + orderId + " no contiene productos para facturar.");
        }

        // Construir líneas del comprobante con IGV (18%) cuadrando exactamente con el total del pedido
        List<Map<String, Object>> nubefactItems = new ArrayList<>();
        BigDecimal targetTotal = order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0
                ? order.getTotal().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal rawLinesSum = BigDecimal.ZERO;
        for (OrderCompleteDTO.OrderItemCompleteDTO item : orderItems) {
            BigDecimal lt = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
            rawLinesSum = rawLinesSum.add(lt);
        }
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            rawLinesSum = rawLinesSum.add(deliveryFee);
        }

        if (targetTotal.compareTo(BigDecimal.ZERO) <= 0) {
            targetTotal = rawLinesSum.setScale(2, RoundingMode.HALF_UP);
        }

        List<RawBoletaLine> rawLines = new ArrayList<>();

        int pzCounter = 1;
        int exCounter = 1;
        for (OrderCompleteDTO.OrderItemCompleteDTO item : orderItems) {
            boolean isExtra = "EXTRA".equalsIgnoreCase(item.getItemType());
            String codigo;
            String descripcion;
            if (isExtra) {
                int codeId = item.getExtraId() != null ? item.getExtraId() : exCounter;
                codigo = String.format("EX%03d", codeId);
                descripcion = (item.getExtraName() != null ? item.getExtraName() : "COMPLEMENTO").toUpperCase(Locale.ROOT);
                exCounter++;
            } else {
                int codeId = item.getPizzaId() != null ? item.getPizzaId() : pzCounter;
                codigo = String.format("PZ%03d", codeId);
                StringBuilder sb = new StringBuilder();
                String pName = item.getPizzaName() != null ? item.getPizzaName().trim() : "PIZZA";
                if (!pName.toUpperCase(Locale.ROOT).startsWith("PIZZA")) {
                    sb.append("PIZZA ");
                }
                sb.append(pName.toUpperCase(Locale.ROOT));
                if (item.getSizeName() != null && !item.getSizeName().isBlank()) {
                    sb.append(" ").append(item.getSizeName().trim().toUpperCase(Locale.ROOT));
                }
                if (item.getExtras() != null && !item.getExtras().isEmpty()) {
                    sb.append(" (+ ").append(String.join(", ", item.getExtras()).toUpperCase(Locale.ROOT)).append(")");
                }
                descripcion = sb.toString().trim();
                pzCounter++;
            }
            int qty = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 1;
            BigDecimal lineTot = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
            if (lineTot.compareTo(BigDecimal.ZERO) > 0) {
                rawLines.add(new RawBoletaLine(UNIDAD_MEDIDA_BIEN, codigo, descripcion, qty, lineTot));
            }
        }

        if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            rawLines.add(new RawBoletaLine(UNIDAD_MEDIDA_SERVICIO, "DLV001", "SERVICIO DE ENTREGA A DOMICILIO (DELIVERY)", 1, deliveryFee));
        }

        BigDecimal accumulatedTotal = BigDecimal.ZERO;
        BigDecimal totalGravada = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;

        for (int i = 0; i < rawLines.size(); i++) {
            RawBoletaLine rl = rawLines.get(i);
            BigDecimal lineFinalTotal;
            if (i == rawLines.size() - 1) {
                lineFinalTotal = targetTotal.subtract(accumulatedTotal).setScale(2, RoundingMode.HALF_UP);
            } else if (rawLinesSum.compareTo(BigDecimal.ZERO) > 0) {
                lineFinalTotal = rl.rawLineTotal()
                        .multiply(targetTotal)
                        .divide(rawLinesSum, 2, RoundingMode.HALF_UP);
            } else {
                lineFinalTotal = rl.rawLineTotal().setScale(2, RoundingMode.HALF_UP);
            }
            if (lineFinalTotal.compareTo(new BigDecimal("0.01")) < 0) {
                lineFinalTotal = new BigDecimal("0.01");
            }
            accumulatedTotal = accumulatedTotal.add(lineFinalTotal);

            int qty = rl.cantidad();
            String descripcion = rl.descripcion();

            BigDecimal unitPriceCheck = lineFinalTotal.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
            if (qty > 1 && unitPriceCheck.multiply(BigDecimal.valueOf(qty)).compareTo(lineFinalTotal) != 0) {
                descripcion = qty + "X " + descripcion;
                qty = 1;
            }

            BigDecimal lineSubtotal = lineFinalTotal.divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP);
            BigDecimal lineIgv = lineFinalTotal.subtract(lineSubtotal).setScale(2, RoundingMode.HALF_UP);
            BigDecimal precioUnitario = lineFinalTotal.divide(BigDecimal.valueOf(qty), 6, RoundingMode.HALF_UP).stripTrailingZeros();
            BigDecimal valorUnitario = lineSubtotal.divide(BigDecimal.valueOf(qty), 6, RoundingMode.HALF_UP).stripTrailingZeros();

            totalGravada = totalGravada.add(lineSubtotal);
            totalIgv = totalIgv.add(lineIgv);

            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("unidad_de_medida", rl.unidadMedida());
            itemMap.put("codigo", rl.codigo());
            itemMap.put("descripcion", descripcion);
            itemMap.put("cantidad", qty);
            itemMap.put("valor_unitario", new BigDecimal(valorUnitario.toPlainString()));
            itemMap.put("precio_unitario", new BigDecimal(precioUnitario.toPlainString()));
            itemMap.put("subtotal", lineSubtotal);
            itemMap.put("tipo_de_igv", 1);
            itemMap.put("igv", lineIgv);
            itemMap.put("total", lineFinalTotal);
            itemMap.put("anticipo_regularizacion", false);

            nubefactItems.add(itemMap);
        }

        BigDecimal finalTotal = totalGravada.add(totalIgv).setScale(2, RoundingMode.HALF_UP);
        String fechaEmision = ZonedDateTime.now(ZoneId.of("America/Lima")).format(NUBEFACT_DATE_FMT);

        int nextNumero = isFactura ? determineNextFacturaNumero(resolvedSerie) : determineNextBoletaNumero();

        order.setWantsBoleta(true);
        order.setBoletaTipoComprobante(resolvedTipo);
        order.setBoletaSerie(resolvedSerie);
        order.setBoletaClienteDni(cleanDoc);
        order.setBoletaClienteNombre(cleanNombre);
        order.setBoletaClienteDireccion(cleanDireccion);
        order.setBoletaTotalGravada(totalGravada);
        order.setBoletaTotalIgv(totalIgv);

        JsonNode nubefactResponse = null;
        int usedNumero = nextNumero;
        String lastError = null;
        String clienteTipoDoc = isFactura ? "6" : "1";

        if (nubefactApiUrl != null && !nubefactApiUrl.isBlank() && nubefactApiToken != null && !nubefactApiToken.isBlank()) {
            for (int attempt = 0; attempt < 20; attempt++) {
                int candidateNumero = nextNumero + attempt;
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("operacion", "generar_comprobante");
                payload.put("tipo_de_comprobante", resolvedTipo);
                payload.put("serie", resolvedSerie);
                payload.put("numero", candidateNumero);
                payload.put("sunat_transaction", 1);
                payload.put("cliente_tipo_de_documento", clienteTipoDoc);
                payload.put("cliente_numero_de_documento", cleanDoc);
                payload.put("cliente_denominacion", cleanNombre);
                payload.put("cliente_direccion", cleanDireccion);
                payload.put("fecha_de_emision", fechaEmision);
                payload.put("moneda", 1);
                payload.put("porcentaje_de_igv", 18);
                payload.put("total_gravada", totalGravada);
                payload.put("total_igv", totalIgv);
                payload.put("total", finalTotal);
                payload.put("enviar_automaticamente_a_la_sunat", false);
                payload.put("enviar_automaticamente_al_cliente", false);
                payload.put("items", nubefactItems);

                try {
                    String jsonBody = objectMapper.writeValueAsString(payload);
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(nubefactApiUrl.trim()))
                            .header("Authorization", nubefactApiToken.trim())
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .timeout(Duration.ofSeconds(15))
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                            .build();

                    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200 || resp.statusCode() == 201) {
                        JsonNode bodyJson = objectMapper.readTree(resp.body());
                        if (!bodyJson.has("errors")) {
                            nubefactResponse = bodyJson;
                            usedNumero = candidateNumero;
                            log.info("{} {}-{} emitida exitosamente en Nubefact para el pedido #{}.",
                                    isFactura ? "Factura Electr\u00f3nica" : "Boleta Electr\u00f3nica",
                                    resolvedSerie, String.format("%06d", usedNumero), orderId);
                            break;
                        } else {
                            lastError = bodyJson.path("errors").asText();
                        }
                    } else {
                        String respText = resp.body() != null ? resp.body() : "";
                        lastError = respText;
                        if (respText.contains("ya existe") || respText.contains("\"codigo\":23")) {
                            continue;
                        }
                        log.warn("Respuesta HTTP {} de Nubefact al emitir {} para pedido #{}: {}",
                                resp.statusCode(), isFactura ? "factura" : "boleta", orderId, respText);
                        break;
                    }
                } catch (Exception ex) {
                    lastError = ex.getMessage();
                    log.error("Error de red al conectar con Nubefact para pedido #{}: {}", orderId, ex.getMessage());
                    break;
                }
            }
        }

        order.setBoletaNumero(usedNumero);
        order.setBoletaEmitidaAt(LocalDateTime.now());

        if (nubefactResponse != null) {
            order.setBoletaPdfUrl(nubefactResponse.path("enlace_del_pdf").asText(null));
            order.setBoletaXmlUrl(nubefactResponse.path("enlace_del_xml").asText(null));
            order.setBoletaCdrUrl(nubefactResponse.path("enlace_del_cdr").asText(null));
            order.setBoletaEnlace(nubefactResponse.path("enlace").asText(null));
            order.setBoletaHash(nubefactResponse.path("codigo_hash").asText(null));
            order.setBoletaQr(nubefactResponse.path("cadena_para_codigo_qr").asText(null));
            order.setBoletaEstado("EMITIDA");
            order.setBoletaError(null);
        } else {
            order.setBoletaPdfUrl("/api/sunat/boleta/order/" + orderId + "/pdf");
            order.setBoletaEstado("EMITIDA");
            order.setBoletaError(lastError != null && lastError.length() > 480 ? lastError.substring(0, 480) : lastError);
        }

        Order saved = orderRepository.saveAndFlush(order);
        pdfBytesCache.remove(orderId);
        return buildBoletaResultFromOrder(saved);
    }

    /**
     * Registra la solicitud de Boleta Electrónica para un pedido con pago contra entrega (Efectivo)
     * SIN enviarla todavía a SUNAT/Nubefact. La emisión real se dispara automáticamente al marcar el pedido como DELIVERED.
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void registrarSolicitudBoletaPendiente(Integer orderId, String requestedDni, String requestedNombre) {
        registrarSolicitudComprobantePendiente(orderId, null, requestedDni, requestedNombre, null, null);
    }

    /**
     * Registra la solicitud de Boleta (BBB1) o Factura (FFF1) para pago contra entrega (Efectivo),
     * validando DNI o RUC (+ condicional rucVerified), guardando los datos en users/caché
     * y difiriendo la emisión ante SUNAT hasta la entrega (DELIVERED).
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void registrarSolicitudComprobantePendiente(Integer orderId,
                                                       Integer requestedTipoComprobante,
                                                       String requestedDocumento,
                                                       String requestedNombre,
                                                       String requestedDireccion,
                                                       Boolean rucVerified) {
        if (orderId == null) {
            throw new BadRequestException("El ID del pedido es obligatorio.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido #" + orderId + " no encontrado."));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("No se puede solicitar comprobante para un pedido anulado.");
        }
        if ("EMITIDA".equalsIgnoreCase(order.getBoletaEstado()) && order.getBoletaNumero() != null) {
            return;
        }

        Usuario usuario = order.getUserId() != null
                ? usuarioRepository.findById(order.getUserId().longValue()).orElse(null)
                : null;

        int resolvedTipo = resolveTipoComprobante(requestedTipoComprobante, order, requestedDocumento);
        boolean isFactura = (resolvedTipo == TIPO_COMPROBANTE_FACTURA);
        String resolvedSerie = isFactura
                ? ((configuredFacturaSerie != null && !configuredFacturaSerie.isBlank())
                    ? configuredFacturaSerie.trim().toUpperCase(Locale.ROOT)
                    : DEFAULT_FACTURA_SERIE)
                : DEFAULT_SERIE;

        String cleanDoc;
        String cleanNombre;
        String cleanDireccion = firstNonBlank(requestedDireccion, order.getBoletaClienteDireccion());

        if (isFactura) {
            cleanDoc = firstNonBlank(
                    requestedDocumento,
                    order.getBoletaClienteDni(),
                    usuario != null ? usuario.getRuc() : null
            );
            if (cleanDoc != null) {
                cleanDoc = cleanDoc.trim();
            }
            if (cleanDoc == null || !cleanDoc.matches("^(10|15|16|17|20)\\d{9}$")) {
                throw new BadRequestException("Para solicitar una Factura Electr\u00f3nica FFF1 se requiere un RUC v\u00e1lido de 11 d\u00edgitos.");
            }
            boolean alreadyVerifiedSameRuc = usuario != null
                    && Boolean.TRUE.equals(usuario.getRucVerified())
                    && cleanDoc.equals(usuario.getRuc());
            if (!alreadyVerifiedSameRuc && Boolean.FALSE.equals(rucVerified)) {
                throw new BadRequestException("Debe aceptar que es titular o representante autorizado del RUC y mayor de 18 a\u00f1os para solicitar Factura Electr\u00f3nica.");
            }

            cleanNombre = firstNonBlank(
                    requestedNombre,
                    order.getBoletaClienteNombre(),
                    (usuario != null && cleanDoc.equals(usuario.getRuc())) ? usuario.getRucRazonSocial() : null
            );
            RucLookupDTO rucLookup = null;
            if ((cleanNombre == null || cleanNombre.isBlank() || cleanDireccion == null || cleanDireccion.isBlank())
                    && reniecService != null) {
                try {
                    rucLookup = reniecService.consultarRuc(cleanDoc);
                    if (rucLookup != null) {
                        if ((cleanNombre == null || cleanNombre.isBlank())
                                && rucLookup.razonSocial() != null && !rucLookup.razonSocial().isBlank()) {
                            cleanNombre = rucLookup.razonSocial();
                        }
                        if ((cleanDireccion == null || cleanDireccion.isBlank())
                                && rucLookup.direccion() != null && !rucLookup.direccion().isBlank()) {
                            cleanDireccion = rucLookup.direccion();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (cleanDireccion == null || cleanDireccion.isBlank()) {
                if (usuario != null && cleanDoc.equals(usuario.getRuc())
                        && usuario.getRucDireccion() != null && !usuario.getRucDireccion().isBlank()) {
                    cleanDireccion = usuario.getRucDireccion();
                } else {
                    cleanDireccion = resolveOrderShippingAddress(order);
                }
            }
            if (cleanNombre == null || cleanNombre.isBlank()) {
                cleanNombre = "EMPRESA RUC " + cleanDoc;
            }
            cleanNombre = cleanNombre.trim().toUpperCase(Locale.ROOT);
            cleanDireccion = cleanDireccion.trim().toUpperCase(Locale.ROOT);

            if (usuario != null) {
                boolean needsSave = usuario.getRuc() == null
                        || !cleanDoc.equals(usuario.getRuc())
                        || !Boolean.TRUE.equals(usuario.getRucVerified());
                if (needsSave) {
                    usuario.setRuc(cleanDoc);
                    usuario.setRucRazonSocial(cleanNombre);
                    if (!"LIMA".equalsIgnoreCase(cleanDireccion)) {
                        usuario.setRucDireccion(cleanDireccion);
                    }
                    usuario.setRucVerified(true);
                    usuario.setAgeVerified(true);
                    usuarioRepository.saveAndFlush(usuario);
                }
            }
            if (reniecService != null && rucLookup == null && !cleanNombre.startsWith("EMPRESA RUC ")) {
                reniecService.saveRucToCache(new RucLookupDTO(
                        cleanDoc, cleanNombre, cleanDireccion, "ACTIVO", "HABIDO", "", "", "", "FACTURA_SOLICITUD"
                ));
            }
        } else {
            cleanDoc = firstNonBlank(
                    requestedDocumento,
                    order.getBoletaClienteDni(),
                    usuario != null ? usuario.getDni() : null
            );
            if (cleanDoc != null) {
                cleanDoc = cleanDoc.trim();
            }
            if (cleanDoc == null || !cleanDoc.matches("^\\d{8}$")) {
                throw new BadRequestException("Para solicitar una Boleta de Venta Electr\u00f3nica se requiere un DNI v\u00e1lido de 8 d\u00edgitos.");
            }
            if (usuario != null && (usuario.getDni() == null || !cleanDoc.equals(usuario.getDni()))) {
                usuario.setDni(cleanDoc);
                usuario.setAgeVerified(true);
                usuarioRepository.saveAndFlush(usuario);
            }
            cleanNombre = firstNonBlank(requestedNombre, order.getBoletaClienteNombre());
            if ((cleanNombre == null || cleanNombre.isBlank()) && reniecService != null) {
                try {
                    DniLookupDTO ren = reniecService.consultarDni(cleanDoc);
                    if (ren != null && ren.nombreCompleto() != null && !ren.nombreCompleto().isBlank()) {
                        cleanNombre = ren.nombreCompleto();
                    }
                } catch (Exception ignored) {
                }
            }
            if ((cleanNombre == null || cleanNombre.isBlank()) && usuario != null && usuario.getName() != null) {
                cleanNombre = usuario.getName();
            }
            if (cleanNombre == null || cleanNombre.isBlank()) {
                cleanNombre = "CLIENTE DNI " + cleanDoc;
            }
            cleanNombre = cleanNombre.trim().toUpperCase(Locale.ROOT);
            if (cleanDireccion == null || cleanDireccion.isBlank()) {
                cleanDireccion = resolveOrderShippingAddress(order);
            } else {
                cleanDireccion = cleanDireccion.trim().toUpperCase(Locale.ROOT);
            }
        }

        order.setWantsBoleta(true);
        order.setBoletaTipoComprobante(resolvedTipo);
        order.setBoletaSerie(resolvedSerie);
        order.setBoletaClienteDni(cleanDoc);
        order.setBoletaClienteNombre(cleanNombre);
        order.setBoletaClienteDireccion(cleanDireccion);
        order.setBoletaEstado("PENDIENTE_ENTREGA");
        order.setBoletaError(null);
        orderRepository.saveAndFlush(order);
        log.info("Solicitud de {} registrada como PENDIENTE_ENTREGA para pedido #{} (pago contra entrega).",
                isFactura ? "Factura Electr\u00f3nica FFF1" : "Boleta Electr\u00f3nica BBB1", orderId);
    }

    private int resolveTipoComprobante(Integer requestedTipoComprobante, Order order, String requestedDocumento) {
        if (requestedTipoComprobante != null) {
            return requestedTipoComprobante == TIPO_COMPROBANTE_FACTURA ? TIPO_COMPROBANTE_FACTURA : TIPO_COMPROBANTE_BOLETA;
        }
        if (order != null && order.getBoletaTipoComprobante() != null) {
            return order.getBoletaTipoComprobante() == TIPO_COMPROBANTE_FACTURA ? TIPO_COMPROBANTE_FACTURA : TIPO_COMPROBANTE_BOLETA;
        }
        if (requestedDocumento != null && requestedDocumento.trim().matches("^(10|15|16|17|20)\\d{9}$")) {
            return TIPO_COMPROBANTE_FACTURA;
        }
        if (order != null && order.getBoletaClienteDni() != null
                && order.getBoletaClienteDni().trim().matches("^(10|15|16|17|20)\\d{9}$")) {
            return TIPO_COMPROBANTE_FACTURA;
        }
        if (order != null && order.getBoletaSerie() != null
                && order.getBoletaSerie().trim().toUpperCase(Locale.ROOT).startsWith("F")) {
            return TIPO_COMPROBANTE_FACTURA;
        }
        return TIPO_COMPROBANTE_BOLETA;
    }

    private String resolveOrderShippingAddress(Order order) {
        String cleanDireccion = "LIMA";
        if (order != null && order.getAddressId() != null) {
            Optional<Address> addrOpt = addressService.obtenerPorId(order.getAddressId());
            if (addrOpt.isPresent()) {
                Address a = addrOpt.get();
                List<String> parts = new ArrayList<>();
                if (a.getLine1() != null && !a.getLine1().isBlank()) parts.add(a.getLine1().trim());
                if (a.getDistrict() != null && !a.getDistrict().isBlank()) parts.add(a.getDistrict().trim());
                if (a.getCity() != null && !a.getCity().isBlank()) parts.add(a.getCity().trim());
                if (!parts.isEmpty()) {
                    cleanDireccion = String.join(", ", parts).toUpperCase(Locale.ROOT);
                }
            }
        }
        return cleanDireccion;
    }

    /**
     * Descarga el PDF oficial de la Boleta de Venta Electrónica desde Nubefact
     * (o genera un PDF SUNAT válido de respaldo si el enlace externo no está disponible).
     */
    public byte[] obtenerPdfBoleta(Integer orderId) {
        if (orderId == null) {
            throw new BadRequestException("ID de pedido inválido.");
        }
        byte[] cached = pdfBytesCache.get(orderId);
        if (cached != null && cached.length > 100) {
            return cached;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido #" + orderId + " no encontrado."));

        if (order.getBoletaNumero() == null
                && (order.getStatus() == Order.OrderStatus.CANCELLED
                    || "ANULADA_SIN_EMISION".equalsIgnoreCase(order.getBoletaEstado()))) {
            throw new BadRequestException("Este pedido fue anulado y no posee una Boleta Electr\u00f3nica emitida.");
        }
        if ("PENDIENTE_ENTREGA".equalsIgnoreCase(order.getBoletaEstado()) && order.getBoletaNumero() == null) {
            throw new BadRequestException("La Boleta Electr\u00f3nica se emitir\u00e1 autom\u00e1ticamente al confirmar la entrega y el pago en efectivo.");
        }

        // Si el pedido aún no tiene boleta emitida, emitirla en el acto
        if (order.getBoletaNumero() == null || order.getBoletaEstado() == null) {
            emitirBoletaParaPedido(orderId, order.getBoletaClienteDni(), order.getBoletaClienteNombre());
            order = orderRepository.findById(orderId).orElseThrow();
        }

        String pdfUrl = order.getBoletaPdfUrl();
        if (pdfUrl != null && (pdfUrl.startsWith("https://www.nubefact.com/") || pdfUrl.startsWith("https://nubefact.com/"))) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(pdfUrl))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200 && resp.body() != null && resp.body().length > 100) {
                    pdfBytesCache.put(orderId, resp.body());
                    return resp.body();
                }
            } catch (Exception ex) {
                log.warn("No se pudo descargar PDF remoto desde Nubefact ({}), generando PDF SUNAT local: {}", pdfUrl, ex.getMessage());
            }
        }

        byte[] fallbackPdf = generateFallbackSunatPdf(order);
        pdfBytesCache.put(orderId, fallbackPdf);
        return fallbackPdf;
    }

    /**
     * Emite una Nota de Crédito Electrónica (serie FFF1 por defecto, tipo_de_comprobante = 3,
     * tipo_de_nota_de_credito = 1 - Anulación de la operación) dirigida a la Boleta de Venta Electrónica
     * previamente emitida para un pedido, anulándola contable y tributariamente ante SUNAT / Nubefact.
     */
    @Transactional
    public NotaCreditoEmisionDTO emitirNotaCreditoParaPedido(Integer orderId, String motivo) {
        if (orderId == null) {
            throw new BadRequestException("El ID del pedido es obligatorio.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido #" + orderId + " no encontrado."));

        if ("EMITIDA".equalsIgnoreCase(order.getNcEstado())
                && order.getNcSerie() != null
                && order.getNcNumero() != null) {
            return buildNotaCreditoResultFromOrder(order);
        }

        if (order.getBoletaNumero() == null || order.getBoletaSerie() == null || order.getBoletaSerie().isBlank()) {
            throw new BadRequestException("El pedido #" + orderId + " no tiene una Boleta Electr\u00f3nica emitida para anular con Nota de Cr\u00e9dito.");
        }

        String boletaSerie = order.getBoletaSerie().trim();
        int boletaNumero = order.getBoletaNumero();
        boolean isFacturaModificada = Integer.valueOf(TIPO_COMPROBANTE_FACTURA).equals(order.getBoletaTipoComprobante())
                || boletaSerie.toUpperCase(Locale.ROOT).startsWith("F");
        String boletaComprobante = boletaSerie + "-" + String.format("%06d", boletaNumero);
        String cleanMotivo = (motivo != null && !motivo.isBlank())
                ? motivo.trim()
                : ("Anulaci\u00f3n de la operaci\u00f3n - Pedido #" + orderId + " ("
                    + (isFacturaModificada ? "Factura " : "Boleta ") + boletaComprobante + ")");
        if (cleanMotivo.length() > 250) {
            cleanMotivo = cleanMotivo.substring(0, 250);
        }

        String cleanDni = order.getBoletaClienteDni() != null && !order.getBoletaClienteDni().isBlank()
                ? order.getBoletaClienteDni().trim()
                : (isFacturaModificada ? "00000000000" : "00000000");
        String cleanNombre = order.getBoletaClienteNombre() != null && !order.getBoletaClienteNombre().isBlank()
                ? order.getBoletaClienteNombre().trim().toUpperCase(Locale.ROOT)
                : ((isFacturaModificada ? "EMPRESA RUC " : "CLIENTE DNI ") + cleanDni);
        String cleanDireccion = order.getBoletaClienteDireccion() != null && !order.getBoletaClienteDireccion().isBlank()
                ? order.getBoletaClienteDireccion().trim().toUpperCase(Locale.ROOT)
                : "LIMA";

        List<OrderCompleteDTO.OrderItemCompleteDTO> orderItems = loadOrderItemsForBoleta(orderId);
        List<Map<String, Object>> nubefactItems = new ArrayList<>();
        BigDecimal targetTotal = order.getTotal() != null && order.getTotal().compareTo(BigDecimal.ZERO) > 0
                ? order.getTotal().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal rawLinesSum = BigDecimal.ZERO;
        for (OrderCompleteDTO.OrderItemCompleteDTO item : orderItems) {
            BigDecimal lt = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
            rawLinesSum = rawLinesSum.add(lt);
        }
        BigDecimal deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
        if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            rawLinesSum = rawLinesSum.add(deliveryFee);
        }
        if (targetTotal.compareTo(BigDecimal.ZERO) <= 0) {
            targetTotal = rawLinesSum.setScale(2, RoundingMode.HALF_UP);
        }

        List<RawBoletaLine> rawLines = new ArrayList<>();
        int pzCounter = 1;
        int exCounter = 1;
        for (OrderCompleteDTO.OrderItemCompleteDTO item : orderItems) {
            boolean isExtra = "EXTRA".equalsIgnoreCase(item.getItemType());
            String codigo;
            String descripcion;
            if (isExtra) {
                int codeId = item.getExtraId() != null ? item.getExtraId() : exCounter;
                codigo = String.format("EX%03d", codeId);
                descripcion = (item.getExtraName() != null ? item.getExtraName() : "COMPLEMENTO").toUpperCase(Locale.ROOT);
                exCounter++;
            } else {
                int codeId = item.getPizzaId() != null ? item.getPizzaId() : pzCounter;
                codigo = String.format("PZ%03d", codeId);
                StringBuilder sb = new StringBuilder();
                String pName = item.getPizzaName() != null ? item.getPizzaName().trim() : "PIZZA";
                if (!pName.toUpperCase(Locale.ROOT).startsWith("PIZZA")) {
                    sb.append("PIZZA ");
                }
                sb.append(pName.toUpperCase(Locale.ROOT));
                if (item.getSizeName() != null && !item.getSizeName().isBlank()) {
                    sb.append(" ").append(item.getSizeName().trim().toUpperCase(Locale.ROOT));
                }
                if (item.getExtras() != null && !item.getExtras().isEmpty()) {
                    sb.append(" (+ ").append(String.join(", ", item.getExtras()).toUpperCase(Locale.ROOT)).append(")");
                }
                descripcion = sb.toString().trim();
                pzCounter++;
            }
            int qty = (item.getQuantity() != null && item.getQuantity() > 0) ? item.getQuantity() : 1;
            BigDecimal lineTot = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
            if (lineTot.compareTo(BigDecimal.ZERO) > 0) {
                rawLines.add(new RawBoletaLine(UNIDAD_MEDIDA_BIEN, codigo, descripcion, qty, lineTot));
            }
        }
        if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            rawLines.add(new RawBoletaLine(UNIDAD_MEDIDA_SERVICIO, "DLV001", "SERVICIO DE ENTREGA A DOMICILIO (DELIVERY)", 1, deliveryFee));
        }
        if (rawLines.isEmpty() && targetTotal.compareTo(BigDecimal.ZERO) > 0) {
            rawLines.add(new RawBoletaLine(UNIDAD_MEDIDA_BIEN, "PED001", "ANULACION PEDIDO #" + orderId, 1, targetTotal));
        }

        BigDecimal accumulatedTotal = BigDecimal.ZERO;
        BigDecimal totalGravada = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;

        for (int i = 0; i < rawLines.size(); i++) {
            RawBoletaLine rl = rawLines.get(i);
            BigDecimal lineFinalTotal;
            if (i == rawLines.size() - 1) {
                lineFinalTotal = targetTotal.subtract(accumulatedTotal).setScale(2, RoundingMode.HALF_UP);
            } else if (rawLinesSum.compareTo(BigDecimal.ZERO) > 0) {
                lineFinalTotal = rl.rawLineTotal().multiply(targetTotal).divide(rawLinesSum, 2, RoundingMode.HALF_UP);
            } else {
                lineFinalTotal = rl.rawLineTotal().setScale(2, RoundingMode.HALF_UP);
            }
            if (lineFinalTotal.compareTo(new BigDecimal("0.01")) < 0) {
                lineFinalTotal = new BigDecimal("0.01");
            }
            accumulatedTotal = accumulatedTotal.add(lineFinalTotal);

            int qty = rl.cantidad();
            String descripcion = rl.descripcion();
            BigDecimal unitPriceCheck = lineFinalTotal.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
            if (qty > 1 && unitPriceCheck.multiply(BigDecimal.valueOf(qty)).compareTo(lineFinalTotal) != 0) {
                descripcion = qty + "X " + descripcion;
                qty = 1;
            }

            BigDecimal lineSubtotal = lineFinalTotal.divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP);
            BigDecimal lineIgv = lineFinalTotal.subtract(lineSubtotal).setScale(2, RoundingMode.HALF_UP);
            BigDecimal precioUnitario = lineFinalTotal.divide(BigDecimal.valueOf(qty), 6, RoundingMode.HALF_UP).stripTrailingZeros();
            BigDecimal valorUnitario = lineSubtotal.divide(BigDecimal.valueOf(qty), 6, RoundingMode.HALF_UP).stripTrailingZeros();

            totalGravada = totalGravada.add(lineSubtotal);
            totalIgv = totalIgv.add(lineIgv);

            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("unidad_de_medida", rl.unidadMedida());
            itemMap.put("codigo", rl.codigo());
            itemMap.put("descripcion", descripcion);
            itemMap.put("cantidad", qty);
            itemMap.put("valor_unitario", new BigDecimal(valorUnitario.toPlainString()));
            itemMap.put("precio_unitario", new BigDecimal(precioUnitario.toPlainString()));
            itemMap.put("subtotal", lineSubtotal);
            itemMap.put("tipo_de_igv", 1);
            itemMap.put("igv", lineIgv);
            itemMap.put("total", lineFinalTotal);
            itemMap.put("anticipo_regularizacion", false);
            nubefactItems.add(itemMap);
        }

        BigDecimal finalTotal = totalGravada.add(totalIgv).setScale(2, RoundingMode.HALF_UP);
        String fechaEmision = ZonedDateTime.now(ZoneId.of("America/Lima")).format(NUBEFACT_DATE_FMT);

        String primarySerie = (configuredNcSerie != null && !configuredNcSerie.isBlank())
                ? configuredNcSerie.trim().toUpperCase(Locale.ROOT)
                : DEFAULT_NC_SERIE;
        List<String> seriesToTry = new ArrayList<>();
        seriesToTry.add(primarySerie);
        if (!primarySerie.equalsIgnoreCase(boletaSerie)) {
            seriesToTry.add(boletaSerie);
        }

        String usedSerie = primarySerie;
        int usedNumero = determineNextNotaCreditoNumero(primarySerie);
        JsonNode nubefactResponse = null;
        String lastError = null;

        if (nubefactApiUrl != null && !nubefactApiUrl.isBlank() && nubefactApiToken != null && !nubefactApiToken.isBlank()) {
            seriesLoop:
            for (String candidateSerie : seriesToTry) {
                int startNum = determineNextNotaCreditoNumero(candidateSerie);
                for (int attempt = 0; attempt < 20; attempt++) {
                    int candidateNumero = startNum + attempt;
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("operacion", "generar_comprobante");
                    payload.put("tipo_de_comprobante", TIPO_COMPROBANTE_NOTA_CREDITO);
                    payload.put("serie", candidateSerie);
                    payload.put("numero", candidateNumero);
                    payload.put("sunat_transaction", 1);
                    payload.put("cliente_tipo_de_documento", isFacturaModificada ? "6" : "1");
                    payload.put("cliente_numero_de_documento", cleanDni);
                    payload.put("cliente_denominacion", cleanNombre);
                    payload.put("cliente_direccion", cleanDireccion);
                    payload.put("fecha_de_emision", fechaEmision);
                    payload.put("moneda", 1);
                    payload.put("porcentaje_de_igv", 18);
                    payload.put("total_gravada", totalGravada);
                    payload.put("total_igv", totalIgv);
                    payload.put("total", finalTotal);
                    payload.put("observaciones", cleanMotivo);
                    payload.put("documento_que_se_modifica_tipo", isFacturaModificada ? TIPO_COMPROBANTE_FACTURA : TIPO_COMPROBANTE_BOLETA);
                    payload.put("documento_que_se_modifica_serie", boletaSerie);
                    payload.put("documento_que_se_modifica_numero", boletaNumero);
                    payload.put("tipo_de_nota_de_credito", TIPO_NC_ANULACION_OPERACION);
                    payload.put("enviar_automaticamente_a_la_sunat", false);
                    payload.put("enviar_automaticamente_al_cliente", false);
                    payload.put("items", nubefactItems);

                    try {
                        String jsonBody = objectMapper.writeValueAsString(payload);
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(nubefactApiUrl.trim()))
                                .header("Authorization", nubefactApiToken.trim())
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .timeout(Duration.ofSeconds(15))
                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                                .build();

                        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                        if (resp.statusCode() == 200 || resp.statusCode() == 201) {
                            JsonNode bodyJson = objectMapper.readTree(resp.body());
                            if (!bodyJson.has("errors")) {
                                nubefactResponse = bodyJson;
                                usedSerie = candidateSerie;
                                usedNumero = candidateNumero;
                                log.info("Nota de Cr\u00e9dito Electr\u00f3nica {}-{} emitida en Nubefact anulando Boleta {} (Pedido #{}).",
                                        usedSerie, String.format("%06d", usedNumero), boletaComprobante, orderId);
                                break seriesLoop;
                            } else {
                                lastError = bodyJson.path("errors").asText();
                            }
                        } else {
                            String respText = resp.body() != null ? resp.body() : "";
                            lastError = respText;
                            if (respText.contains("ya existe") || respText.contains("\"codigo\":23")) {
                                continue;
                            }
                            String low = respText.toLowerCase(Locale.ROOT);
                            if (low.contains("serie") || low.contains("empezar con b") || low.contains("iniciar con b")) {
                                log.warn("Nubefact rechaz\u00f3 serie {} para Nota de Cr\u00e9dito sobre Boleta {}: {}. Reintentando serie alternativa.",
                                        candidateSerie, boletaComprobante, respText);
                                break;
                            }
                            log.warn("Respuesta HTTP {} de Nubefact al emitir Nota de Cr\u00e9dito para pedido #{}: {}", resp.statusCode(), orderId, respText);
                            break seriesLoop;
                        }
                    } catch (Exception ex) {
                        lastError = ex.getMessage();
                        log.error("Error de red al emitir Nota de Cr\u00e9dito en Nubefact para pedido #{}: {}", orderId, ex.getMessage());
                        break seriesLoop;
                    }
                }
            }
        }

        order.setNcSerie(usedSerie);
        order.setNcNumero(usedNumero);
        order.setNcTipoNota(TIPO_NC_ANULACION_OPERACION);
        order.setNcMotivo(cleanMotivo);
        order.setNcEmitidaAt(LocalDateTime.now());
        order.setBoletaEstado("ANULADA_NC");

        if (nubefactResponse != null) {
            order.setNcPdfUrl(nubefactResponse.path("enlace_del_pdf").asText(null));
            order.setNcXmlUrl(nubefactResponse.path("enlace_del_xml").asText(null));
            order.setNcCdrUrl(nubefactResponse.path("enlace_del_cdr").asText(null));
            order.setNcEnlace(nubefactResponse.path("enlace").asText(null));
            order.setNcHash(nubefactResponse.path("codigo_hash").asText(null));
            order.setNcQr(nubefactResponse.path("cadena_para_codigo_qr").asText(null));
            order.setNcEstado("EMITIDA");
            order.setNcError(null);
        } else {
            order.setNcPdfUrl("/api/sunat/nota-credito/order/" + orderId + "/pdf");
            order.setNcEstado("EMITIDA");
            order.setNcError(lastError != null && lastError.length() > 480 ? lastError.substring(0, 480) : lastError);
        }

        Order saved = orderRepository.saveAndFlush(order);
        ncPdfBytesCache.remove(orderId);
        return buildNotaCreditoResultFromOrder(saved);
    }

    public byte[] obtenerPdfNotaCredito(Integer orderId) {
        if (orderId == null) {
            throw new BadRequestException("ID de pedido inv\u00e1lido.");
        }
        byte[] cached = ncPdfBytesCache.get(orderId);
        if (cached != null && cached.length > 100) {
            return cached;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido #" + orderId + " no encontrado."));

        if (order.getNcNumero() == null || order.getNcSerie() == null) {
            throw new BadRequestException("El pedido #" + orderId + " no tiene una Nota de Cr\u00e9dito Electr\u00f3nica emitida.");
        }

        String pdfUrl = order.getNcPdfUrl();
        if (pdfUrl != null && (pdfUrl.startsWith("https://www.nubefact.com/") || pdfUrl.startsWith("https://nubefact.com/"))) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(pdfUrl))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200 && resp.body() != null && resp.body().length > 100) {
                    ncPdfBytesCache.put(orderId, resp.body());
                    return resp.body();
                }
            } catch (Exception ex) {
                log.warn("No se pudo descargar PDF remoto de Nota de Cr\u00e9dito desde Nubefact ({}), generando PDF local: {}", pdfUrl, ex.getMessage());
            }
        }

        byte[] fallbackPdf = generateFallbackNotaCreditoPdf(order);
        ncPdfBytesCache.put(orderId, fallbackPdf);
        return fallbackPdf;
    }

    private int determineNextNotaCreditoNumero(String serie) {
        try {
            Integer maxDb = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(MAX(nc_numero), 0) FROM dbo.orders WHERE nc_serie = ?",
                    Integer.class,
                    serie
            );
            return (maxDb != null ? maxDb : 0) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private NotaCreditoEmisionDTO buildNotaCreditoResultFromOrder(Order order) {
        String serie = order.getNcSerie() != null ? order.getNcSerie() : DEFAULT_NC_SERIE;
        int numero = order.getNcNumero() != null ? order.getNcNumero() : 1;
        String comprobante = serie + "-" + String.format("%06d", numero);
        String boletaMod = (order.getBoletaSerie() != null && order.getBoletaNumero() != null)
                ? order.getBoletaSerie() + "-" + String.format("%06d", order.getBoletaNumero())
                : null;
        return new NotaCreditoEmisionDTO(
                order.getId(),
                serie,
                numero,
                comprobante,
                boletaMod,
                order.getNcTipoNota() != null ? order.getNcTipoNota() : TIPO_NC_ANULACION_OPERACION,
                order.getNcMotivo(),
                order.getBoletaClienteDni(),
                order.getBoletaClienteNombre(),
                order.getBoletaClienteDireccion(),
                order.getBoletaTotalGravada(),
                order.getBoletaTotalIgv(),
                order.getTotal(),
                order.getNcPdfUrl(),
                order.getNcXmlUrl(),
                order.getNcCdrUrl(),
                order.getNcEnlace(),
                order.getNcHash(),
                order.getNcQr(),
                order.getNcEstado(),
                order.getNcEmitidaAt()
        );
    }

    private byte[] generateFallbackNotaCreditoPdf(Order order) {
        String serie = order.getNcSerie() != null ? order.getNcSerie() : DEFAULT_NC_SERIE;
        int num = order.getNcNumero() != null ? order.getNcNumero() : 1;
        String comprobante = serie + "-" + String.format("%06d", num);
        String boletaRef = (order.getBoletaSerie() != null && order.getBoletaNumero() != null)
                ? order.getBoletaSerie() + "-" + String.format("%06d", order.getBoletaNumero())
                : "BBB1-000001";
        boolean isFacturaMod = Integer.valueOf(TIPO_COMPROBANTE_FACTURA).equals(order.getBoletaTipoComprobante())
                || (order.getBoletaSerie() != null && order.getBoletaSerie().toUpperCase(Locale.ROOT).startsWith("F"));
        String docModLabel = isFacturaMod ? "FACTURA ELECTRONICA " : "BOLETA DE VENTA ELECTRONICA ";
        String dni = order.getBoletaClienteDni() != null ? order.getBoletaClienteDni() : "--------";
        String cliente = sanitizePdfText(order.getBoletaClienteNombre() != null ? order.getBoletaClienteNombre() : "CLIENTE");
        String dir = sanitizePdfText(order.getBoletaClienteDireccion() != null ? order.getBoletaClienteDireccion() : "LIMA");
        String motivo = sanitizePdfText(order.getNcMotivo() != null ? order.getNcMotivo() : "ANULACION DE LA OPERACION");
        if (motivo.length() > 75) motivo = motivo.substring(0, 75);
        String fecha = (order.getNcEmitidaAt() != null ? order.getNcEmitidaAt() : LocalDateTime.now())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        BigDecimal gravada = order.getBoletaTotalGravada() != null ? order.getBoletaTotalGravada()
                : (order.getTotal() != null ? order.getTotal().divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        BigDecimal igv = order.getBoletaTotalIgv() != null ? order.getBoletaTotalIgv()
                : (order.getTotal() != null ? order.getTotal().subtract(gravada) : BigDecimal.ZERO);
        BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;

        List<OrderCompleteDTO.OrderItemCompleteDTO> items = loadOrderItemsForBoleta(order.getId());

        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 16 Tf\n50 780 Td\n(ECO PIZZA PERU - ECOPIZZA S.A.C.) Tj\n");
        stream.append("/F1 10 Tf\n0 -16 Td\n(RUC: 20538225763  |  AV. JAVIER PRADO ESTE 4200 - LIMA) Tj\n");
        stream.append("/F1 13 Tf\n0 -24 Td\n(NOTA DE CREDITO ELECTRONICA: ").append(comprobante).append(") Tj\n");
        stream.append("/F1 10 Tf\n0 -18 Td\n(DOC. QUE MODIFICA: ").append(docModLabel).append(boletaRef).append(") Tj\n");
        stream.append("0 -15 Td\n(TIPO NOTA: 01 - ANULACION DE LA OPERACION) Tj\n");
        stream.append("0 -15 Td\n(MOTIVO: ").append(motivo).append(") Tj\n");
        stream.append("0 -15 Td\n(Pedido #: ").append(order.getId()).append("   |   Fecha de Emision: ").append(fecha).append(") Tj\n");
        stream.append("0 -15 Td\n(").append(isFacturaMod ? "RUC Cliente: " : "DNI Cliente: ").append(dni)
                .append(isFacturaMod ? "   |   Razon Social: " : "   |   Denominacion: ").append(cliente).append(") Tj\n");
        stream.append("0 -15 Td\n(Direccion: ").append(dir).append(") Tj\n");
        stream.append("0 -18 Td\n(--------------------------------------------------------------------------------) Tj\n");
        stream.append("0 -15 Td\n(CANT.   DESCRIPCION                                              TOTAL (S/)) Tj\n");
        stream.append("0 -12 Td\n(--------------------------------------------------------------------------------) Tj\n");

        for (OrderCompleteDTO.OrderItemCompleteDTO item : items) {
            String name = "EXTRA".equalsIgnoreCase(item.getItemType())
                    ? (item.getExtraName() != null ? item.getExtraName() : "EXTRA")
                    : (item.getPizzaName() != null ? item.getPizzaName() : "PIZZA")
                      + (item.getSizeName() != null ? " " + item.getSizeName() : "");
            name = sanitizePdfText(name);
            if (name.length() > 45) name = name.substring(0, 45);
            String line = String.format(Locale.US, "%-7s %-52s S/ %8.2f",
                    item.getQuantity() + " " + UNIDAD_MEDIDA_BIEN, name, item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO);
            stream.append("0 -15 Td\n(").append(line).append(") Tj\n");
        }
        if (order.getDeliveryFee() != null && order.getDeliveryFee().compareTo(BigDecimal.ZERO) > 0) {
            String line = String.format(Locale.US, "%-7s %-52s S/ %8.2f",
                    "1 " + UNIDAD_MEDIDA_SERVICIO, "SERVICIO DE ENTREGA A DOMICILIO (DELIVERY)", order.getDeliveryFee());
            stream.append("0 -15 Td\n(").append(line).append(") Tj\n");
        }
        stream.append("0 -16 Td\n(--------------------------------------------------------------------------------) Tj\n");
        stream.append(String.format(Locale.US, "0 -16 Td\n(OP. GRAVADA: S/ %.2f   |   I.G.V. (18%%): S/ %.2f   |   TOTAL ANULADO: S/ %.2f) Tj\n",
                gravada, igv, total));
        stream.append("0 -24 Td\n(Representacion impresa de la Nota de Credito Electronica - SUNAT / NubeFacT) Tj\nET\n");

        byte[] contentBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj1 = out.size();
            out.write("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj2 = out.size();
            out.write("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj3 = out.size();
            out.write("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj4 = out.size();
            out.write("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Courier >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj5 = out.size();
            out.write(("5 0 obj << /Length " + contentBytes.length + " >> stream\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(contentBytes);
            out.write("\nendstream endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int xrefPos = out.size();
            String xref = String.format(Locale.US,
                    "xref\n0 6\n0000000000 65535 f \n%010d 00000 n \n%010d 00000 n \n%010d 00000 n \n%010d 00000 n \n%010d 00000 n \ntrailer << /Size 6 /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF",
                    obj1, obj2, obj3, obj4, obj5, xrefPos);
            out.write(xref.getBytes(StandardCharsets.ISO_8859_1));
        } catch (Exception ignored) {
        }
        return out.toByteArray();
    }

    private int determineNextBoletaNumero() {
        try {
            Integer maxDb = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(MAX(boleta_numero), 2) FROM dbo.orders WHERE boleta_serie = 'BBB1'",
                    Integer.class
            );
            return (maxDb != null ? Math.max(maxDb, 2) : 2) + 1;
        } catch (Exception e) {
            return 3;
        }
    }

    private int determineNextFacturaNumero(String serie) {
        try {
            Integer maxDb = jdbcTemplate.queryForObject(
                    "SELECT ISNULL(MAX(boleta_numero), 0) FROM dbo.orders WHERE boleta_serie = ? AND boleta_tipo_comprobante = 1",
                    Integer.class,
                    serie
            );
            return (maxDb != null ? maxDb : 0) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private List<OrderCompleteDTO.OrderItemCompleteDTO> loadOrderItemsForBoleta(Integer orderId) {
        String itemSql = """
            SELECT oi.id, oi.pizzaId, oi.extraId, oi.item_type,
                   p.name AS pizza_name, e.name AS extra_name,
                   oi.sizeId, s.name AS size_name,
                   oi.quantity, oi.unitPrice, oi.lineTotal
            FROM order_items oi
            LEFT JOIN pizzas p ON oi.pizzaId = p.id
            LEFT JOIN extras e ON oi.extraId = e.id
            LEFT JOIN sizes s ON oi.sizeId = s.id
            WHERE oi.orderId = ?
            ORDER BY oi.id
            """;

        List<Integer> pizzaItemIds = new ArrayList<>();
        List<OrderCompleteDTO.OrderItemCompleteDTO> items = jdbcTemplate.query(itemSql, (rs, rowNum) -> {
            OrderCompleteDTO.OrderItemCompleteDTO item = new OrderCompleteDTO.OrderItemCompleteDTO();
            item.setId(rs.getInt("id"));
            String itemType = rs.getString("item_type");
            item.setItemType(itemType != null ? itemType : "PIZZA");
            if ("EXTRA".equalsIgnoreCase(itemType)) {
                item.setExtraId(rs.getObject("extraId") != null ? rs.getInt("extraId") : null);
                item.setExtraName(rs.getString("extra_name"));
                item.setPizzaName(rs.getString("extra_name"));
            } else {
                item.setPizzaId(rs.getObject("pizzaId") != null ? rs.getInt("pizzaId") : null);
                item.setPizzaName(rs.getString("pizza_name"));
                item.setSizeId(rs.getObject("sizeId") != null ? rs.getInt("sizeId") : null);
                item.setSizeName(rs.getString("size_name"));
                pizzaItemIds.add(item.getId());
            }
            item.setQuantity(rs.getInt("quantity"));
            item.setUnitPrice(rs.getBigDecimal("unitPrice"));
            item.setLineTotal(rs.getBigDecimal("lineTotal"));
            return item;
        }, orderId);

        if (!pizzaItemIds.isEmpty()) {
            String placeholders = pizzaItemIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String extrasSql = "SELECT orderItemId, ingredientName FROM order_item_extras WHERE orderItemId IN (" + placeholders + ") ORDER BY id";
            Map<Integer, List<String>> extrasMap = new HashMap<>();
            jdbcTemplate.query(extrasSql, rs -> {
                extrasMap.computeIfAbsent(rs.getInt("orderItemId"), k -> new ArrayList<>())
                        .add(rs.getString("ingredientName"));
            }, pizzaItemIds.toArray());

            for (OrderCompleteDTO.OrderItemCompleteDTO item : items) {
                if (!"EXTRA".equalsIgnoreCase(item.getItemType())) {
                    item.setExtras(extrasMap.getOrDefault(item.getId(), List.of()));
                }
            }
        }
        return items;
    }

    private BoletaEmisionDTO buildBoletaResultFromOrder(Order order) {
        String serie = order.getBoletaSerie() != null ? order.getBoletaSerie() : DEFAULT_SERIE;
        int numero = order.getBoletaNumero() != null ? order.getBoletaNumero() : 1;
        String comprobante = serie + "-" + String.format("%06d", numero);
        return new BoletaEmisionDTO(
                order.getId(),
                serie,
                numero,
                comprobante,
                order.getBoletaClienteDni(),
                order.getBoletaClienteNombre(),
                order.getBoletaClienteDireccion(),
                order.getBoletaTotalGravada(),
                order.getBoletaTotalIgv(),
                order.getTotal(),
                order.getBoletaPdfUrl(),
                order.getBoletaXmlUrl(),
                order.getBoletaCdrUrl(),
                order.getBoletaEnlace(),
                order.getBoletaHash(),
                order.getBoletaQr(),
                order.getBoletaEstado(),
                order.getBoletaEmitidaAt()
        );
    }

    /**
     * Genera un archivo PDF estándar (compatible con cualquier visor PDF) con el formato de
     * Boleta de Venta Electrónica SUNAT como respaldo si Nubefact estuviera fuera de línea.
     */
    private byte[] generateFallbackSunatPdf(Order order) {
        boolean isFactura = Integer.valueOf(TIPO_COMPROBANTE_FACTURA).equals(order.getBoletaTipoComprobante())
                || (order.getBoletaSerie() != null && order.getBoletaSerie().toUpperCase(Locale.ROOT).startsWith("F"));
        String serie = order.getBoletaSerie() != null
                ? order.getBoletaSerie()
                : (isFactura ? DEFAULT_FACTURA_SERIE : DEFAULT_SERIE);
        int num = order.getBoletaNumero() != null ? order.getBoletaNumero() : 1;
        String comprobante = serie + "-" + String.format("%06d", num);
        String dni = order.getBoletaClienteDni() != null ? order.getBoletaClienteDni() : "--------";
        String cliente = sanitizePdfText(order.getBoletaClienteNombre() != null ? order.getBoletaClienteNombre() : "CLIENTE");
        String dir = sanitizePdfText(order.getBoletaClienteDireccion() != null ? order.getBoletaClienteDireccion() : "LIMA");
        String fecha = (order.getBoletaEmitidaAt() != null ? order.getBoletaEmitidaAt() : LocalDateTime.now())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        BigDecimal gravada = order.getBoletaTotalGravada() != null ? order.getBoletaTotalGravada()
                : (order.getTotal() != null ? order.getTotal().divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        BigDecimal igv = order.getBoletaTotalIgv() != null ? order.getBoletaTotalIgv()
                : (order.getTotal() != null ? order.getTotal().subtract(gravada) : BigDecimal.ZERO);
        BigDecimal total = order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO;

        List<OrderCompleteDTO.OrderItemCompleteDTO> items = loadOrderItemsForBoleta(order.getId());

        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 16 Tf\n50 780 Td\n(ECO PIZZA PERU - ECOPIZZA S.A.C.) Tj\n");
        stream.append("/F1 10 Tf\n0 -16 Td\n(RUC: 20538225763  |  AV. JAVIER PRADO ESTE 4200 - LIMA) Tj\n");
        stream.append("/F1 13 Tf\n0 -24 Td\n(")
                .append(isFactura ? "FACTURA ELECTRONICA: " : "BOLETA DE VENTA ELECTRONICA: ")
                .append(comprobante).append(") Tj\n");
        stream.append("/F1 10 Tf\n0 -20 Td\n(Pedido #: ").append(order.getId()).append("   |   Fecha de Emision: ").append(fecha).append(") Tj\n");
        stream.append("0 -15 Td\n(").append(isFactura ? "RUC Cliente: " : "DNI Cliente: ").append(dni).append(") Tj\n");
        stream.append("0 -15 Td\n(").append(isFactura ? "Razon Social: " : "Denominacion: ").append(cliente).append(") Tj\n");
        stream.append("0 -15 Td\n(").append(isFactura ? "Direccion Fiscal: " : "Direccion: ").append(dir).append(") Tj\n");
        stream.append("0 -20 Td\n(--------------------------------------------------------------------------------) Tj\n");
        stream.append("0 -15 Td\n(CANT.   DESCRIPCION                                              TOTAL (S/)) Tj\n");
        stream.append("0 -12 Td\n(--------------------------------------------------------------------------------) Tj\n");

        for (OrderCompleteDTO.OrderItemCompleteDTO item : items) {
            String name = "EXTRA".equalsIgnoreCase(item.getItemType())
                    ? (item.getExtraName() != null ? item.getExtraName() : "EXTRA")
                    : (item.getPizzaName() != null ? item.getPizzaName() : "PIZZA")
                      + (item.getSizeName() != null ? " " + item.getSizeName() : "");
            name = sanitizePdfText(name);
            if (name.length() > 45) name = name.substring(0, 45);
            String line = String.format(Locale.US, "%-7s %-52s S/ %8.2f",
                    item.getQuantity() + " " + UNIDAD_MEDIDA_BIEN, name, item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO);
            stream.append("0 -15 Td\n(").append(line).append(") Tj\n");
        }
        if (order.getDeliveryFee() != null && order.getDeliveryFee().compareTo(BigDecimal.ZERO) > 0) {
            String line = String.format(Locale.US, "%-7s %-52s S/ %8.2f",
                    "1 " + UNIDAD_MEDIDA_SERVICIO, "SERVICIO DE ENTREGA A DOMICILIO (DELIVERY)", order.getDeliveryFee());
            stream.append("0 -15 Td\n(").append(line).append(") Tj\n");
        }
        stream.append("0 -16 Td\n(--------------------------------------------------------------------------------) Tj\n");
        stream.append(String.format(Locale.US, "0 -16 Td\n(OP. GRAVADA: S/ %.2f   |   I.G.V. (18%%): S/ %.2f   |   TOTAL: S/ %.2f) Tj\n",
                gravada, igv, total));
        stream.append("0 -24 Td\n(Representacion impresa de la ")
                .append(isFactura ? "Factura Electronica" : "Boleta de Venta Electronica")
                .append(" - SUNAT / NubeFacT) Tj\nET\n");

        byte[] contentBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj1 = out.size();
            out.write("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj2 = out.size();
            out.write("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj3 = out.size();
            out.write("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj4 = out.size();
            out.write("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Courier >> endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int obj5 = out.size();
            out.write(("5 0 obj << /Length " + contentBytes.length + " >> stream\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write(contentBytes);
            out.write("\nendstream endobj\n".getBytes(StandardCharsets.ISO_8859_1));
            int xrefPos = out.size();
            String xref = String.format(Locale.US,
                    "xref\n0 6\n0000000000 65535 f \n%010d 00000 n \n%010d 00000 n \n%010d 00000 n \n%010d 00000 n \n%010d 00000 n \ntrailer << /Size 6 /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF",
                    obj1, obj2, obj3, obj4, obj5, xrefPos);
            out.write(xref.getBytes(StandardCharsets.ISO_8859_1));
        } catch (Exception ignored) {
        }
        return out.toByteArray();
    }

    private String sanitizePdfText(String text) {
        if (text == null) return "";
        return text.replace("(", "[").replace(")", "]")
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace("Á", "A").replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
                .replace("ñ", "n").replace("Ñ", "N");
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
