package com.example.services;

import com.example.dtos.OrderEmailDispatchResultDTO;
import com.example.dtos.StockAlertDTO;
import com.example.models.Usuario;
import com.example.repositories.UsuarioRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias para EmailService y Ventana de Consolidacion de Stock")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    private EmailService emailService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
        lenient().when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        emailService = new EmailService(mailSenderProvider, usuarioRepository, jdbcTemplate);
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "configuredFrom", "sistema.prueba@gmail.com");
        ReflectionTestUtils.setField(emailService, "mailUsername", "sistema.prueba@gmail.com");
    }

    @Test
    @DisplayName("Enviar correo de pedido al cliente y administradores retornando confirmacion emailSent")
    @SuppressWarnings("unchecked")
    void testSendOrderCreatedEmailsToCustomerAndAdmins() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));

        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setName("Admin Principal");
        admin.setEmail("admin.prueba@gmail.com");
        admin.setRole(Usuario.Role.ADMIN);
        when(usuarioRepository.findByRoleAndDeletedAtIsNull(Usuario.Role.ADMIN)).thenReturn(List.of(admin));

        when(jdbcTemplate.query(contains("FROM orders o"), any(RowMapper.class), eq(105))).thenAnswer(inv -> {
            RowMapper<Object> mapper = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class, withSettings().strictness(Strictness.LENIENT));
            when(rs.getInt("order_id")).thenReturn(105);
            when(rs.getInt("user_id")).thenReturn(7);
            when(rs.getString("customer_name")).thenReturn("Carlos Cliente");
            when(rs.getString("customer_email")).thenReturn("carlos.cliente@gmail.com");
            when(rs.getString("customer_phone")).thenReturn("999888777");
            when(rs.getObject("address_id")).thenReturn(3);
            when(rs.getString("address_line1")).thenReturn("Av. Arequipa 1234");
            when(rs.getString("address_city")).thenReturn("Lima");
            when(rs.getString("address_district")).thenReturn("Miraflores");
            when(rs.getString("address_reference")).thenReturn("Frente al parque");
            when(rs.getString("delivery_type")).thenReturn("DELIVERY");
            when(rs.getString("status")).thenReturn("PENDING");
            when(rs.getObject("payment_method_id")).thenReturn(1);
            when(rs.getString("payment_method_name")).thenReturn("Yape");
            when(rs.getBigDecimal("subtotal")).thenReturn(new BigDecimal("45.00"));
            when(rs.getBigDecimal("deliveryFee")).thenReturn(new BigDecimal("5.00"));
            when(rs.getBigDecimal("discount")).thenReturn(BigDecimal.ZERO);
            when(rs.getBigDecimal("total")).thenReturn(new BigDecimal("50.00"));
            when(rs.getTimestamp("order_date")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
            return List.of(mapper.mapRow(rs, 0));
        });

        when(jdbcTemplate.query(contains("FROM order_items oi"), any(RowMapper.class), eq(105))).thenAnswer(inv -> {
            RowMapper<Object> mapper = inv.getArgument(1);
            ResultSet rs = mock(ResultSet.class, withSettings().strictness(Strictness.LENIENT));
            when(rs.getInt("id")).thenReturn(501);
            when(rs.getString("item_type")).thenReturn("PIZZA");
            when(rs.getObject("pizzaId")).thenReturn(1);
            when(rs.getString("pizza_name")).thenReturn("Pepperoni Especial");
            when(rs.getObject("sizeId")).thenReturn(2);
            when(rs.getString("size_name")).thenReturn("Familiar");
            when(rs.getInt("quantity")).thenReturn(1);
            when(rs.getBigDecimal("unitPrice")).thenReturn(new BigDecimal("45.00"));
            when(rs.getBigDecimal("lineTotal")).thenReturn(new BigDecimal("45.00"));
            return List.of(mapper.mapRow(rs, 0));
        });

        doNothing().when(jdbcTemplate).query(contains("FROM order_item_extras"), any(RowCallbackHandler.class), any(Object[].class));

        OrderEmailDispatchResultDTO result = emailService.sendOrderCreatedEmails(105);

        assertTrue(result.customerEmailSent());
        assertEquals("carlos.cliente@gmail.com", result.customerEmail());
        assertTrue(result.adminEmailSent());
        assertEquals(1, result.adminRecipientsCount());

        ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(msgCaptor.capture());

        List<MimeMessage> sentMessages = msgCaptor.getAllValues();
        assertEquals("carlos.cliente@gmail.com", sentMessages.get(0).getAllRecipients()[0].toString());
        assertTrue(sentMessages.get(0).getSubject().contains("#105"));
        assertTrue(sentMessages.get(0).getSubject().contains("Eco Pizza"));
        assertFalse(sentMessages.get(0).getSubject().toLowerCase().contains("pizza hut"));
        assertTrue(sentMessages.get(0).getFrom()[0].toString().contains("Eco Pizza"));
        assertTrue(sentMessages.get(0).getContent().toString().contains("Eco Pizza"));
        assertFalse(sentMessages.get(0).getContent().toString().toLowerCase().contains("pizza hut"));

        assertEquals("admin.prueba@gmail.com", sentMessages.get(1).getAllRecipients()[0].toString());
        assertTrue(sentMessages.get(1).getSubject().contains("#105"));
        assertTrue(sentMessages.get(1).getSubject().contains("Eco Pizza"));
        assertFalse(sentMessages.get(1).getSubject().toLowerCase().contains("pizza hut"));
        assertTrue(sentMessages.get(1).getFrom()[0].toString().contains("Eco Pizza"));
    }

    @Test
    @DisplayName("Enviar correo de recuperacion y confirmacion de contrasena con marca Eco Pizza")
    void testPasswordResetAndChangedEmailsUseEcoPizzaBranding() throws Exception {
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));

        boolean sent = emailService.sendPasswordResetEmail("cliente@gmail.com", "Ana Perez", "482910", 15);
        assertTrue(sent);
        emailService.sendPasswordChangedConfirmationEmailAsync("cliente@gmail.com", "Ana Perez");

        ArgumentCaptor<MimeMessage> msgCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(2)).send(msgCaptor.capture());

        for (MimeMessage msg : msgCaptor.getAllValues()) {
            assertTrue(msg.getSubject().contains("Eco Pizza"));
            assertFalse(msg.getSubject().toLowerCase().contains("pizza hut"));
            assertTrue(msg.getFrom()[0].toString().contains("Eco Pizza"));
            String body = msg.getContent().toString();
            assertTrue(body.contains("Eco Pizza"));
            assertFalse(body.toLowerCase().contains("pizza hut"));
        }
    }

    @Test
    @DisplayName("Consolidar multiples productos en alerta durante ventana de 60s y enviar 1 solo correo conjunto")
    @SuppressWarnings("unchecked")
    void testOneMinuteBatchConsolidationSendsSingleCombinedEmail() throws Exception {
        Connection conn = mock(Connection.class);
        DatabaseMetaData meta = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(meta);
        when(meta.getDatabaseProductName()).thenReturn("Microsoft SQL Server");
        when(jdbcTemplate.queryForObject(contains("OBJECT_ID"), eq(Integer.class))).thenReturn(1);

        StockAlertNotificationService batchService = new StockAlertNotificationService(jdbcTemplate, dataSource, emailService);
        ReflectionTestUtils.setField(batchService, "batchWindowSeconds", 60);

        // 1. Antes de cumplir 60s (ej. 25s transcurridos): NO debe enviar correo todavia
        when(jdbcTemplate.queryForObject(contains("DATEDIFF"), eq(Integer.class))).thenReturn(25);
        int sentEarly = batchService.processPendingAlerts(false);
        assertEquals(0, sentEarly);
        verify(mailSender, never()).send(any(MimeMessage.class));

        // 2. Cumplidos los 60s: agrupa TODOS los productos pendientes en 1 solo correo
        when(jdbcTemplate.queryForObject(contains("DATEDIFF"), eq(Integer.class))).thenReturn(62);
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));

        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setEmail("admin.inventario@gmail.com");
        admin.setRole(Usuario.Role.ADMIN);
        when(usuarioRepository.findByRoleAndDeletedAtIsNull(Usuario.Role.ADMIN)).thenReturn(List.of(admin));

        when(jdbcTemplate.query(contains("SELECT TOP 200"), any(RowMapper.class))).thenAnswer(inv -> {
            RowMapper<StockAlertDTO> mapper = inv.getArgument(1);

            ResultSet rs1 = mock(ResultSet.class, withSettings().strictness(Strictness.LENIENT));
            when(rs1.getInt("id")).thenReturn(10);
            when(rs1.getString("itemType")).thenReturn("INGREDIENT");
            when(rs1.getInt("itemId")).thenReturn(3);
            when(rs1.getString("itemName")).thenReturn("Queso Mozzarella");
            when(rs1.getString("unit")).thenReturn("g");
            when(rs1.getBigDecimal("newStock")).thenReturn(new BigDecimal("200.00"));
            when(rs1.getBigDecimal("minStock")).thenReturn(new BigDecimal("500.00"));
            when(rs1.getString("alertLevel")).thenReturn("LOW_STOCK");

            ResultSet rs2 = mock(ResultSet.class, withSettings().strictness(Strictness.LENIENT));
            when(rs2.getInt("id")).thenReturn(11);
            when(rs2.getString("itemType")).thenReturn("EXTRA");
            when(rs2.getInt("itemId")).thenReturn(2);
            when(rs2.getString("itemName")).thenReturn("Inca Kola 1.5L");
            when(rs2.getString("unit")).thenReturn("und");
            when(rs2.getBigDecimal("newStock")).thenReturn(BigDecimal.ZERO);
            when(rs2.getBigDecimal("minStock")).thenReturn(new BigDecimal("5.00"));
            when(rs2.getString("alertLevel")).thenReturn("OUT_OF_STOCK");

            return List.of(mapper.mapRow(rs1, 0), mapper.mapRow(rs2, 1));
        });

        int dispatchedProducts = batchService.processPendingAlerts(false);

        assertEquals(2, dispatchedProducts);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(jdbcTemplate, times(1)).update(contains("SET emailSent = 1"), any(Object[].class));
    }
}
