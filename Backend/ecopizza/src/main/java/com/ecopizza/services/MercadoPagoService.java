package com.ecopizza.services;

import com.ecopizza.dtos.CheckoutRequestDTO;
import com.ecopizza.dtos.MercadoPagoChargeResultDTO;
import com.ecopizza.dtos.MercadoPagoConfigDTO;
import com.ecopizza.exceptions.BadRequestException;
import com.ecopizza.models.Order;
import com.ecopizza.models.PaymentMethod;
import com.ecopizza.models.Usuario;
import com.ecopizza.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Integración oficial con Mercado Pago Perú (Checkout API / Bricks & Yape SDK v2).
 * - El Access Token solo reside en el backend (.env).
 * - El monto cobrado siempre se calcula desde el servidor (anti-manipulación).
 * - Envía X-Idempotency-Key por transacción para evitar cobros duplicados.
 */
@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);
    private static final String MP_PAYMENTS_URL = "https://api.mercadopago.com/v1/payments";

    private final UsuarioRepository usuarioRepository;
    private final PaymentMethodService paymentMethodService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    @Value("${mercadopago.public.key:}")
    private String publicKey;

    @Value("${mercadopago.access.token:}")
    private String accessToken;

    @Value("${mercadopago.sandbox.mode:true}")
    private boolean sandboxMode;

    public MercadoPagoService(UsuarioRepository usuarioRepository,
                              PaymentMethodService paymentMethodService) {
        this.usuarioRepository = usuarioRepository;
        this.paymentMethodService = paymentMethodService;
    }

    public boolean isConfigured() {
        String pk = publicKey != null ? publicKey.trim() : "";
        String at = accessToken != null ? accessToken.trim() : "";
        return !pk.isEmpty()
                && !at.isEmpty()
                && !pk.startsWith("tu_")
                && !at.startsWith("tu_")
                && !pk.startsWith("TU_")
                && !at.startsWith("TU_");
    }

    public MercadoPagoConfigDTO obtenerConfiguracionPublica() {
        boolean configured = isConfigured();
        String pk = configured ? publicKey.trim() : "";
        boolean isTestCredentials = !configured || pk.startsWith("TEST-") || (accessToken != null && accessToken.trim().startsWith("TEST-"));

        return new MercadoPagoConfigDTO(
                configured,
                pk,
                sandboxMode || isTestCredentials,
                "PEN",
                "es-PE",
                List.of("CARD", "YAPE")
        );
    }

    /**
     * Determina si el método de pago elegido debe procesarse a través de Mercado Pago
     * (Tarjeta o Yape cuando las credenciales estén configuradas y el cliente envíe token).
     */
    public boolean requiresGatewayProcessing(Integer paymentMethodId, CheckoutRequestDTO.PaymentDTO paymentDto) {
        if (!isConfigured() || paymentDto == null) {
            return false;
        }
        if (paymentDto.getMercadoPagoToken() != null && !paymentDto.getMercadoPagoToken().isBlank()) {
            return true;
        }
        if (paymentMethodId != null) {
            PaymentMethod pm = paymentMethodService.obtenerPorId(paymentMethodId).orElse(null);
            if (pm != null && pm.getName() != null) {
                String lower = pm.getName().toLowerCase(Locale.ROOT);
                return lower.contains("tarjeta") || lower.contains("card") || lower.contains("yape");
            }
        }
        return false;
    }

    /**
     * Procesa un cobro real en Mercado Pago (Sandbox o Producción según las llaves en .env)
     * usando el token PCI-DSS generado en el navegador por Card Payment Brick o Yape SDK v2.
     */
    public MercadoPagoChargeResultDTO procesarPago(Order savedOrder, CheckoutRequestDTO.PaymentDTO paymentDto) {
        if (!isConfigured()) {
            return new MercadoPagoChargeResultDTO(
                    false,
                    null,
                    "LOCAL_MODE",
                    "credentials_not_configured",
                    null,
                    paymentDto != null ? paymentDto.getTransactionId() : null,
                    "Mercado Pago sin credenciales en .env; modo local activo."
            );
        }

        if (paymentDto == null || paymentDto.getMercadoPagoToken() == null || paymentDto.getMercadoPagoToken().isBlank()) {
            throw new BadRequestException(
                    "Falta el token seguro de Mercado Pago. Completa el formulario de pago en el módulo de Mercado Pago antes de confirmar.");
        }

        BigDecimal verifiedAmount = savedOrder.getTotal() != null
                ? savedOrder.getTotal().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        if (verifiedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El monto del pedido debe ser mayor a S/ 0.00 para cobrar en Mercado Pago.");
        }

        Usuario usuario = savedOrder.getUserId() != null
                ? usuarioRepository.findById(savedOrder.getUserId().longValue()).orElse(null)
                : null;

        String payerEmail = firstNonBlank(
                paymentDto.getMercadoPagoPayerEmail(),
                usuario != null ? usuario.getEmail() : null,
                "cliente@pizzahut-mvp.pe"
        );
        String mpMethodId = firstNonBlank(paymentDto.getMercadoPagoPaymentMethodId(), "visa").toLowerCase(Locale.ROOT);
        boolean isYape = "yape".equalsIgnoreCase(mpMethodId);

        int installments = isYape
                ? 1
                : (paymentDto.getMercadoPagoInstallments() != null && paymentDto.getMercadoPagoInstallments() > 0
                    ? paymentDto.getMercadoPagoInstallments()
                    : 1);

        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", payerEmail.trim());

        String docNumber = firstNonBlank(
                paymentDto.getMercadoPagoPayerDocNumber(),
                savedOrder.getBoletaClienteDni(),
                usuario != null ? usuario.getDni() : null
        );
        if (docNumber != null && docNumber.matches("^\\d{8,12}$")) {
            Map<String, Object> identification = new LinkedHashMap<>();
            identification.put("type", firstNonBlank(paymentDto.getMercadoPagoPayerDocType(), "DNI"));
            identification.put("number", docNumber);
            payer.put("identification", identification);
        }

        if (isYape) {
            String phone = firstNonBlank(
                    paymentDto.getMercadoPagoPayerPhone(),
                    usuario != null ? usuario.getPhone() : null
            );
            if (phone != null && !phone.isBlank()) {
                Map<String, Object> phoneMap = new LinkedHashMap<>();
                phoneMap.put("area_code", "+51");
                phoneMap.put("number", phone.replaceAll("\\D", ""));
                payer.put("phone", phoneMap);
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_amount", verifiedAmount);
        body.put("token", paymentDto.getMercadoPagoToken().trim());
        body.put("description", "Pedido Pizza Hut #" + savedOrder.getId());
        body.put("installments", installments);
        body.put("payment_method_id", mpMethodId);
        if (!isYape && paymentDto.getMercadoPagoIssuerId() != null && !paymentDto.getMercadoPagoIssuerId().isBlank()) {
            try {
                body.put("issuer_id", Long.parseLong(paymentDto.getMercadoPagoIssuerId().trim()));
            } catch (NumberFormatException ignored) {
                body.put("issuer_id", paymentDto.getMercadoPagoIssuerId().trim());
            }
        }
        body.put("external_reference", "ORDER-" + savedOrder.getId());
        body.put("payer", payer);

        String idempotencyKey = "ph-order-" + savedOrder.getId() + "-" + UUID.randomUUID();

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MP_PAYMENTS_URL))
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Idempotency-Key", idempotencyKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = response.body() != null && !response.body().isBlank()
                    ? objectMapper.readTree(response.body())
                    : null;

            if (response.statusCode() >= 200 && response.statusCode() < 300 && json != null) {
                String mpId = json.path("id").asText("");
                String status = json.path("status").asText("rejected").toLowerCase(Locale.ROOT);
                String statusDetail = json.path("status_detail").asText("");
                String resolvedMethod = json.path("payment_method_id").asText(mpMethodId);

                log.info("Respuesta Mercado Pago para pedido #{}: id={}, status={}, detail={}, method={}",
                        savedOrder.getId(), mpId, status, statusDetail, resolvedMethod);

                if ("approved".equalsIgnoreCase(status)) {
                    String txId = "MP-" + mpId;
                    return new MercadoPagoChargeResultDTO(
                            true,
                            mpId,
                            "approved",
                            statusDetail,
                            resolvedMethod,
                            txId,
                            "Pago aprobado por Mercado Pago (" + txId + ")"
                    );
                }

                String friendlyReason = translateStatusDetail(status, statusDetail);
                throw new BadRequestException("Pago rechazado por Mercado Pago: " + friendlyReason);
            }

            String apiError = extractMercadoPagoError(json, response.statusCode());
            log.warn("Error HTTP {} de Mercado Pago al cobrar pedido #{}: {}", response.statusCode(), savedOrder.getId(), apiError);
            throw new BadRequestException("Mercado Pago no pudo procesar el pago: " + apiError);

        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error de conexión con Mercado Pago para pedido #{}: {}", savedOrder.getId(), ex.getMessage());
            throw new BadRequestException("No se pudo conectar con la pasarela de Mercado Pago. Inténtalo nuevamente en unos segundos.");
        }
    }

    private String extractMercadoPagoError(JsonNode json, int statusCode) {
        if (json == null) {
            return "Error de comunicación (HTTP " + statusCode + ")";
        }
        String msg = json.path("message").asText("");
        if (json.has("cause") && json.get("cause").isArray() && !json.get("cause").isEmpty()) {
            JsonNode firstCause = json.get("cause").get(0);
            String desc = firstCause.path("description").asText("");
            String code = firstCause.path("code").asText("");
            if (!desc.isBlank()) {
                return desc + (code.isBlank() ? "" : " (código " + code + ")");
            }
        }
        return !msg.isBlank() ? msg : "Solicitud rechazada (HTTP " + statusCode + ")";
    }

    private String translateStatusDetail(String status, String detail) {
        if (detail == null) detail = "";
        return switch (detail) {
            case "cc_rejected_insufficient_amount" -> "Fondos insuficientes en la tarjeta.";
            case "cc_rejected_bad_filled_security_code" -> "El código de seguridad (CVV) es incorrecto.";
            case "cc_rejected_bad_filled_date" -> "La fecha de vencimiento de la tarjeta es inválida.";
            case "cc_rejected_bad_filled_card_number" -> "Revisa el número de tarjeta ingresado.";
            case "cc_rejected_bad_filled_other" -> "Revisa los datos ingresados de la tarjeta.";
            case "cc_rejected_call_for_authorize" -> "Debes autorizar este pago con tu banco emisor.";
            case "cc_rejected_card_disabled" -> "Tu tarjeta no está habilitada para compras online.";
            case "cc_rejected_duplicated_payment" -> "Ya realizaste un pago por ese mismo monto recientemente.";
            case "cc_rejected_high_risk" -> "Pago rechazado por políticas antifraude de Mercado Pago.";
            case "cc_rejected_max_attempts" -> "Superaste el límite de intentos permitidos con esta tarjeta.";
            case "cc_rejected_other_reason" -> "Tu banco o emisor no procesó el pago. Prueba con otro medio.";
            case "pending_contingency", "in_process", "pending_review_manual" -> "El pago quedó pendiente de revisión por Mercado Pago. Intenta con otra tarjeta.";
            default -> "Estado '" + status + "'" + (!detail.isBlank() ? " (" + detail + ")" : "") + ". Verifica los datos e inténtalo nuevamente.";
        };
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
