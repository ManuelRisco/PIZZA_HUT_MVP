package com.ecopizza.services;

import com.ecopizza.dtos.OrderCompleteDTO;
import com.ecopizza.dtos.OrderEmailDispatchResultDTO;
import com.ecopizza.dtos.StockAlertDTO;
import com.ecopizza.models.Usuario;
import com.ecopizza.repositories.UsuarioRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String MAIL_FONT = "'DM Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";
    private static final String COLOR_BG_OUTER = "#f4f4f5";
    private static final String COLOR_BG_CARD = "#ffffff";
    private static final String COLOR_BG_SUBTLE = "#f9fafb";
    private static final String COLOR_TEXT_PRIMARY = "#111827";
    private static final String COLOR_TEXT_SECONDARY = "#374151";
    private static final String COLOR_TEXT_MUTED = "#52525b";
    private static final String COLOR_BORDER_SUBTLE = "#e5e7eb";
    private static final String COLOR_BORDER_DEFAULT = "#d4d4d8";
    private static final String COLOR_BORDER_STRONG = "#111827";

    private final JavaMailSender mailSender;
    private final UsuarioRepository usuarioRepository;
    private final JdbcTemplate jdbcTemplate;
    private SunatService sunatService;

    @Autowired(required = false)
    public void setSunatService(SunatService sunatService) {
        this.sunatService = sunatService;
    }

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

    public OrderEmailDispatchResultDTO sendOrderCreatedEmails(Integer orderId) {
        if (orderId == null) {
            return OrderEmailDispatchResultDTO.notSent(null);
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
            return OrderEmailDispatchResultDTO.notSent(customerEmail);
        }
        if (order == null) {
            log.warn("No se encontr\u00f3 informaci\u00f3n del pedido #{} para enviar correos.", orderId);
            return OrderEmailDispatchResultDTO.notSent(null);
        }

        boolean customerSent = false;
        boolean adminSent = false;
        int adminCount = 0;

        // 1. Correo de confirmacion para el cliente que realizo el pedido (con PDF de Boleta Electronica adjunto solo si ya fue emitida)
        if (isDeliverableEmail(customerEmail)) {
            try {
                boolean boletaEmitida = order.getBoletaNumero() != null
                        || "EMITIDA".equalsIgnoreCase(order.getBoletaEstado());
                boolean esFactura = isFacturaOrder(order);
                String tipoDocNombre = esFactura ? "Factura Electr\u00f3nica" : "Boleta Electr\u00f3nica";
                String comprobanteLabel = order.getBoletaComprobante() != null ? order.getBoletaComprobante() : "";
                String customerSubject = boletaEmitida && !comprobanteLabel.isBlank()
                        ? "Confirmaci\u00f3n de tu Pedido #" + order.getId() + " y " + tipoDocNombre + " " + comprobanteLabel + " - Eco Pizza"
                        : "Confirmaci\u00f3n de tu Pedido #" + order.getId() + " - Eco Pizza";
                String customerHtml = buildCustomerOrderHtml(order);

                byte[] boletaPdfBytes = null;
                String boletaFilename = null;
                if (boletaEmitida && sunatService != null) {
                    try {
                        boletaPdfBytes = sunatService.obtenerPdfBoleta(order.getId());
                        String serie = order.getBoletaSerie() != null ? order.getBoletaSerie() : (esFactura ? "FFF1" : "BBB1");
                        int num = order.getBoletaNumero() != null ? order.getBoletaNumero() : order.getId();
                        boletaFilename = String.format("%s_%s-%06d.pdf", esFactura ? "Factura" : "Boleta", serie, num);
                    } catch (Exception pdfEx) {
                        log.warn("No se pudo adjuntar el PDF del comprobante al correo del pedido #{}: {}", order.getId(), pdfEx.getMessage());
                    }
                }

                if (boletaPdfBytes != null && boletaPdfBytes.length > 0 && boletaFilename != null) {
                    sendHtmlMessageWithAttachment(List.of(customerEmail), customerSubject, customerHtml,
                            boletaFilename, boletaPdfBytes, "application/pdf");
                } else {
                    sendHtmlMessage(List.of(customerEmail), customerSubject, customerHtml);
                }
                customerSent = true;
                log.info("Correo de confirmaci\u00f3n del pedido #{} enviado al cliente {} (boletaAdjunta={}).",
                        order.getId(), customerEmail, boletaPdfBytes != null);
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
                String adminSubject = "Nuevo Pedido #" + order.getId() + " Recibido (" + totalText + ") - Administraci\u00f3n Eco Pizza";
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

        return new OrderEmailDispatchResultDTO(customerSent, customerEmail, adminSent, adminCount);
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
                ? "URGENTE: Stock Agotado detectado en Inventario (" + alerts.size() + " producto(s) en alerta) - Eco Pizza"
                : "Alerta Consolidada de Falta de Stock (" + alerts.size() + " producto(s) bajo m\u00ednimo) - Eco Pizza";

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
        helper.setFrom(new InternetAddress(from, "Eco Pizza Notificaciones", StandardCharsets.UTF_8.name()));
        helper.setTo(toAddresses.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    protected void sendHtmlMessageWithAttachment(List<String> toAddresses,
                                                 String subject,
                                                 String htmlBody,
                                                 String attachmentFilename,
                                                 byte[] attachmentBytes,
                                                 String contentType) throws Exception {
        String from = resolveFromAddress();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(new InternetAddress(from, "Eco Pizza Facturaci\u00f3n Electr\u00f3nica", StandardCharsets.UTF_8.name()));
        helper.setTo(toAddresses.toArray(new String[0]));
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        if (attachmentBytes != null && attachmentBytes.length > 0 && attachmentFilename != null) {
            helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentBytes),
                    contentType != null ? contentType : "application/pdf");
        }
        mailSender.send(message);
    }

    public boolean sendBoletaEmailForOrder(Integer orderId) {
        if (orderId == null || !isMailOperational()) {
            return false;
        }
        OrderCompleteDTO order = loadCompleteOrder(orderId);
        if (order == null || !isDeliverableEmail(order.getUserEmail())) {
            return false;
        }
        try {
            boolean esFactura = isFacturaOrder(order);
            String serie = order.getBoletaSerie() != null ? order.getBoletaSerie() : (esFactura ? "FFF1" : "BBB1");
            int num = order.getBoletaNumero() != null ? order.getBoletaNumero() : orderId;
            String comprobante = order.getBoletaComprobante() != null ? order.getBoletaComprobante() : String.format("%s-%06d", serie, num);
            String tipoLabel = esFactura ? "Factura Electr\u00f3nica " : "Boleta de Venta Electr\u00f3nica ";
            String subject = tipoLabel + comprobante + " (Pedido #" + order.getId() + ") - Eco Pizza";
            String html = buildCustomerOrderHtml(order);
            byte[] pdfBytes = sunatService != null ? sunatService.obtenerPdfBoleta(orderId) : null;
            String filename = String.format("%s_%s-%06d.pdf", esFactura ? "Factura" : "Boleta", serie, num);

            if (pdfBytes != null && pdfBytes.length > 0) {
                sendHtmlMessageWithAttachment(List.of(order.getUserEmail().trim()), subject, html, filename, pdfBytes, "application/pdf");
            } else {
                sendHtmlMessage(List.of(order.getUserEmail().trim()), subject, html);
            }
            return true;
        } catch (Exception ex) {
            log.error("Error al enviar correo con Boleta Electr\u00f3nica para pedido #{}: {}", orderId, ex.getMessage());
            return false;
        }
    }

    public boolean sendNotaCreditoEmailForOrder(Integer orderId) {
        if (orderId == null || !isMailOperational()) {
            return false;
        }
        OrderCompleteDTO order = loadCompleteOrder(orderId);
        if (order == null || !isDeliverableEmail(order.getUserEmail()) || order.getNcNumero() == null) {
            return false;
        }
        try {
            String ncSerie = order.getNcSerie() != null ? order.getNcSerie() : "FFF1";
            int ncNum = order.getNcNumero();
            String ncComprobante = order.getNcComprobante() != null
                    ? order.getNcComprobante()
                    : String.format("%s-%06d", ncSerie, ncNum);
            boolean esFactura = isFacturaOrder(order);
            String docShortLabel = esFactura ? "Factura " : "Boleta ";
            String boletaRef = order.getBoletaComprobante() != null
                    ? order.getBoletaComprobante()
                    : ((esFactura ? "FFF1-" : "BBB1-") + orderId);
            String subject = "Nota de Cr\u00e9dito Electr\u00f3nica " + ncComprobante
                    + " (Anulaci\u00f3n de " + docShortLabel + boletaRef + ") - Eco Pizza";
            String html = buildNotaCreditoEmailHtml(order, ncComprobante, boletaRef);
            byte[] pdfBytes = sunatService != null ? sunatService.obtenerPdfNotaCredito(orderId) : null;
            String filename = String.format("NotaCredito_%s-%06d.pdf", ncSerie, ncNum);

            if (pdfBytes != null && pdfBytes.length > 0) {
                sendHtmlMessageWithAttachment(List.of(order.getUserEmail().trim()), subject, html, filename, pdfBytes, "application/pdf");
            } else {
                sendHtmlMessage(List.of(order.getUserEmail().trim()), subject, html);
            }
            return true;
        } catch (Exception ex) {
            log.error("Error al enviar correo con Nota de Cr\u00e9dito Electr\u00f3nica para pedido #{}: {}", orderId, ex.getMessage());
            return false;
        }
    }

    private String buildNotaCreditoEmailHtml(OrderCompleteDTO order, String ncComprobante, String boletaRef) {
        boolean esFactura = isFacturaOrder(order);
        String docLong = esFactura ? "Factura Electr\u00f3nica" : "Boleta de Venta Electr\u00f3nica";
        String docShort = esFactura ? "Factura" : "Boleta";
        String idDocLabel = esFactura ? "RUC Cliente" : "DNI Cliente";
        String customerName = order.getUserName() != null && !order.getUserName().isBlank()
                ? escapeHtml(order.getUserName()) : "Cliente";
        String dni = order.getBoletaClienteDni() != null ? escapeHtml(order.getBoletaClienteDni()) : "--------";
        String motivo = order.getNcMotivo() != null && !order.getNcMotivo().isBlank()
                ? escapeHtml(order.getNcMotivo()) : "Anulaci\u00f3n de la operaci\u00f3n";
        String fecha = (order.getNcEmitidaAt() != null ? order.getNcEmitidaAt() : LocalDateTime.now()).format(DATE_TIME_FMT);

        List<String[]> calloutRows = new ArrayList<>();
        calloutRows.add(new String[]{"Documento modificado", escapeHtml(docShort + " " + boletaRef)});
        calloutRows.add(new String[]{"Tipo de nota", "01 - Anulaci\u00f3n de la operaci\u00f3n"});
        calloutRows.add(new String[]{"Motivo", motivo});
        calloutRows.add(new String[]{idDocLabel, dni});
        calloutRows.add(new String[]{"Importe total anulado", escapeHtml(formatMoney(order.getTotal()))});

        StringBuilder inner = new StringBuilder();
        inner.append(buildKeyValueRowsHtml(calloutRows));
        if (order.getNcPdfUrl() != null && order.getNcPdfUrl().startsWith("http")) {
            inner.append(buildActionButtonHtml(order.getNcPdfUrl(), "Descargar Nota de Cr\u00e9dito (PDF)"));
        }

        StringBuilder body = new StringBuilder();
        body.append("<p style=\"margin:0 0 14px;color:").append(COLOR_TEXT_PRIMARY).append(";\">Estimado(a) <strong>")
                .append(customerName).append("</strong>,</p>");
        body.append("<p style=\"margin:0 0 18px;color:").append(COLOR_TEXT_SECONDARY)
                .append(";\">Le informamos que se ha emitido la <strong>Nota de Cr\u00e9dito Electr\u00f3nica ")
                .append(escapeHtml(ncComprobante)).append("</strong>, anulando su ").append(docLong)
                .append(" <strong>").append(escapeHtml(boletaRef)).append("</strong> ante SUNAT.</p>");
        body.append(buildCalloutBoxHtml("Resumen de anulaci\u00f3n SUNAT", inner.toString()));
        body.append("<p style=\"margin:18px 0 0;font-size:12px;color:").append(COLOR_TEXT_MUTED)
                .append(";\">Se adjunta el archivo PDF oficial de su Nota de Cr\u00e9dito Electr\u00f3nica a este correo.</p>");

        return wrapEmailDocument(
                "Nota de Cr\u00e9dito Electr\u00f3nica " + ncComprobante + " emitida para el pedido #" + order.getId(),
                "Nota de Cr\u00e9dito Electr\u00f3nica " + escapeHtml(ncComprobante),
                "Emitida el " + escapeHtml(fecha) + " &middot; Pedido #" + order.getId(),
                body.toString(),
                "Facturaci\u00f3n Electr\u00f3nica Eco Pizza &middot; Documento tributario emitido ante SUNAT",
                620
        );
    }

    private String wrapEmailDocument(String preheader,
                                       String title,
                                       String subtitle,
                                       String bodyHtml,
                                       String footerNote,
                                       int maxWidthPx) {
        String safePreheader = preheader != null ? escapeHtml(preheader) : "";
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"es\"><head>");
        sb.append("<meta charset=\"UTF-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        sb.append("<meta name=\"color-scheme\" content=\"light\">");
        sb.append("<link href=\"https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&amp;display=swap\" rel=\"stylesheet\">");
        sb.append("</head>");
        sb.append("<body style=\"margin:0;padding:0;background-color:").append(COLOR_BG_OUTER)
                .append(";font-family:").append(MAIL_FONT)
                .append(";color:").append(COLOR_TEXT_PRIMARY)
                .append(";-webkit-font-smoothing:antialiased;\">");
        sb.append("<span style=\"display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all;\">")
                .append(safePreheader).append("</span>");
        sb.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"background-color:")
                .append(COLOR_BG_OUTER).append(";padding:24px 12px;\"><tr><td align=\"center\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"max-width:")
                .append(maxWidthPx).append("px;background-color:").append(COLOR_BG_CARD)
                .append(";border:1px solid ").append(COLOR_BORDER_SUBTLE)
                .append(";border-top:3px solid ").append(COLOR_BORDER_STRONG)
                .append(";border-radius:8px;overflow:hidden;border-collapse:collapse;\">");
        sb.append("<tr><td style=\"padding:24px 28px 18px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE).append(";\">");
        sb.append("<div style=\"font-size:12px;font-weight:700;letter-spacing:0.08em;color:")
                .append(COLOR_TEXT_MUTED).append(";text-transform:uppercase;\">ECO PIZZA</div>");
        sb.append("<h1 style=\"margin:8px 0 4px;font-size:20px;font-weight:700;color:")
                .append(COLOR_TEXT_PRIMARY).append(";line-height:1.3;font-variant-numeric:tabular-nums;\">")
                .append(title).append("</h1>");
        if (subtitle != null && !subtitle.isBlank()) {
            sb.append("<p style=\"margin:0;font-size:13px;color:").append(COLOR_TEXT_MUTED)
                    .append(";line-height:1.4;font-variant-numeric:tabular-nums;\">").append(subtitle).append("</p>");
        }
        sb.append("</td></tr>");
        sb.append("<tr><td style=\"padding:24px 28px;color:").append(COLOR_TEXT_PRIMARY)
                .append(";font-size:14px;line-height:1.6;\">").append(bodyHtml).append("</td></tr>");
        sb.append("<tr><td style=\"background-color:").append(COLOR_BG_SUBTLE)
                .append(";border-top:1px solid ").append(COLOR_BORDER_SUBTLE)
                .append(";padding:14px 28px;font-size:12px;color:").append(COLOR_TEXT_MUTED)
                .append(";line-height:1.5;\">").append(footerNote).append("</td></tr>");
        sb.append("</table></td></tr></table></body></html>");
        return sb.toString();
    }

    private String buildKeyValueBoxHtml(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        return "<div style=\"background-color:" + COLOR_BG_SUBTLE
                + ";border:1px solid " + COLOR_BORDER_SUBTLE
                + ";border-radius:6px;padding:12px 16px;margin-bottom:20px;\">"
                + buildKeyValueRowsHtml(rows)
                + "</div>";
    }

    private String buildKeyValueRowsHtml(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"border-collapse:collapse;font-size:13px;line-height:1.5;\">");
        for (String[] row : rows) {
            if (row == null || row.length < 2) continue;
            sb.append("<tr>");
            sb.append("<td style=\"padding:4px 12px 4px 0;color:").append(COLOR_TEXT_MUTED)
                    .append(";font-weight:600;vertical-align:top;width:38%;white-space:nowrap;\">")
                    .append(escapeHtml(row[0])).append(":</td>");
            sb.append("<td style=\"padding:4px 0;color:").append(COLOR_TEXT_PRIMARY)
                    .append(";vertical-align:top;font-variant-numeric:tabular-nums;\">")
                    .append(row[1]).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String buildCalloutBoxHtml(String heading, String innerHtml) {
        return "<div style=\"margin-top:20px;background-color:" + COLOR_BG_SUBTLE
                + ";border:1px solid " + COLOR_BORDER_DEFAULT
                + ";border-left:3px solid " + COLOR_BORDER_STRONG
                + ";border-radius:6px;padding:16px 18px;font-size:13px;color:" + COLOR_TEXT_PRIMARY + ";\">"
                + "<div style=\"margin:0 0 10px;font-size:14px;font-weight:700;color:" + COLOR_TEXT_PRIMARY + ";\">"
                + heading + "</div>"
                + innerHtml
                + "</div>";
    }

    private String buildActionButtonHtml(String url, String label) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return "<div style=\"margin-top:14px;\"><a href=\"" + escapeHtml(url)
                + "\" target=\"_blank\" rel=\"noopener noreferrer\" style=\"display:inline-block;background-color:"
                + COLOR_BORDER_STRONG + ";color:" + COLOR_BG_CARD
                + ";text-decoration:none;font-weight:600;font-size:13px;padding:10px 18px;border-radius:6px;\">"
                + escapeHtml(label) + "</a></div>";
    }

    private OrderCompleteDTO loadCompleteOrder(Integer orderId) {
        String sql = """
            SELECT o.id AS order_id, o.delivery_type, o.status, o.subtotal, o.deliveryFee, o.discount,
                   o.promo_code, o.total, o.notes, o.estimatedDelivery, o.createdAt AS order_date,
                   o.wants_boleta, o.boleta_tipo_comprobante, o.boleta_serie, o.boleta_numero,
                   o.boleta_cliente_dni, o.boleta_cliente_nombre, o.boleta_cliente_direccion,
                   o.boleta_pdf_url, o.boleta_xml_url, o.boleta_cdr_url, o.boleta_enlace,
                   o.boleta_hash, o.boleta_qr, o.boleta_estado, o.boleta_total_gravada,
                   o.boleta_total_igv, o.boleta_emitida_at,
                   o.nc_serie, o.nc_numero, o.nc_tipo_nota, o.nc_motivo, o.nc_pdf_url, o.nc_hash, o.nc_estado, o.nc_emitida_at,
                   u.id AS user_id, u.name AS customer_name, u.email AS customer_email, u.phone AS customer_phone, u.dni AS customer_dni,
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
            dto.setUserDni(rs.getString("customer_dni"));
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

            dto.setWantsBoleta(rs.getObject("wants_boleta") != null && rs.getBoolean("wants_boleta"));
            dto.setBoletaTipoComprobante(rs.getObject("boleta_tipo_comprobante") != null ? rs.getInt("boleta_tipo_comprobante") : null);
            String serie = rs.getString("boleta_serie");
            Integer numero = rs.getObject("boleta_numero") != null ? rs.getInt("boleta_numero") : null;
            dto.setBoletaSerie(serie);
            dto.setBoletaNumero(numero);
            if (serie != null && numero != null) {
                dto.setBoletaComprobante(serie + "-" + String.format("%06d", numero));
            }
            dto.setBoletaClienteDni(rs.getString("boleta_cliente_dni"));
            dto.setBoletaClienteNombre(rs.getString("boleta_cliente_nombre"));
            dto.setBoletaClienteDireccion(rs.getString("boleta_cliente_direccion"));
            dto.setBoletaPdfUrl(rs.getString("boleta_pdf_url"));
            dto.setBoletaXmlUrl(rs.getString("boleta_xml_url"));
            dto.setBoletaCdrUrl(rs.getString("boleta_cdr_url"));
            dto.setBoletaEnlace(rs.getString("boleta_enlace"));
            dto.setBoletaHash(rs.getString("boleta_hash"));
            dto.setBoletaQr(rs.getString("boleta_qr"));
            dto.setBoletaEstado(rs.getString("boleta_estado"));
            dto.setBoletaTotalGravada(rs.getBigDecimal("boleta_total_gravada"));
            dto.setBoletaTotalIgv(rs.getBigDecimal("boleta_total_igv"));
            Timestamp emitida = rs.getTimestamp("boleta_emitida_at");
            dto.setBoletaEmitidaAt(emitida != null ? emitida.toLocalDateTime() : null);

            String ncSerie = rs.getString("nc_serie");
            Integer ncNum = rs.getObject("nc_numero") != null ? rs.getInt("nc_numero") : null;
            dto.setNcSerie(ncSerie);
            dto.setNcNumero(ncNum);
            if (ncSerie != null && ncNum != null) {
                dto.setNcComprobante(ncSerie + "-" + String.format("%06d", ncNum));
            }
            dto.setNcTipoNota(rs.getObject("nc_tipo_nota") != null ? rs.getInt("nc_tipo_nota") : null);
            dto.setNcMotivo(rs.getString("nc_motivo"));
            dto.setNcPdfUrl(rs.getString("nc_pdf_url"));
            dto.setNcHash(rs.getString("nc_hash"));
            dto.setNcEstado(rs.getString("nc_estado"));
            Timestamp ncEmitida = rs.getTimestamp("nc_emitida_at");
            dto.setNcEmitidaAt(ncEmitida != null ? ncEmitida.toLocalDateTime() : null);
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
                ? escapeHtml(order.getEstimatedDelivery().format(DATE_TIME_FMT)) : "30 - 45 min aprox.";
        String createdDate = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_TIME_FMT) : LocalDateTime.now().format(DATE_TIME_FMT);

        List<String[]> metaRows = new ArrayList<>();
        metaRows.add(new String[]{"Modalidad", deliveryLabel});
        metaRows.add(new String[]{"Direcci\u00f3n / Entrega", addressHtml});
        metaRows.add(new String[]{"M\u00e9todo de pago", paymentMethod});
        metaRows.add(new String[]{"Entrega estimada", estimated});
        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            metaRows.add(new String[]{"Notas", escapeHtml(order.getNotes())});
        }

        StringBuilder body = new StringBuilder();
        body.append("<p style=\"margin:0 0 16px;color:").append(COLOR_TEXT_PRIMARY).append(";\">Estimado(a) <strong>")
                .append(customerName).append("</strong>, confirmamos la recepci\u00f3n de su pedido.</p>");
        body.append(buildKeyValueBoxHtml(metaRows));
        body.append("<h2 style=\"margin:0 0 10px;font-size:15px;font-weight:700;color:")
                .append(COLOR_TEXT_PRIMARY).append(";\">Detalle de su pedido</h2>");
        body.append(buildOrderItemsTableHtml(order.getItems()));
        body.append(buildOrderTotalsHtml(order));
        body.append(buildBoletaSummaryHtml(order));

        return wrapEmailDocument(
                "Pedido #" + order.getId() + " confirmado en Eco Pizza",
                "Confirmaci\u00f3n de pedido #" + order.getId(),
                "Registrado el " + escapeHtml(createdDate),
                body.toString(),
                "Para consultas sobre el pedido #" + order.getId() + ", responda a este correo indicando su n\u00famero de orden. &middot; Eco Pizza",
                620
        );
    }

    private String buildAdminOrderHtml(OrderCompleteDTO order) {
        String customerName = order.getUserName() != null && !order.getUserName().isBlank()
                ? escapeHtml(order.getUserName()) : "Usuario #" + order.getUserId();
        String customerEmail = order.getUserEmail() != null ? escapeHtml(order.getUserEmail()) : "No registrado";
        String customerPhone = order.getUserPhone() != null && !order.getUserPhone().isBlank()
                ? escapeHtml(order.getUserPhone()) : "No registrado";
        String deliveryLabel = "PICKUP".equalsIgnoreCase(order.getDeliveryType())
                ? "Recojo en tienda" : "Delivery a domicilio";
        String addressHtml = buildAddressSummaryHtml(order);
        String paymentMethod = order.getPaymentMethodName() != null && !order.getPaymentMethodName().isBlank()
                ? escapeHtml(order.getPaymentMethodName()) : "ID #" + order.getPaymentMethodId();
        String createdDate = order.getCreatedAt() != null
                ? order.getCreatedAt().format(DATE_TIME_FMT) : LocalDateTime.now().format(DATE_TIME_FMT);
        String totalText = formatMoney(order.getTotal());

        List<String[]> metaRows = new ArrayList<>();
        metaRows.add(new String[]{"Fecha y hora", escapeHtml(createdDate)});
        metaRows.add(new String[]{"Cliente", customerName + " (ID #" + order.getUserId() + ")"});
        metaRows.add(new String[]{"Correo", customerEmail});
        metaRows.add(new String[]{"Tel\u00e9fono", customerPhone});
        metaRows.add(new String[]{"Modalidad", "<strong style=\"color:" + COLOR_TEXT_PRIMARY + ";\">" + escapeHtml(deliveryLabel) + "</strong>"});
        metaRows.add(new String[]{"Direcci\u00f3n", addressHtml});
        metaRows.add(new String[]{"M\u00e9todo de pago", paymentMethod});
        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            metaRows.add(new String[]{"Notas", escapeHtml(order.getNotes())});
        }

        StringBuilder body = new StringBuilder();
        body.append(buildKeyValueBoxHtml(metaRows));
        body.append("<h2 style=\"margin:0 0 10px;font-size:15px;font-weight:700;color:")
                .append(COLOR_TEXT_PRIMARY).append(";\">Detalle para preparaci\u00f3n</h2>");
        body.append(buildOrderItemsTableHtml(order.getItems()));
        body.append(buildOrderTotalsHtml(order));

        return wrapEmailDocument(
                "Nuevo pedido #" + order.getId() + " (" + totalText + ")",
                "Nuevo pedido #" + order.getId() + " &mdash; " + escapeHtml(totalText),
                "Notificaci\u00f3n operativa &middot; " + escapeHtml(createdDate),
                body.toString(),
                "Administraci\u00f3n Eco Pizza &middot; Registro interno de despacho",
                640
        );
    }

    private String buildOrderItemsTableHtml(List<OrderCompleteDTO.OrderItemCompleteDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p style=\"font-size:14px;color:" + COLOR_TEXT_MUTED + ";\">Pedido registrado sin desglose de \u00edtems.</p>";
        }

        StringBuilder rows = new StringBuilder();
        for (OrderCompleteDTO.OrderItemCompleteDTO item : items) {
            String baseName = "EXTRA".equalsIgnoreCase(item.getItemType())
                    ? (item.getExtraName() != null ? item.getExtraName() : "Complemento")
                    : (item.getPizzaName() != null ? item.getPizzaName() : "Pizza");
            StringBuilder desc = new StringBuilder("<strong>").append(escapeHtml(baseName)).append("</strong>");
            if (item.getSizeName() != null && !item.getSizeName().isBlank()) {
                desc.append(" <span style=\"color:").append(COLOR_TEXT_MUTED).append(";\">(")
                        .append(escapeHtml(item.getSizeName())).append(")</span>");
            }
            if (item.getExtras() != null && !item.getExtras().isEmpty()) {
                String extrasJoined = item.getExtras().stream()
                        .map(this::escapeHtml)
                        .collect(Collectors.joining(", "));
                desc.append("<br><span style=\"font-size:12px;color:").append(COLOR_TEXT_MUTED)
                        .append(";\">+ Adicionales: ").append(extrasJoined).append("</span>");
            }

            rows.append("<tr>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:13px;color:").append(COLOR_TEXT_PRIMARY).append(";\">").append(desc).append("</td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:13px;color:").append(COLOR_TEXT_PRIMARY)
                    .append(";text-align:center;font-variant-numeric:tabular-nums;\">")
                    .append(item.getQuantity() != null ? item.getQuantity() : 1).append("</td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:13px;color:").append(COLOR_TEXT_SECONDARY)
                    .append(";text-align:right;font-variant-numeric:tabular-nums;white-space:nowrap;\">")
                    .append(escapeHtml(formatMoney(item.getUnitPrice()))).append("</td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:13px;color:").append(COLOR_TEXT_PRIMARY)
                    .append(";text-align:right;font-weight:600;font-variant-numeric:tabular-nums;white-space:nowrap;\">")
                    .append(escapeHtml(formatMoney(item.getLineTotal()))).append("</td>")
                    .append("</tr>");
        }

        return "<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"border-collapse:collapse;margin-bottom:16px;\">"
                + "<thead><tr style=\"background-color:" + COLOR_BG_SUBTLE + ";text-align:left;\">"
                + "<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:" + COLOR_TEXT_SECONDARY + ";border-bottom:2px solid " + COLOR_BORDER_DEFAULT + ";\">Producto</th>"
                + "<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:" + COLOR_TEXT_SECONDARY + ";border-bottom:2px solid " + COLOR_BORDER_DEFAULT + ";text-align:center;\">Cant.</th>"
                + "<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:" + COLOR_TEXT_SECONDARY + ";border-bottom:2px solid " + COLOR_BORDER_DEFAULT + ";text-align:right;\">P. Unit.</th>"
                + "<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:" + COLOR_TEXT_SECONDARY + ";border-bottom:2px solid " + COLOR_BORDER_DEFAULT + ";text-align:right;\">Subtotal</th>"
                + "</tr></thead>"
                + "<tbody>" + rows + "</tbody>"
                + "</table>";
    }

    private String buildOrderTotalsHtml(OrderCompleteDTO order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"width:100%;max-width:300px;margin-left:auto;border-collapse:collapse;font-size:13px;\">");
        sb.append("<tr>")
                .append("<td style=\"padding:4px 0;color:").append(COLOR_TEXT_SECONDARY).append(";\">Subtotal:</td>")
                .append("<td style=\"padding:4px 0;text-align:right;color:").append(COLOR_TEXT_PRIMARY)
                .append(";font-variant-numeric:tabular-nums;\">").append(escapeHtml(formatMoney(order.getSubtotal()))).append("</td>")
                .append("</tr>");

        if (order.getDiscount() != null && order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            String promoLabel = order.getPromoCode() != null && !order.getPromoCode().isBlank()
                    ? " (" + escapeHtml(order.getPromoCode()) + ")" : "";
            sb.append("<tr>")
                    .append("<td style=\"padding:4px 0;color:").append(COLOR_TEXT_SECONDARY).append(";\">Descuento").append(promoLabel).append(":</td>")
                    .append("<td style=\"padding:4px 0;text-align:right;color:").append(COLOR_TEXT_PRIMARY)
                    .append(";font-weight:600;font-variant-numeric:tabular-nums;\">-").append(escapeHtml(formatMoney(order.getDiscount()))).append("</td>")
                    .append("</tr>");
        }

        sb.append("<tr>")
                .append("<td style=\"padding:4px 0;color:").append(COLOR_TEXT_SECONDARY).append(";\">Costo de env\u00edo:</td>")
                .append("<td style=\"padding:4px 0;text-align:right;color:").append(COLOR_TEXT_PRIMARY)
                .append(";font-variant-numeric:tabular-nums;\">").append(escapeHtml(formatMoney(order.getDeliveryFee()))).append("</td>")
                .append("</tr>");

        sb.append("<tr>")
                .append("<td style=\"padding:10px 0 4px;border-top:2px solid ").append(COLOR_BORDER_STRONG)
                .append(";font-size:16px;font-weight:700;color:").append(COLOR_TEXT_PRIMARY).append(";\">Total:</td>")
                .append("<td style=\"padding:10px 0 4px;border-top:2px solid ").append(COLOR_BORDER_STRONG)
                .append(";font-size:16px;font-weight:700;text-align:right;color:").append(COLOR_TEXT_PRIMARY)
                .append(";font-variant-numeric:tabular-nums;\">").append(escapeHtml(formatMoney(order.getTotal()))).append("</td>")
                .append("</tr>");
        sb.append("</table>");
        return sb.toString();
    }

    private String buildStockAlertHtml(List<StockAlertDTO> alerts, boolean hasOutOfStock) {
        String title = hasOutOfStock
                ? "Alerta de inventario: stock agotado (" + alerts.size() + " \u00edtem(s))"
                : "Alerta de inventario: stock bajo m\u00ednimo (" + alerts.size() + " \u00edtem(s))";
        String nowFormatted = LocalDateTime.now().format(DATE_TIME_FMT);

        StringBuilder rows = new StringBuilder();
        for (StockAlertDTO a : alerts) {
            boolean isOut = "OUT_OF_STOCK".equalsIgnoreCase(a.getAlertLevel());
            String badgeStyle = isOut
                    ? "display:inline-block;padding:3px 8px;border-radius:4px;font-size:11px;font-weight:600;background-color:"
                            + COLOR_BORDER_STRONG + ";color:" + COLOR_BG_CARD + ";border:1px solid " + COLOR_BORDER_STRONG + ";white-space:nowrap;"
                    : "display:inline-block;padding:3px 8px;border-radius:4px;font-size:11px;font-weight:600;background-color:"
                            + COLOR_BG_OUTER + ";color:" + COLOR_TEXT_PRIMARY + ";border:1px solid " + COLOR_BORDER_DEFAULT + ";white-space:nowrap;";
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

            rows.append("<tr>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE).append(";font-size:13px;\">")
                    .append("<strong>").append(escapeHtml(a.getItemName())).append("</strong><br>")
                    .append("<span style=\"color:").append(COLOR_TEXT_MUTED).append(";font-size:12px;\">")
                    .append(typeLabel).append(" (ID #").append(a.getItemId() != null ? a.getItemId() : 0).append(")</span></td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:12px;color:").append(COLOR_TEXT_SECONDARY).append(";\">").append(flow).append("</td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:13px;text-align:right;font-variant-numeric:tabular-nums;\">")
                    .append(escapeHtml(formatQuantity(a.getPreviousStock(), unit))).append(" ").append(unit).append("</td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:13px;text-align:right;font-weight:700;color:").append(COLOR_TEXT_PRIMARY)
                    .append(";font-variant-numeric:tabular-nums;\">")
                    .append(escapeHtml(formatQuantity(a.getNewStock(), unit))).append(" ").append(unit).append("</td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:13px;text-align:right;font-variant-numeric:tabular-nums;\">")
                    .append(escapeHtml(formatQuantity(a.getMinStock(), unit))).append(" ").append(unit).append("</td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";text-align:center;\"><span style=\"").append(badgeStyle).append("\">").append(badgeText).append("</span></td>")
                    .append("<td style=\"padding:10px 8px;border-bottom:1px solid ").append(COLOR_BORDER_SUBTLE)
                    .append(";font-size:12px;color:").append(COLOR_TEXT_SECONDARY).append(";\">").append(reason).append("</td>")
                    .append("</tr>");
        }

        StringBuilder body = new StringBuilder();
        body.append("<p style=\"margin:0 0 16px;font-size:14px;color:").append(COLOR_TEXT_SECONDARY).append(";\">")
                .append("Se han detectado productos <strong>por debajo o igual al stock m\u00ednimo permitido</strong>. Gestione su reposici\u00f3n desde el m\u00f3dulo de Inventario / Kardex.</p>");
        body.append("<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"border-collapse:collapse;margin-bottom:8px;\">")
                .append("<thead><tr style=\"background-color:").append(COLOR_BG_SUBTLE).append(";text-align:left;\">")
                .append("<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:").append(COLOR_TEXT_SECONDARY).append(";border-bottom:2px solid ").append(COLOR_BORDER_DEFAULT).append(";\">\u00cdtem</th>")
                .append("<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:").append(COLOR_TEXT_SECONDARY).append(";border-bottom:2px solid ").append(COLOR_BORDER_DEFAULT).append(";\">Movimiento</th>")
                .append("<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:").append(COLOR_TEXT_SECONDARY).append(";border-bottom:2px solid ").append(COLOR_BORDER_DEFAULT).append(";text-align:right;\">Anterior</th>")
                .append("<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:").append(COLOR_TEXT_SECONDARY).append(";border-bottom:2px solid ").append(COLOR_BORDER_DEFAULT).append(";text-align:right;\">Actual</th>")
                .append("<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:").append(COLOR_TEXT_SECONDARY).append(";border-bottom:2px solid ").append(COLOR_BORDER_DEFAULT).append(";text-align:right;\">M\u00ednimo</th>")
                .append("<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:").append(COLOR_TEXT_SECONDARY).append(";border-bottom:2px solid ").append(COLOR_BORDER_DEFAULT).append(";text-align:center;\">Estado</th>")
                .append("<th scope=\"col\" style=\"padding:10px 8px;font-size:12px;font-weight:600;color:").append(COLOR_TEXT_SECONDARY).append(";border-bottom:2px solid ").append(COLOR_BORDER_DEFAULT).append(";\">Detalle</th>")
                .append("</tr></thead>")
                .append("<tbody>").append(rows).append("</tbody>")
                .append("</table>");

        return wrapEmailDocument(
                title + " - Eco Pizza",
                title,
                "Control autom\u00e1tico de inventario &middot; " + escapeHtml(nowFormatted),
                body.toString(),
                "Fecha de env\u00edo: " + escapeHtml(nowFormatted) + " &middot; No se enviar\u00e1n alertas repetidas hasta que el stock supere el m\u00ednimo.",
                740
        );
    }

    private String buildAddressSummaryHtml(OrderCompleteDTO order) {
        if ("PICKUP".equalsIgnoreCase(order.getDeliveryType())) {
            return "Recojo presencial en local Eco Pizza";
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

    private boolean isFacturaOrder(OrderCompleteDTO order) {
        if (order == null) return false;
        if (Integer.valueOf(1).equals(order.getBoletaTipoComprobante())) return true;
        return order.getBoletaSerie() != null && order.getBoletaSerie().trim().toUpperCase().startsWith("F");
    }

    private String buildBoletaSummaryHtml(OrderCompleteDTO order) {
        if (order == null || (!Boolean.TRUE.equals(order.getWantsBoleta()) && order.getBoletaNumero() == null)) {
            return "";
        }
        boolean esFactura = isFacturaOrder(order);
        String tipoTitulo = esFactura ? "Factura Electr\u00f3nica" : "Boleta de Venta Electr\u00f3nica";
        String idLabel = esFactura ? "RUC Cliente" : "DNI Cliente";
        String nameLabel = esFactura ? "Raz\u00f3n Social" : "Denominaci\u00f3n";
        String dni = order.getBoletaClienteDni() != null ? escapeHtml(order.getBoletaClienteDni()) : "--------";
        String denom = order.getBoletaClienteNombre() != null ? escapeHtml(order.getBoletaClienteNombre()) : escapeHtml(order.getUserName());

        if (order.getBoletaNumero() == null && "PENDIENTE_ENTREGA".equalsIgnoreCase(order.getBoletaEstado())) {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"Comprobante", tipoTitulo + " (Pago contra entrega)"});
            rows.add(new String[]{idLabel, dni});
            rows.add(new String[]{nameLabel, denom});
            String inner = buildKeyValueRowsHtml(rows)
                    + "<p style=\"margin:10px 0 0;font-size:12px;color:" + COLOR_TEXT_MUTED
                    + ";\">La " + tipoTitulo + " SUNAT se emitir\u00e1 y enviar\u00e1 en PDF a este correo al confirmar la entrega y el pago en efectivo.</p>";
            return buildCalloutBoxHtml("Comprobante electr\u00f3nico pendiente de emisi\u00f3n", inner);
        }

        if (order.getBoletaNumero() == null && !"EMITIDA".equalsIgnoreCase(order.getBoletaEstado())) {
            return "";
        }

        String comprobante = order.getBoletaComprobante() != null
                ? escapeHtml(order.getBoletaComprobante())
                : ((esFactura ? "FFF1-" : "BBB1-") + order.getId());
        String gravada = formatMoney(order.getBoletaTotalGravada());
        String igv = formatMoney(order.getBoletaTotalIgv());
        String total = formatMoney(order.getTotal());

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{idLabel, dni});
        rows.add(new String[]{nameLabel, denom});
        rows.add(new String[]{"Op. Gravada", escapeHtml(gravada)});
        rows.add(new String[]{"I.G.V. (18%)", escapeHtml(igv)});
        rows.add(new String[]{"Importe Total", "<strong>" + escapeHtml(total) + "</strong>"});
        if (order.getBoletaHash() != null && !order.getBoletaHash().isBlank()) {
            rows.add(new String[]{"Hash CPE", escapeHtml(order.getBoletaHash())});
        }

        StringBuilder inner = new StringBuilder();
        inner.append(buildKeyValueRowsHtml(rows));
        inner.append("<p style=\"margin:10px 0 0;font-size:12px;color:").append(COLOR_TEXT_MUTED)
                .append(";\">Se adjunta el archivo PDF oficial de su ").append(tipoTitulo).append(" a este correo.</p>");
        if (order.getBoletaPdfUrl() != null && order.getBoletaPdfUrl().startsWith("http")) {
            inner.append(buildActionButtonHtml(order.getBoletaPdfUrl(), "Descargar " + tipoTitulo + " (PDF)"));
        }

        return buildCalloutBoxHtml(tipoTitulo + " " + comprobante, inner.toString());
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
            String subject = "C\u00f3digo de recuperaci\u00f3n de contrase\u00f1a - Eco Pizza";
            String html = buildPasswordResetHtml(safeName, safeCode, expirationMinutes);

            sendHtmlMessage(List.of(recipientEmail.trim()), subject, html);
            log.info("Correo con c\u00f3digo de recuperaci\u00f3n enviado exitosamente a {}.", recipientEmail);
            return true;
        } catch (Exception ex) {
            log.error("Error al enviar correo de recuperaci\u00f3n a {}: {}", recipientEmail, ex.getMessage());
            return false;
        }
    }

    private String buildPasswordResetHtml(String safeName, String safeCode, int expirationMinutes) {
        String nowFormatted = LocalDateTime.now().format(DATE_TIME_FMT);
        StringBuilder body = new StringBuilder();
        body.append("<p style=\"margin:0 0 14px;color:").append(COLOR_TEXT_PRIMARY)
                .append(";\">Estimado(a) <strong>").append(safeName).append("</strong>,</p>");
        body.append("<p style=\"margin:0 0 18px;color:").append(COLOR_TEXT_SECONDARY)
                .append(";\">Recibimos una solicitud para restablecer la contrase\u00f1a de su cuenta. Ingrese el siguiente c\u00f3digo de verificaci\u00f3n en la pantalla de recuperaci\u00f3n:</p>");
        body.append("<div style=\"margin:20px 0;padding:20px;background-color:").append(COLOR_BG_SUBTLE)
                .append(";border:1px solid ").append(COLOR_BORDER_DEFAULT)
                .append(";border-radius:6px;text-align:center;\">")
                .append("<div style=\"font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.08em;color:")
                .append(COLOR_TEXT_MUTED).append(";margin-bottom:8px;\">C\u00f3digo de verificaci\u00f3n</div>")
                .append("<div style=\"font-size:30px;font-weight:700;letter-spacing:6px;color:")
                .append(COLOR_TEXT_PRIMARY).append(";font-variant-numeric:tabular-nums;\">").append(safeCode).append("</div>")
                .append("<div style=\"font-size:12px;color:").append(COLOR_TEXT_MUTED)
                .append(";margin-top:8px;\">V\u00e1lido por <strong>").append(expirationMinutes).append(" minutos</strong></div>")
                .append("</div>");
        body.append("<p style=\"margin:0;font-size:13px;color:").append(COLOR_TEXT_MUTED)
                .append(";\">Si no solicit\u00f3 este cambio, puede ignorar este mensaje; su contrase\u00f1a actual no ser\u00e1 modificada.</p>");

        return wrapEmailDocument(
                "C\u00f3digo de verificaci\u00f3n Eco Pizza",
                "C\u00f3digo de verificaci\u00f3n",
                "Restablecimiento de contrase\u00f1a &middot; Eco Pizza",
                body.toString(),
                "Enviado el " + escapeHtml(nowFormatted) + " &middot; Seguridad de cuentas Eco Pizza",
                560
        );
    }

    @Async("mailTaskExecutor")
    public void sendPasswordChangedConfirmationEmailAsync(String recipientEmail, String userName) {
        if (!isDeliverableEmail(recipientEmail) || !isMailOperational()) {
            return;
        }
        try {
            String safeName = (userName != null && !userName.isBlank()) ? escapeHtml(userName.trim()) : "Cliente";
            String subject = "Tu contrase\u00f1a ha sido actualizada - Eco Pizza";
            String html = buildPasswordChangedHtml(safeName);
            sendHtmlMessage(List.of(recipientEmail.trim()), subject, html);
        } catch (Exception ex) {
            log.debug("Aviso: no se pudo enviar confirmaci\u00f3n de cambio de contrase\u00f1a a {}: {}", recipientEmail, ex.getMessage());
        }
    }

    private String buildPasswordChangedHtml(String safeName) {
        String nowFormatted = LocalDateTime.now().format(DATE_TIME_FMT);
        StringBuilder body = new StringBuilder();
        body.append("<p style=\"margin:0 0 14px;color:").append(COLOR_TEXT_PRIMARY)
                .append(";\">Estimado(a) <strong>").append(safeName).append("</strong>,</p>");
        body.append("<p style=\"margin:0 0 14px;color:").append(COLOR_TEXT_SECONDARY)
                .append(";\">Le confirmamos que la contrase\u00f1a de su cuenta en <strong>Eco Pizza</strong> fue actualizada correctamente el <strong style=\"font-variant-numeric:tabular-nums;\">")
                .append(escapeHtml(nowFormatted)).append("</strong>.</p>");
        body.append("<p style=\"margin:0;font-size:13px;color:").append(COLOR_TEXT_MUTED)
                .append(";\">Si no reconoce esta operaci\u00f3n, p\u00f3ngase en contacto con soporte de inmediato.</p>");

        return wrapEmailDocument(
                "Actualizaci\u00f3n de contrase\u00f1a confirmada en Eco Pizza",
                "Contrase\u00f1a actualizada",
                "Seguridad de cuentas &middot; Eco Pizza",
                body.toString(),
                "Seguridad de cuentas Eco Pizza",
                560
        );
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
