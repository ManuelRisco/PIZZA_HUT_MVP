package com.example.services;

import com.example.dtos.OrderCompleteDTO;
import com.example.dtos.StockAlertDTO;
import com.example.models.Usuario;
import com.example.repositories.UsuarioRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmailService {

    public record OrderEmailDispatchResult(
            boolean customerEmailSent,
            String customerEmail,
            boolean adminEmailSent,
            int adminRecipientsCount
    ) {
        public static OrderEmailDispatchResult notSent(String customerEmail) {
            return new OrderEmailDispatchResult(false, customerEmail, false, 0);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String configuredFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        UsuarioRepository usuarioRepository,
                        JdbcTemplate jdbcTemplate) {
        this.mailSender = mailSenderProvider != null ? mailSenderProvider.getIfAvailable() : null;
        this.usuarioRepository = usuarioRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isMailOperational() {
        String sender = resolveFromAddress();
        return mailEnabled && mailSender != null && sender != null && !sender.isBlank();
    }

    public String resolveFromAddress() {
        if (configuredFrom != null && !configuredFrom.isBlank()) {
            return configuredFrom.trim();
        }
        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername.trim();
        }
        return null;
    }

    public List<String> resolveAdminEmails() {
        Set<String> recipients = new LinkedHashSet<>();
        List<Usuario> admins = usuarioRepository.findByRoleAndDeletedAtIsNull(Usuario.Role.ADMIN);
        if (admins != null) {
            for (Usuario admin : admins) {
                if (isDeliverableEmail(admin.getEmail())) {
                    recipients.add(admin.getEmail().trim());
                }
            }
        }
        if (recipients.isEmpty()) {
            String fallback = resolveFromAddress();
            if (isDeliverableEmail(fallback)) {
                recipients.add(fallback);
            }
        }
        return new ArrayList<>(recipients);
    }

    @Async("mailTaskExecutor")
    public void sendOrderCreatedEmailsAsync(Integer orderId) {
        sendOrderCreatedEmails(orderId);
    }

    public OrderEmailDispatchResult sendOrderCreatedEmails(Integer orderId) {
        if (orderId == null) {
            return OrderEmailDispatchResult.notSent(null);
        }
        OrderCompleteDTO order = null;
        try {
            order = loadCompleteOrder(orderId);
        } catch (Exception ex) {
            log.warn("No se pudo cargar informaci\u00f3n completa del pedido #{}: {}", orderId, ex.getMessage());
        }

        String customerEmail = order != null && order.getUserEmail() != null ? order.getUserEmail().trim() : null;
        if (!isMailOperational()) {
            log.debug("Servicio de correo deshabilitado o sin credenciales en .env; se omite notificaci\u00f3n de pedido #{}.", orderId);
            return OrderEmailDispatchResult.notSent(customerEmail);
        }
        if (order == null) {
            log.warn("No se encontr\u00f3 informaci\u00f3n del pedido #{} para enviar correos.", orderId);
            return OrderEmailDispatchResult.notSent(null);
        }

        boolean customerSent = false;
        boolean adminSent = false;
        int adminCount = 0;

        // 1. Correo de confirmacion para el cliente que realizo el pedido
        if (isDeliverableEmail(customerEmail)) {
            try {
                String customerSubject = "Confirmaci\u00f3n de tu Pedido #" + order.getId() + " - Pizza Hut";
                String customerHtml = buildCustomerOrderHtml(order);
                sendHtmlMessage(List.of(customerEmail), customerSubject, customerHtml);
                customerSent = true;
                log.info("Correo de confirmaci\u00f3n del pedido #{} enviado al cliente {}.", order.getId(), customerEmail);
            } catch (Exception ex) {
                log.error("Error al enviar correo al cliente ({}) para pedido #{}: {}", customerEmail, orderId, ex.getMessage());
            }
        } else {
            log.warn("El cliente del pedido #{} no posee un correo v\u00e1lido entregable ({}).", order.getId(), customerEmail);
        }

        // 2. Correo operativo para todos los administradores activos
        List<String> adminEmails = resolveAdminEmails();
        if (!adminEmails.isEmpty()) {
            try {
                String totalText = formatMoney(order.getTotal());
                String adminSubject = "Nuevo Pedido #" + order.getId() + " Recibido (" + totalText + ") - Administraci\u00f3n Pizza Hut";
                String adminHtml = buildAdminOrderHtml(order);
                sendHtmlMessage(adminEmails, adminSubject, adminHtml);
                adminSent = true;
                adminCount = adminEmails.size();
                log.info("Notificaci\u00f3n administrativa del pedido #{} enviada a {} administrador(es): {}",
                        order.getId(), adminEmails.size(), adminEmails);
            } catch (Exception ex) {
                log.error("Error al enviar correo administrativo para pedido #{}: {}", orderId, ex.getMessage());
            }
        } else {
            log.warn("No se encontraron correos de administradores activos para notificar el pedido #{}.", order.getId());
        }

        return new OrderEmailDispatchResult(customerSent, customerEmail, adminSent, adminCount);
    }

    public List<String> sendStockShortageAlertEmail(List<StockAlertDTO> alerts) throws Exception {
        if (alerts == null || alerts.isEmpty() || !isMailOperational()) {
            return List.of();
        }

        List<String> adminEmails = resolveAdminEmails();
        if (adminEmails.isEmpty()) {
            log.warn("No existen administradores activos con correo v\u00e1lido para recibir alerta de falta de stock.");
            return List.of();
        }

        boolean hasOutOfStock = alerts.stream().anyMatch(a -> "OUT_OF_STOCK".equalsIgnoreCase(a.getAlertLevel()));
        String subject = hasOutOfStock
                ? "URGENTE: Stock Agotado detectado en Inventario (" + alerts.size() + " producto(s) en alerta) - Pizza Hut"
                : "Alerta Consolidada de Falta de Stock (" + alerts.size() + " producto(s) bajo m\u00ednimo) - Pizza Hut";

        String html = buildStockAlertHtml(alerts, hasOutOfStock);
        sendHtmlMessage(adminEmails, subject, html);
        log.info("Alerta consolidada de falta de stock ({} productos) enviada exitosamente a administradores: {}",
                alerts.size(), adminEmails);
        return adminEmails;
    }

    protected void sendHtmlMessage(List<String> toAddresses, String subject, String htmlBody) throws Exception {
        String from = resolveFromAddress();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(new InternetAddress(from, "Pizza Hut Notificaciones", StandardCharsets.UTF_8.name()));
        helper.setTo(toAddresses.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private OrderCompleteDTO loadCompleteOrder(Integer orderId) {
        String sql = """
            SELECT o.id AS order_id, o.delivery_type, o.status, o.subtotal, o.deliveryFee, o.discount,
                   o.promo_code, o.total, o.notes, o.estimatedDelivery, o.createdAt AS order_date,
                   u.id AS user_id, u.name AS customer_name, u.email AS customer_email, u.phone AS customer_phone,
                   a.id AS address_id, a.line1 AS address_line1, a.city AS address_city,
                   a.district AS address_district, a.reference AS address_reference,
                   pm.id AS payment_method_id, pm.name AS payment_method_name
            FROM orders o
            INNER JOIN users u ON o.userId = u.id
            LEFT JOIN addresses a ON o.addressId = a.id
            LEFT JOIN payment_methods pm ON o.paymentMethodId = pm.id
            WHERE o.id = ?
            """;

        List<OrderCompleteDTO> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            OrderCompleteDTO dto = new OrderCompleteDTO();
            dto.setId(rs.getInt("order_id"));
            dto.setUserId(rs.getInt("user_id"));
            dto.setUserName(rs.getString("customer_name"));
            dto.setUserEmail(rs.getString("customer_email"));
            dto.setUserPhone(rs.getString("customer_phone"));
            dto.setAddressId(rs.getObject("address_id") != null ? rs.getInt("address_id") : null);
            dto.setAddressLine1(rs.getString("address_line1"));
            dto.setAddressCity(rs.getString("address_city"));
            dto.setAddressDistrict(rs.getString("address_district"));
            dto.setAddressReference(rs.getString("address_reference"));
            dto.setDeliveryType(rs.getString("delivery_type"));
            dto.setStatus(rs.getString("status"));
            dto.setPaymentMethodId(rs.getObject("payment_method_id") != null ? rs.getInt("payment_method_id") : null);
            dto.setPaymentMethodName(rs.getString("payment_method_name"));
            dto.setSubtotal(rs.getBigDecimal("subtotal"));
            dto.setDeliveryFee(rs.getBigDecimal("deliveryFee"));
            dto.setDiscount(rs.getBigDecimal("discount") != null ? rs.getBigDecimal("discount") : BigDecimal.ZERO);
            dto.setPromoCode(rs.getString("promo_code"));
            dto.setTotal(rs.getBigDecimal("total"));
            dto.setNotes(rs.getString("notes"));
            Timestamp est = rs.getTimestamp("estimatedDelivery");
            dto.setEstimatedDelivery(est != null ? est.toLocalDateTime() : null);
            Timestamp created = rs.getTimestamp("order_date");
            dto.setCreatedAt(created != null ? created.toLocalDateTime() : LocalDateTime.now());
            return dto;
        }, orderId);

        if (results.isEmpty()) {
            return null;
        }

        OrderCompleteDTO order = results.get(0);
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

        order.setItems(items);
        return order;
    }

    private String buildCustomerOrderHtml(OrderCompleteDTO order) {
        String customerName = order.getUserName() != null && !order.getUserName().isBlank()
                ? escapeHtml(order.getUserName()) : "Cliente";
        String deliveryLabel = "PICKUP".equalsIgnoreCase(order.getDeliveryType())
                ? "Recojo en tienda" : "Entrega a domicilio";
        String addressHtml = buildAddressSummaryHtml(order);
        String paymentMethod = order.getPaymentMethodName() != null && !order.getPaymentMethodName().isBlank()
                ? escapeHtml(order.getPaymentMethodName()) : "Por confirmar";
        String estimated = order.getEstimatedDelivery() != null
                ? order.getEstimatedDelivery().format(DATE_TIME_FMT) : "30 - 45 min aprox.";
        String createdDate = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_TIME_FMT) : LocalDateTime.now().format(DATE_TIME_FMT);

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#f4f4f6;font-family:'Segoe UI',Arial,sans-serif;color:#1f2937;">
              <div style="max-width:640px;margin:24px auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,0.08);">
                <div style="background:#c8102e;color:#ffffff;padding:24px 28px;">
                  <h1 style="margin:0;font-size:22px;font-weight:700;">\u00a1Gracias por tu pedido en Pizza Hut!</h1>
                  <p style="margin:6px 0 0;font-size:14px;opacity:0.95;">Pedido <strong>#%d</strong> registrado el %s</p>
                </div>
                <div style="padding:24px 28px;">
                  <p style="margin:0 0 16px;font-size:15px;">Hola <strong>%s</strong>, hemos recibido tu pedido correctamente y nuestro equipo ya lo est\u00e1 procesando.</p>
                  <div style="background:#fff8f8;border:1px solid #f3d2d7;border-radius:8px;padding:14px 16px;margin-bottom:20px;font-size:14px;">
                    <p style="margin:4px 0;"><strong>Modalidad:</strong> %s</p>
                    <p style="margin:4px 0;"><strong>Direcci\u00f3n / Entrega:</strong> %s</p>
                    <p style="margin:4px 0;"><strong>M\u00e9todo de pago:</strong> %s</p>
                    <p style="margin:4px 0;"><strong>Entrega estimada:</strong> %s</p>
                    %s
                  </div>
                  <h3 style="margin:0 0 10px;font-size:16px;color:#111827;">Detalle de tu pedido</h3>
                  %s
                  %s
                  <p style="margin:24px 0 0;font-size:13px;color:#6b7280;text-align:center;">
                    Si tienes alguna consulta sobre tu pedido #%d, responde a este correo o cont\u00e1ctanos indicando tu n\u00famero de orden.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                order.getId(),
                createdDate,
                customerName,
                deliveryLabel,
                addressHtml,
                paymentMethod,
                estimated,
                buildNotesRowHtml(order.getNotes()),
                buildOrderItemsTableHtml(order.getItems()),
                buildOrderTotalsHtml(order),
                order.getId()
        );
    }

    private String buildAdminOrderHtml(OrderCompleteDTO order) {
        String customerName = order.getUserName() != null && !order.getUserName().isBlank()
                ? escapeHtml(order.getUserName()) : "Usuario #" + order.getUserId();
        String customerEmail = order.getUserEmail() != null ? escapeHtml(order.getUserEmail()) : "No registrado";
        String customerPhone = order.getUserPhone() != null && !order.getUserPhone().isBlank()
                ? escapeHtml(order.getUserPhone()) : "No registrado";
        String deliveryLabel = "PICKUP".equalsIgnoreCase(order.getDeliveryType())
                ? "RECOJO EN TIENDA" : "DELIVERY A DOMICILIO";
        String addressHtml = buildAddressSummaryHtml(order);
        String paymentMethod = order.getPaymentMethodName() != null && !order.getPaymentMethodName().isBlank()
                ? escapeHtml(order.getPaymentMethodName()) : "ID #" + order.getPaymentMethodId();
        String createdDate = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_TIME_FMT) : LocalDateTime.now().format(DATE_TIME_FMT);

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:'Segoe UI',Arial,sans-serif;color:#111827;">
              <div style="max-width:680px;margin:24px auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,0.1);">
                <div style="background:#111827;color:#ffffff;padding:20px 28px;border-bottom:4px solid #c8102e;">
                  <div style="font-size:12px;text-transform:uppercase;letter-spacing:1px;color:#fca5a5;">Panel Administrativo \u00b7 Alerta de Venta</div>
                  <h1 style="margin:4px 0 0;font-size:22px;">Nuevo Pedido #%d \u2014 %s</h1>
                </div>
                <div style="padding:24px 28px;">
                  <div style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:14px 16px;margin-bottom:20px;font-size:14px;">
                    <p style="margin:4px 0;"><strong>Fecha y Hora:</strong> %s</p>
                    <p style="margin:4px 0;"><strong>Cliente:</strong> %s (ID #%d)</p>
                    <p style="margin:4px 0;"><strong>Correo Cliente:</strong> %s</p>
                    <p style="margin:4px 0;"><strong>Tel\u00e9fono:</strong> %s</p>
                    <p style="margin:4px 0;"><strong>Modalidad:</strong> <span style="color:#c8102e;font-weight:700;">%s</span></p>
                    <p style="margin:4px 0;"><strong>Direcci\u00f3n:</strong> %s</p>
                    <p style="margin:4px 0;"><strong>M\u00e9todo de Pago:</strong> %s</p>
                    %s
                  </div>
                  <h3 style="margin:0 0 10px;font-size:16px;">\u00cdtems para preparaci\u00f3n</h3>
                  %s
                  %s
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                order.getId(),
                formatMoney(order.getTotal()),
                createdDate,
                customerName,
                order.getUserId(),
                customerEmail,
                customerPhone,
                deliveryLabel,
                addressHtml,
                paymentMethod,
                buildNotesRowHtml(order.getNotes()),
                buildOrderItemsTableHtml(order.getItems()),
                buildOrderTotalsHtml(order)
        );
    }

    private String buildOrderItemsTableHtml(List<OrderCompleteDTO.OrderItemCompleteDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p style=\"font-size:14px;color:#6b7280;\">Pedido registrado sin desglose de \u00edtems.</p>";
        }

        StringBuilder rows = new StringBuilder();
        for (OrderCompleteDTO.OrderItemCompleteDTO item : items) {
            String baseName = "EXTRA".equalsIgnoreCase(item.getItemType())
                    ? (item.getExtraName() != null ? item.getExtraName() : "Complemento")
                    : (item.getPizzaName() != null ? item.getPizzaName() : "Pizza");
            StringBuilder desc = new StringBuilder("<strong>").append(escapeHtml(baseName)).append("</strong>");
            if (item.getSizeName() != null && !item.getSizeName().isBlank()) {
                desc.append(" <span style=\"color:#4b5563;\">(").append(escapeHtml(item.getSizeName())).append(")</span>");
            }
            if (item.getExtras() != null && !item.getExtras().isEmpty()) {
                String extrasJoined = item.getExtras().stream()
                        .map(this::escapeHtml)
                        .collect(Collectors.joining(", "));
                desc.append("<br><span style=\"font-size:12px;color:#6b7280;\">+ Toppings extra: ")
                        .append(extrasJoined).append("</span>");
            }

            rows.append("""
                <tr>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:14px;">%s</td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:14px;text-align:center;">%d</td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:14px;text-align:right;">%s</td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:14px;text-align:right;font-weight:600;">%s</td>
                </tr>
                """.formatted(
                    desc.toString(),
                    item.getQuantity() != null ? item.getQuantity() : 1,
                    formatMoney(item.getUnitPrice()),
                    formatMoney(item.getLineTotal())
            ));
        }

        return """
            <table style="width:100%%;border-collapse:collapse;margin-bottom:16px;">
              <thead>
                <tr style="background:#f3f4f6;text-align:left;">
                  <th style="padding:10px 8px;font-size:13px;border-bottom:2px solid #d1d5db;">Producto</th>
                  <th style="padding:10px 8px;font-size:13px;border-bottom:2px solid #d1d5db;text-align:center;">Cant.</th>
                  <th style="padding:10px 8px;font-size:13px;border-bottom:2px solid #d1d5db;text-align:right;">P. Unit.</th>
                  <th style="padding:10px 8px;font-size:13px;border-bottom:2px solid #d1d5db;text-align:right;">Subtotal</th>
                </tr>
              </thead>
              <tbody>%s</tbody>
            </table>
            """.formatted(rows.toString());
    }

    private String buildOrderTotalsHtml(OrderCompleteDTO order) {
        StringBuilder discountRow = new StringBuilder();
        if (order.getDiscount() != null && order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            String promoLabel = order.getPromoCode() != null && !order.getPromoCode().isBlank()
                    ? " (" + escapeHtml(order.getPromoCode()) + ")" : "";
            discountRow.append("""
                <tr>
                  <td style="padding:4px 0;color:#16a34a;">Descuento%s:</td>
                  <td style="padding:4px 0;text-align:right;color:#16a34a;font-weight:600;">-%s</td>
                </tr>
                """.formatted(promoLabel, formatMoney(order.getDiscount())));
        }

        return """
            <table style="width:100%%;max-width:300px;margin-left:auto;border-collapse:collapse;font-size:14px;">
              <tr>
                <td style="padding:4px 0;color:#4b5563;">Subtotal:</td>
                <td style="padding:4px 0;text-align:right;">%s</td>
              </tr>
              %s
              <tr>
                <td style="padding:4px 0;color:#4b5563;">Costo de env\u00edo:</td>
                <td style="padding:4px 0;text-align:right;">%s</td>
              </tr>
              <tr style="border-top:2px solid #111827;font-size:16px;font-weight:700;">
                <td style="padding:8px 0;color:#111827;">Total:</td>
                <td style="padding:8px 0;text-align:right;color:#c8102e;">%s</td>
              </tr>
            </table>
            """.formatted(
                formatMoney(order.getSubtotal()),
                discountRow.toString(),
                formatMoney(order.getDeliveryFee()),
                formatMoney(order.getTotal())
        );
    }

    private String buildStockAlertHtml(List<StockAlertDTO> alerts, boolean hasOutOfStock) {
        String headerBg = hasOutOfStock ? "#991b1b" : "#b45309";
        String bannerTitle = hasOutOfStock
                ? "Alerta Cr\u00edtica Consolidada: Stock Agotado / Bajo M\u00ednimo (" + alerts.size() + " producto(s))"
                : "Alerta Preventiva Consolidada: Falta de Stock (" + alerts.size() + " producto(s))";

        StringBuilder rows = new StringBuilder();
        for (StockAlertDTO a : alerts) {
            boolean isOut = "OUT_OF_STOCK".equalsIgnoreCase(a.getAlertLevel());
            String badgeBg = isOut ? "#fee2e2" : "#fef3c7";
            String badgeColor = isOut ? "#991b1b" : "#92400e";
            String badgeText = isOut ? "AGOTADO" : "STOCK BAJO";
            String typeLabel = switch (a.getItemType() != null ? a.getItemType().toUpperCase() : "") {
                case "INGREDIENT" -> "Insumo";
                case "EXTRA" -> "Extra";
                case "SIZE" -> "Masa";
                default -> escapeHtml(a.getItemType());
            };
            String unit = a.getUnit() != null ? escapeHtml(a.getUnit()) : "";
            String flow = escapeHtml((a.getMovementDirection() != null ? a.getMovementDirection() : "SALIDA")
                    + (a.getMovementType() != null ? " \u00b7 " + translateMovementType(a.getMovementType()) : ""));
            String reason = a.getReason() != null && !a.getReason().isBlank()
                    ? escapeHtml(a.getReason()) : "Detecci\u00f3n autom\u00e1tica por trigger SQL";

            rows.append("""
                <tr>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:13px;">
                    <strong>%s</strong><br>
                    <span style="color:#6b7280;font-size:12px;">%s (ID #%d)</span>
                  </td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:12px;">%s</td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:13px;text-align:right;">%s %s</td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:13px;text-align:right;font-weight:700;color:%s;">%s %s</td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:13px;text-align:right;">%s %s</td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;text-align:center;">
                    <span style="display:inline-block;padding:3px 8px;border-radius:999px;font-size:11px;font-weight:700;background:%s;color:%s;">%s</span>
                  </td>
                  <td style="padding:10px 8px;border-bottom:1px solid #e5e7eb;font-size:12px;color:#374151;">%s</td>
                </tr>
                """.formatted(
                    escapeHtml(a.getItemName()),
                    typeLabel,
                    a.getItemId() != null ? a.getItemId() : 0,
                    flow,
                    formatQuantity(a.getPreviousStock(), unit), unit,
                    badgeColor,
                    formatQuantity(a.getNewStock(), unit), unit,
                    formatQuantity(a.getMinStock(), unit), unit,
                    badgeBg, badgeColor, badgeText,
                    reason
            ));
        }

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:'Segoe UI',Arial,sans-serif;color:#111827;">
              <div style="max-width:780px;margin:24px auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,0.1);">
                <div style="background:%s;color:#ffffff;padding:20px 28px;">
                  <div style="font-size:12px;text-transform:uppercase;letter-spacing:1px;opacity:0.9;">Control Autom\u00e1tico de Inventario (Lote Consolidado)</div>
                  <h1 style="margin:4px 0 0;font-size:21px;">%s</h1>
                </div>
                <div style="padding:24px 28px;">
                  <p style="margin:0 0 16px;font-size:14px;color:#374151;">
                    Los triggers de entradas y salidas han agrupado en este reporte los productos que se encuentran <strong>por debajo o igual al stock m\u00ednimo permitido</strong>. Se recomienda gestionar su reposici\u00f3n en el m\u00f3dulo de Inventario / Kardex.
                  </p>
                  <table style="width:100%%;border-collapse:collapse;margin-bottom:18px;">
                    <thead>
                      <tr style="background:#f9fafb;text-align:left;">
                        <th style="padding:10px 8px;font-size:12px;border-bottom:2px solid #d1d5db;">\u00cdtem</th>
                        <th style="padding:10px 8px;font-size:12px;border-bottom:2px solid #d1d5db;">Movimiento</th>
                        <th style="padding:10px 8px;font-size:12px;border-bottom:2px solid #d1d5db;text-align:right;">Anterior</th>
                        <th style="padding:10px 8px;font-size:12px;border-bottom:2px solid #d1d5db;text-align:right;">Actual</th>
                        <th style="padding:10px 8px;font-size:12px;border-bottom:2px solid #d1d5db;text-align:right;">M\u00ednimo</th>
                        <th style="padding:10px 8px;font-size:12px;border-bottom:2px solid #d1d5db;text-align:center;">Estado</th>
                        <th style="padding:10px 8px;font-size:12px;border-bottom:2px solid #d1d5db;">Detalle</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                  <p style="margin:0;font-size:12px;color:#6b7280;">
                    Fecha de env\u00edo: %s \u00b7 No se volver\u00e1n a enviar correos adicionales para estos productos hasta que su stock sea restablecido superando el stock m\u00ednimo.
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                headerBg,
                bannerTitle,
                rows.toString(),
                LocalDateTime.now().format(DATE_TIME_FMT)
        );
    }

    private String buildAddressSummaryHtml(OrderCompleteDTO order) {
        if ("PICKUP".equalsIgnoreCase(order.getDeliveryType())) {
            return "Recojo presencial en local Pizza Hut";
        }
        List<String> parts = new ArrayList<>();
        if (order.getAddressLine1() != null && !order.getAddressLine1().isBlank()) {
            parts.add(escapeHtml(order.getAddressLine1()));
        }
        if (order.getAddressDistrict() != null && !order.getAddressDistrict().isBlank()) {
            parts.add(escapeHtml(order.getAddressDistrict()));
        }
        if (order.getAddressCity() != null && !order.getAddressCity().isBlank()) {
            parts.add(escapeHtml(order.getAddressCity()));
        }
        String joined = parts.isEmpty() ? "Direcci\u00f3n registrada en cuenta" : String.join(", ", parts);
        if (order.getAddressReference() != null && !order.getAddressReference().isBlank()) {
            joined += " <em>(Ref: " + escapeHtml(order.getAddressReference()) + ")</em>";
        }
        return joined;
    }

    private String buildNotesRowHtml(String notes) {
        if (notes == null || notes.isBlank()) {
            return "";
        }
        return "<p style=\"margin:4px 0;\"><strong>Notas del pedido:</strong> " + escapeHtml(notes) + "</p>";
    }

    private String translateMovementType(String movementType) {
        if (movementType == null) return "";
        return switch (movementType.toUpperCase()) {
            case "SALE" -> "Venta";
            case "LOSS" -> "Merma";
            case "ADJUSTMENT" -> "Ajuste";
            case "PURCHASE" -> "Compra";
            case "RETURN" -> "Devoluci\u00f3n";
            case "DIRECT_UPDATE" -> "Edici\u00f3n directa";
            default -> movementType;
        };
    }

    private boolean isDeliverableEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String trimmed = email.trim().toLowerCase();
        if (!trimmed.contains("@") || trimmed.endsWith("@t.co") || trimmed.endsWith("@example.com") || trimmed.endsWith("@test.local")) {
            return false;
        }
        return true;
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal safe = amount != null ? amount : BigDecimal.ZERO;
        return "S/ " + safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatQuantity(BigDecimal qty, String unit) {
        BigDecimal safe = qty != null ? qty : BigDecimal.ZERO;
        if ("und".equalsIgnoreCase(unit)) {
            return safe.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public boolean sendPasswordResetEmail(String recipientEmail, String userName, String resetCode, int expirationMinutes) {
        if (!isDeliverableEmail(recipientEmail)) {
            log.warn("No se puede enviar c\u00f3digo de recuperaci\u00f3n a un correo no entregable: {}", recipientEmail);
            return false;
        }
        if (!isMailOperational()) {
            log.warn("Servicio de correo inactivo; c\u00f3digo de recuperaci\u00f3n generado para {}", recipientEmail);
            return false;
        }
        try {
            String safeName = (userName != null && !userName.isBlank()) ? escapeHtml(userName.trim()) : "Cliente";
            String safeCode = escapeHtml(resetCode != null ? resetCode.trim() : "");
            String subject = "C\u00f3digo de recuperaci\u00f3n de contrase\u00f1a - Pizza Hut";
            String html = """
                    <!DOCTYPE html>
                    <html lang="es">
                    <head><meta charset="UTF-8"></head>
                    <body style="margin:0;padding:0;background-color:#f4f4f6;font-family:Arial,Helvetica,sans-serif;color:#222;">
                      <div style="max-width:580px;margin:24px auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,0.08);">
                        <div style="background:linear-gradient(135deg,#c8102e,#8c0b20);color:#ffffff;padding:24px 28px;text-align:center;">
                          <h1 style="margin:0;font-size:22px;letter-spacing:0.5px;">Recuperaci\u00f3n de Contrase\u00f1a</h1>
                          <p style="margin:6px 0 0;font-size:14px;opacity:0.92;">Solicitud de restablecimiento de acceso</p>
                        </div>
                        <div style="padding:26px 28px;">
                          <p style="margin:0 0 14px;font-size:15px;">Hola <strong>%s</strong>,</p>
                          <p style="margin:0 0 18px;font-size:14px;line-height:1.6;color:#444;">
                            Recibimos una solicitud para restablecer la contrase\u00f1a de tu cuenta. Ingresa el siguiente c\u00f3digo de verificaci\u00f3n de 6 d\u00edgitos en la pantalla de recuperaci\u00f3n:
                          </p>
                          <div style="margin:22px 0;padding:18px;background:#fff5f6;border:2px dashed #c8102e;border-radius:10px;text-align:center;">
                            <div style="font-size:12px;text-transform:uppercase;color:#8c0b20;letter-spacing:1px;margin-bottom:6px;">C\u00f3digo de Verificaci\u00f3n</div>
                            <div style="font-size:32px;font-weight:bold;letter-spacing:8px;color:#c8102e;font-family:monospace;">%s</div>
                            <div style="font-size:12px;color:#666;margin-top:8px;">V\u00e1lido durante <strong>%d minutos</strong></div>
                          </div>
                          <p style="margin:0 0 10px;font-size:13px;color:#666;line-height:1.5;">
                            Si t\u00fa no solicitaste este cambio, puedes ignorar este mensaje con total tranquilidad; tu contrase\u00f1a actual seguir\u00e1 protegida.
                          </p>
                        </div>
                        <div style="background:#f9fafb;padding:14px 28px;text-align:center;font-size:12px;color:#888;border-top:1px solid #eee;">
                          Enviado el %s &middot; Seguridad de Cuentas Pizza Hut
                        </div>
                      </div>
                    </body>
                    </html>
                    """.formatted(safeName, safeCode, expirationMinutes, LocalDateTime.now().format(DATE_TIME_FMT));

            sendHtmlMessage(List.of(recipientEmail.trim()), subject, html);
            log.info("Correo con c\u00f3digo de recuperaci\u00f3n enviado exitosamente a {}.", recipientEmail);
            return true;
        } catch (Exception ex) {
            log.error("Error al enviar correo de recuperaci\u00f3n a {}: {}", recipientEmail, ex.getMessage());
            return false;
        }
    }

    @Async("mailTaskExecutor")
    public void sendPasswordChangedConfirmationEmailAsync(String recipientEmail, String userName) {
        if (!isDeliverableEmail(recipientEmail) || !isMailOperational()) {
            return;
        }
        try {
            String safeName = (userName != null && !userName.isBlank()) ? escapeHtml(userName.trim()) : "Cliente";
            String subject = "Tu contrase\u00f1a ha sido actualizada - Pizza Hut";
            String html = """
                    <!DOCTYPE html>
                    <html lang="es">
                    <head><meta charset="UTF-8"></head>
                    <body style="margin:0;padding:0;background-color:#f4f4f6;font-family:Arial,Helvetica,sans-serif;color:#222;">
                      <div style="max-width:560px;margin:24px auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,0.08);">
                        <div style="background:linear-gradient(135deg,#15803d,#166534);color:#ffffff;padding:20px 26px;text-align:center;">
                          <h2 style="margin:0;font-size:20px;">Contrase\u00f1a Restablecida con \u00c9xito</h2>
                        </div>
                        <div style="padding:22px 26px;font-size:14px;line-height:1.6;color:#333;">
                          <p>Hola <strong>%s</strong>,</p>
                          <p>Te confirmamos que la contrase\u00f1a de tu cuenta fue actualizada correctamente el <strong>%s</strong>.</p>
                          <p style="color:#666;font-size:13px;">Ya puedes iniciar sesi\u00f3n con tu nueva contrase\u00f1a.</p>
                        </div>
                      </div>
                    </body>
                    </html>
                    """.formatted(safeName, LocalDateTime.now().format(DATE_TIME_FMT));
            sendHtmlMessage(List.of(recipientEmail.trim()), subject, html);
        } catch (Exception ex) {
            log.debug("Aviso: no se pudo enviar confirmaci\u00f3n de cambio de contrase\u00f1a a {}: {}", recipientEmail, ex.getMessage());
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
