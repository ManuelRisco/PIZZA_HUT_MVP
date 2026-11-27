package com.ecopizza.controllers;

import com.ecopizza.dtos.ApiResponse;
import com.ecopizza.dtos.BoletaEmisionDTO;
import com.ecopizza.dtos.ConfigurarDniRequestDTO;
import com.ecopizza.dtos.ConfigurarRucRequestDTO;
import com.ecopizza.dtos.DniLookupDTO;
import com.ecopizza.dtos.EmitirBoletaRequestDTO;
import com.ecopizza.dtos.EmitirNotaCreditoRequestDTO;
import com.ecopizza.dtos.NotaCreditoEmisionDTO;
import com.ecopizza.dtos.RucLookupDTO;
import com.ecopizza.dtos.UsuarioDTO;
import com.ecopizza.exceptions.BadRequestException;
import com.ecopizza.mappers.UsuarioMapper;
import com.ecopizza.models.Order;
import com.ecopizza.models.Payment;
import com.ecopizza.models.Usuario;
import com.ecopizza.security.SecurityUtils;
import com.ecopizza.services.EmailService;
import com.ecopizza.services.OrderService;
import com.ecopizza.services.PaymentService;
import com.ecopizza.services.ReniecService;
import com.ecopizza.services.SunatService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RestController
@RequestMapping("/api/sunat")
public class SunatReniecController {

    private final SunatService sunatService;
    private final ReniecService reniecService;
    private final OrderService orderService;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;
    private final UsuarioMapper usuarioMapper;
    private PaymentService paymentService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public SunatReniecController(SunatService sunatService,
            ReniecService reniecService,
            OrderService orderService,
            EmailService emailService,
            SecurityUtils securityUtils,
            UsuarioMapper usuarioMapper) {
        this.sunatService = sunatService;
        this.reniecService = reniecService;
        this.orderService = orderService;
        this.emailService = emailService;
        this.securityUtils = securityUtils;
        this.usuarioMapper = usuarioMapper;
    }

    /**
     * Endpoint público para consultar datos de un DNI de 8 dígitos en RENIEC
     * (accesible desde Registro público, Login, Carrito y Panel de Administración).
     */
    @GetMapping("/dni/{dni}")
    public ResponseEntity<ApiResponse<DniLookupDTO>> consultarDni(@PathVariable("dni") String dni) {
        DniLookupDTO resultado = reniecService.consultarDni(dni);
        return ResponseEntity.ok(ApiResponse.success(resultado, "Consulta RENIEC exitosa"));
    }

    /**
     * Endpoint público para consultar datos de un RUC peruano de 11 dígitos en SUNAT
     * con caché en memoria y SQL Server (dbo.sunat_ruc_cache).
     */
    @GetMapping("/ruc/{ruc}")
    public ResponseEntity<ApiResponse<RucLookupDTO>> consultarRuc(@PathVariable("ruc") String ruc) {
        RucLookupDTO resultado = reniecService.consultarRuc(ruc);
        return ResponseEntity.ok(ApiResponse.success(resultado, "Consulta SUNAT RUC exitosa"));
    }

    /**
     * Guarda el RUC, razón social, dirección fiscal y la declaración de titularidad y mayoría de edad (+18)
     * en la tabla users para emitir Facturas Electrónicas FFF1.
     */
    @PostMapping("/configurar-ruc")
    public ResponseEntity<ApiResponse<UsuarioDTO>> configurarRuc(@RequestBody ConfigurarRucRequestDTO body) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Debe iniciar sesi\u00f3n para vincular su RUC."));
        }
        String ruc = body != null && body.ruc() != null ? body.ruc().trim() : null;
        Boolean rucVerified = body != null && Boolean.TRUE.equals(body.rucVerified());
        String razonSocial = body != null && body.razonSocial() != null ? body.razonSocial().trim() : null;
        String direccionFiscal = body != null && body.direccionFiscal() != null ? body.direccionFiscal().trim() : null;

        Usuario actualizado = reniecService.configurarRucUsuario(
                currentUserId.longValue(), ruc, rucVerified, razonSocial, direccionFiscal);
        return ResponseEntity.ok(ApiResponse.success(
                usuarioMapper.toDto(actualizado),
                "RUC verificado y vinculado a tu cuenta para futuras Facturas Electr\u00f3nicas FFF1."));
    }

    /**
     * Guarda el DNI y la declaración de mayoría de edad (+18) y titularidad en el
     * usuario autenticado.
     */
    @PostMapping("/configurar-dni")
    public ResponseEntity<ApiResponse<UsuarioDTO>> configurarDni(@RequestBody ConfigurarDniRequestDTO body) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Debe iniciar sesión para vincular su DNI."));
        }
        String dni = body != null && body.dni() != null ? body.dni().trim() : null;
        Boolean ageVerified = body != null && Boolean.TRUE.equals(body.ageVerified());
        String name = body != null && body.name() != null ? body.name().trim() : null;

        Usuario actualizado = reniecService.configurarDniUsuario(currentUserId.longValue(), dni, ageVerified, name);
        return ResponseEntity.ok(ApiResponse.success(
                usuarioMapper.toDto(actualizado),
                "DNI verificado y vinculado a tu cuenta para futuras Boletas Electrónicas."));
    }

    /**
     * Emite (o consulta si ya existe) la Boleta de Venta Electrónica de un pedido y
     * opcionalmente
     * la envía adjunta en PDF por correo electrónico al cliente.
     */
    @PostMapping("/boleta/order/{orderId}/emitir")
    public ResponseEntity<ApiResponse<BoletaEmisionDTO>> emitirBoletaPedido(
            @PathVariable("orderId") Integer orderId,
            @RequestBody(required = false) EmitirBoletaRequestDTO body) {

        Optional<Order> orderOpt = orderService.obtenerPorId(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Pedido no encontrado"));
        }
        Order order = orderOpt.get();
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !order.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("No tienes permiso para emitir la boleta de este pedido"));
            }
        }

        String ruc = body != null && body.ruc() != null && !body.ruc().isBlank() ? body.ruc().trim() : null;
        String dni = body != null && body.dni() != null && !body.dni().isBlank() ? body.dni().trim() : null;
        String documento = ruc != null ? ruc : dni;
        Integer tipoComprobante = body != null ? body.tipoComprobante() : null;
        if (tipoComprobante == null && ruc != null) {
            tipoComprobante = 1;
        }
        String nombre = body != null && body.nombre() != null ? body.nombre().trim() : null;
        String direccion = body != null && body.direccion() != null ? body.direccion().trim() : null;
        Boolean rucVerified = securityUtils.isAdmin() ? Boolean.TRUE : (body != null ? body.rucVerified() : null);
        boolean enviarCorreo = body == null || !Boolean.FALSE.equals(body.enviarCorreo());

        BoletaEmisionDTO result = sunatService.emitirComprobanteParaPedido(
                orderId, tipoComprobante, documento, nombre, direccion, rucVerified);

        Order refreshed = orderService.obtenerPorId(orderId).orElse(order);
        if (refreshed.getStatus() != Order.OrderStatus.DELIVERED
                && refreshed.getStatus() != Order.OrderStatus.CANCELLED) {
            refreshed.setStatus(Order.OrderStatus.DELIVERED);
            orderService.actualizarOrder(orderId, refreshed);
        }
        if (paymentService != null) {
            paymentService.obtenerPorOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() != Payment.PaymentStatus.PAID) {
                    payment.setStatus(Payment.PaymentStatus.PAID);
                    paymentService.actualizarPayment(payment.getId(), payment);
                }
            });
        }

        if (enviarCorreo && emailService != null) {
            try {
                emailService.sendBoletaEmailForOrder(orderId);
            } catch (Exception ignored) {
            }
        }

        boolean esFactura = Integer.valueOf(1).equals(refreshed.getBoletaTipoComprobante())
                || (result.serie() != null && result.serie().startsWith("F"));
        String tipoLabel = esFactura ? "Factura Electr\u00f3nica " : "Boleta de Venta Electr\u00f3nica ";

        return ResponseEntity.ok(ApiResponse.success(
                result,
                tipoLabel + result.comprobante() + " emitida y pedido marcado como Entregado."));
    }

    /**
     * Descarga o visualiza en el navegador el archivo PDF de la Boleta de Venta
     * Electrónica de un pedido.
     */
    @GetMapping("/boleta/order/{orderId}/pdf")
    public ResponseEntity<byte[]> descargarPdfBoleta(
            @PathVariable("orderId") Integer orderId,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {

        Optional<Order> orderOpt = orderService.obtenerPorId(orderId);
        if (orderOpt.isEmpty()) {
            throw new BadRequestException("Pedido no encontrado");
        }
        Order order = orderOpt.get();
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !order.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        byte[] pdfBytes = sunatService.obtenerPdfBoleta(orderId);
        Order refreshed = orderService.obtenerPorId(orderId).orElse(order);
        boolean esFactura = Integer.valueOf(1).equals(refreshed.getBoletaTipoComprobante())
                || (refreshed.getBoletaSerie() != null && refreshed.getBoletaSerie().startsWith("F"));
        String serie = refreshed.getBoletaSerie() != null ? refreshed.getBoletaSerie() : (esFactura ? "FFF1" : "BBB1");
        int num = refreshed.getBoletaNumero() != null ? refreshed.getBoletaNumero() : orderId;
        String prefix = esFactura ? "Factura" : "Boleta";
        String filename = String.format("%s_%s-%06d.pdf", prefix, serie, num);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        headers.setContentDisposition(disposition);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /**
     * Emite una Nota de Crédito Electrónica (FFF1) dirigida a la Boleta de Venta Electrónica de un pedido
     * para anularla ante SUNAT, marca el pedido como CANCELLED y envía el PDF por correo al cliente.
     */
    @PostMapping("/nota-credito/order/{orderId}/emitir")
    public ResponseEntity<ApiResponse<NotaCreditoEmisionDTO>> emitirNotaCreditoPedido(
            @PathVariable("orderId") Integer orderId,
            @RequestBody(required = false) EmitirNotaCreditoRequestDTO body) {
        if (!securityUtils.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Solo administradores pueden emitir Notas de Crédito Electrónicas."));
        }

        Optional<Order> orderOpt = orderService.obtenerPorId(orderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Pedido no encontrado"));
        }

        String motivo = body != null && body.motivo() != null ? body.motivo().trim() : null;
        boolean enviarCorreo = body == null || !Boolean.FALSE.equals(body.enviarCorreo());

        NotaCreditoEmisionDTO result = sunatService.emitirNotaCreditoParaPedido(orderId, motivo);

        Order refreshed = orderService.obtenerPorId(orderId).orElse(orderOpt.get());
        if (refreshed.getStatus() != Order.OrderStatus.CANCELLED) {
            refreshed.setStatus(Order.OrderStatus.CANCELLED);
            orderService.actualizarOrder(orderId, refreshed);
        }
        if (paymentService != null) {
            paymentService.obtenerPorOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() != Payment.PaymentStatus.FAILED) {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    paymentService.actualizarPayment(payment.getId(), payment);
                }
            });
        }

        if (enviarCorreo && emailService != null) {
            try {
                emailService.sendNotaCreditoEmailForOrder(orderId);
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.ok(ApiResponse.success(
                result,
                "Nota de Crédito Electrónica " + result.comprobante()
                        + " emitida correctamente anulando Boleta " + result.boletaModificadaComprobante() + "."
        ));
    }

    /**
     * Descarga o visualiza en el navegador el PDF de la Nota de Crédito Electrónica de un pedido.
     */
    @GetMapping("/nota-credito/order/{orderId}/pdf")
    public ResponseEntity<byte[]> descargarPdfNotaCredito(
            @PathVariable("orderId") Integer orderId,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {
        Optional<Order> orderOpt = orderService.obtenerPorId(orderId);
        if (orderOpt.isEmpty()) {
            throw new BadRequestException("Pedido no encontrado");
        }
        Order order = orderOpt.get();
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !order.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        byte[] pdfBytes = sunatService.obtenerPdfNotaCredito(orderId);
        Order refreshed = orderService.obtenerPorId(orderId).orElse(order);
        String serie = refreshed.getNcSerie() != null ? refreshed.getNcSerie() : "FFF1";
        int num = refreshed.getNcNumero() != null ? refreshed.getNcNumero() : orderId;
        String filename = String.format("NotaCredito_%s-%06d.pdf", serie, num);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        headers.setContentDisposition(disposition);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
