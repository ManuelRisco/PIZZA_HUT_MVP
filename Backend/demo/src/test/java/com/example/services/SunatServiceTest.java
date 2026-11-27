package com.example.services;

import com.example.dtos.BoletaEmisionDTO;
import com.example.dtos.DniLookupDTO;
import com.example.dtos.NotaCreditoEmisionDTO;
import com.example.dtos.OrderCompleteDTO;
import com.example.dtos.RucLookupDTO;
import com.example.exceptions.BadRequestException;
import com.example.models.Address;
import com.example.models.Order;
import com.example.models.Usuario;
import com.example.repositories.OrderRepository;
import com.example.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SunatService - Pruebas unitarias de emisión de boleta y PDF SUNAT")
class SunatServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AddressService addressService;

    @Mock
    private ReniecService reniecService;

    private SunatService sunatService;

    @BeforeEach
    void setUp() {
        sunatService = new SunatService(
                jdbcTemplate,
                orderRepository,
                usuarioRepository,
                addressService,
                reniecService);
    }

    @Test
    @DisplayName("Debe devolver la boleta existente si ya fue emitida previamente")
    void emitirBoletaYaEmitida() {
        Order order = new Order();
        order.setId(42);
        order.setBoletaEstado("EMITIDA");
        order.setBoletaSerie("BBB1");
        order.setBoletaNumero(15);
        order.setBoletaPdfUrl("https://www.nubefact.com/see_invoice/demo.pdf");
        order.setBoletaClienteDni("76949746");
        order.setBoletaClienteNombre("MANUEL RISCO");
        order.setBoletaTotalGravada(new BigDecimal("46.61"));
        order.setBoletaTotalIgv(new BigDecimal("8.39"));
        order.setTotal(new BigDecimal("55.00"));
        order.setBoletaEmitidaAt(LocalDateTime.now());

        when(orderRepository.findById(42)).thenReturn(Optional.of(order));

        BoletaEmisionDTO res = sunatService.emitirBoletaParaPedido(42, null, null);

        assertEquals("BBB1-000015", res.comprobante());
        assertEquals("EMITIDA", res.estado());
        verify(orderRepository, never()).saveAndFlush(any());
        verifyNoInteractions(reniecService);
    }

    @Test
    @DisplayName("Debe emitir boleta resolviendo titular desde ReniecService y calculando IGV 18%")
    @SuppressWarnings("unchecked")
    void emitirBoletaConReniecYDelivery() {
        Order order = new Order();
        order.setId(100);
        order.setUserId(7);
        order.setAddressId(20);
        order.setSubtotal(new BigDecimal("50.00"));
        order.setDeliveryFee(new BigDecimal("5.00"));
        order.setTotal(new BigDecimal("55.00"));

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setDni("76949746");
        usuario.setName("Usuario Demo");

        Address address = new Address();
        address.setId(20);
        address.setLine1("Av. Pardo 123");
        address.setDistrict("Miraflores");
        address.setCity("Lima");

        OrderCompleteDTO.OrderItemCompleteDTO pizzaItem = new OrderCompleteDTO.OrderItemCompleteDTO();
        pizzaItem.setId(1);
        pizzaItem.setItemType("PIZZA");
        pizzaItem.setPizzaId(10);
        pizzaItem.setPizzaName("Americana");
        pizzaItem.setSizeName("Grande");
        pizzaItem.setQuantity(2);
        pizzaItem.setUnitPrice(new BigDecimal("25.00"));
        pizzaItem.setLineTotal(new BigDecimal("50.00"));

        when(orderRepository.findById(100)).thenReturn(Optional.of(order));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(reniecService.consultarDni("76949746")).thenReturn(
                new DniLookupDTO("76949746", "MANUEL", "RISCO", "GIL", "MANUEL RISCO GIL",
                        "CACHE_RENIEC"));
        when(addressService.obtenerPorId(20)).thenReturn(Optional.of(address));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(100))).thenReturn(List.of(pizzaItem));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(10);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaEmisionDTO res = sunatService.emitirBoletaParaPedido(100, null, null);

        assertEquals("BBB1-000011", res.comprobante());
        assertEquals("76949746", res.clienteDni());
        assertEquals("MANUEL RISCO GIL", res.clienteNombre());
        assertEquals("AV. PARDO 123, MIRAFLORES, LIMA", res.clienteDireccion());
        assertEquals(new BigDecimal("55.00"), res.totalGravada().add(res.totalIgv()));
        assertEquals("EMITIDA", res.estado());
        verify(reniecService).consultarDni("76949746");
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    @DisplayName("Debe generar PDF de respaldo con NIU para productos y ZZ para delivery")
    @SuppressWarnings("unchecked")
    void generarPdfFallbackConUnidadZZParaDelivery() {
        Order order = new Order();
        order.setId(101);
        order.setBoletaEstado("EMITIDA");
        order.setBoletaSerie("BBB1");
        order.setBoletaNumero(12);
        order.setBoletaPdfUrl("/api/sunat/boleta/order/101/pdf");
        order.setBoletaClienteDni("76949746");
        order.setBoletaClienteNombre("MANUEL RISCO GIL");
        order.setBoletaClienteDireccion("LIMA");
        order.setBoletaTotalGravada(new BigDecimal("46.61"));
        order.setBoletaTotalIgv(new BigDecimal("8.39"));
        order.setDeliveryFee(new BigDecimal("5.00"));
        order.setTotal(new BigDecimal("55.00"));
        order.setBoletaEmitidaAt(LocalDateTime.of(2026, 11, 25, 22, 0));

        OrderCompleteDTO.OrderItemCompleteDTO pizzaItem = new OrderCompleteDTO.OrderItemCompleteDTO();
        pizzaItem.setId(2);
        pizzaItem.setItemType("PIZZA");
        pizzaItem.setPizzaName("Pepperoni");
        pizzaItem.setSizeName("Mediana");
        pizzaItem.setQuantity(1);
        pizzaItem.setLineTotal(new BigDecimal("50.00"));

        when(orderRepository.findById(101)).thenReturn(Optional.of(order));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(101))).thenReturn(List.of(pizzaItem));

        byte[] pdf = sunatService.obtenerPdfBoleta(101);
        String pdfText = new String(pdf, StandardCharsets.ISO_8859_1);

        assertTrue(pdfText.startsWith("%PDF-1.4"));
        assertTrue(pdfText.contains("1 NIU"), "Debe usar NIU para productos");
        assertTrue(pdfText.contains("1 ZZ"), "Debe usar ZZ para servicio de delivery");
        assertTrue(pdfText.contains("SERVICIO DE ENTREGA A DOMICILIO"));
    }

    @Test
    @DisplayName("Debe fallar si el pedido no tiene DNI válido al emitir boleta")
    void emitirBoletaSinDniValido() {
        Order order = new Order();
        order.setId(102);
        order.setUserId(99);
        when(orderRepository.findById(102)).thenReturn(Optional.of(order));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> sunatService.emitirBoletaParaPedido(102, "123", null));
    }

    @Test
    @DisplayName("Debe emitir Nota de Crédito Electrónica FFF1 anulando una Boleta emitida y generar su PDF")
    @SuppressWarnings("unchecked")
    void emitirNotaCreditoFFF1HaciaBoletaEmitida() {
        Order order = new Order();
        order.setId(205);
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setBoletaSerie("BBB1");
        order.setBoletaNumero(15);
        order.setBoletaEstado("EMITIDA");
        order.setBoletaClienteDni("76949746");
        order.setBoletaClienteNombre("MANUEL RISCO GIL");
        order.setBoletaClienteDireccion("AV. PARDO 123, MIRAFLORES, LIMA");
        order.setBoletaTotalGravada(new BigDecimal("46.61"));
        order.setBoletaTotalIgv(new BigDecimal("8.39"));
        order.setDeliveryFee(new BigDecimal("5.00"));
        order.setTotal(new BigDecimal("55.00"));

        OrderCompleteDTO.OrderItemCompleteDTO pizzaItem = new OrderCompleteDTO.OrderItemCompleteDTO();
        pizzaItem.setId(9);
        pizzaItem.setItemType("PIZZA");
        pizzaItem.setPizzaId(1);
        pizzaItem.setPizzaName("Pepperoni");
        pizzaItem.setSizeName("Familiar");
        pizzaItem.setQuantity(1);
        pizzaItem.setLineTotal(new BigDecimal("50.00"));

        when(orderRepository.findById(205)).thenReturn(Optional.of(order));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(205))).thenReturn(List.of(pizzaItem));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        NotaCreditoEmisionDTO nc = sunatService.emitirNotaCreditoParaPedido(205, "Cliente canceló el pedido");

        assertEquals("FFF1", nc.serie());
        assertEquals("FFF1-000001", nc.comprobante());
        assertEquals("BBB1-000015", nc.boletaModificadaComprobante());
        assertEquals(1, nc.tipoNota());
        assertEquals("EMITIDA", nc.estado());
        assertEquals("ANULADA_NC", order.getBoletaEstado());
        assertEquals("FFF1", order.getNcSerie());
        assertEquals(1, order.getNcNumero());

        byte[] pdf = sunatService.obtenerPdfNotaCredito(205);
        String pdfText = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(pdfText.startsWith("%PDF-1.4"));
        assertTrue(pdfText.contains("NOTA DE CREDITO ELECTRONICA: FFF1-000001"));
        assertTrue(pdfText.contains("BOLETA DE VENTA ELECTRONICA BBB1-000015"));
    }

    @Test
    @DisplayName("Debe rechazar Nota de Crédito si el pedido no tiene Boleta emitida")
    void rechazarNotaCreditoSinBoletaEmitida() {
        Order order = new Order();
        order.setId(206);
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setBoletaEstado("ANULADA_SIN_EMISION");

        when(orderRepository.findById(206)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> sunatService.emitirNotaCreditoParaPedido(206, "Cancelado"));
    }

    @Test
    @DisplayName("Debe emitir Factura Electrónica FFF1 con RUC, guardar RUC en usuario y generar PDF de Factura")
    @SuppressWarnings("unchecked")
    void emitirFacturaFFF1ConRucYGuardarEnUsuario() {
        Order order = new Order();
        order.setId(301);
        order.setUserId(15);
        order.setSubtotal(new BigDecimal("50.00"));
        order.setDeliveryFee(new BigDecimal("5.00"));
        order.setTotal(new BigDecimal("55.00"));

        Usuario usuario = new Usuario();
        usuario.setId(15L);
        usuario.setName("Empresa Demo");

        OrderCompleteDTO.OrderItemCompleteDTO pizzaItem = new OrderCompleteDTO.OrderItemCompleteDTO();
        pizzaItem.setId(1);
        pizzaItem.setItemType("PIZZA");
        pizzaItem.setPizzaId(10);
        pizzaItem.setPizzaName("Americana");
        pizzaItem.setSizeName("Grande");
        pizzaItem.setQuantity(1);
        pizzaItem.setLineTotal(new BigDecimal("50.00"));

        when(orderRepository.findById(301)).thenReturn(Optional.of(order));
        when(usuarioRepository.findById(15L)).thenReturn(Optional.of(usuario));
        when(reniecService.consultarRuc("20538225763")).thenReturn(
                new RucLookupDTO("20538225763", "ECOPIZZA S.A.C.", "AV. JAVIER PRADO 4200 - LIMA",
                        "ACTIVO", "HABIDO", "SURCO", "LIMA", "LIMA", "CACHE_SUNAT"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(301))).thenReturn(List.of(pizzaItem));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("FFF1"))).thenReturn(0);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        BoletaEmisionDTO res = sunatService.emitirComprobanteParaPedido(301, 1, "20538225763", null, null, true);

        assertEquals("FFF1", res.serie());
        assertEquals("FFF1-000001", res.comprobante());
        assertEquals("20538225763", res.clienteDni());
        assertEquals("ECOPIZZA S.A.C.", res.clienteNombre());
        assertEquals("AV. JAVIER PRADO 4200 - LIMA", res.clienteDireccion());
        assertEquals(1, order.getBoletaTipoComprobante());
        assertEquals("20538225763", usuario.getRuc());
        assertTrue(Boolean.TRUE.equals(usuario.getRucVerified()));
        assertTrue(Boolean.TRUE.equals(usuario.getAgeVerified()));

        byte[] pdf = sunatService.obtenerPdfBoleta(301);
        String pdfText = new String(pdf, StandardCharsets.ISO_8859_1);
        assertTrue(pdfText.contains("FACTURA ELECTRONICA: FFF1-000001"));
        assertTrue(pdfText.contains("RUC Cliente: 20538225763"));
        assertTrue(pdfText.contains("Razon Social: ECOPIZZA S.A.C."));
    }

    @Test
    @DisplayName("Debe exigir aceptación condicional de titularidad RUC y mayoría de edad para Factura FFF1")
    void rechazarFacturaSinAceptacionCondicionalRuc() {
        Order order = new Order();
        order.setId(302);
        order.setUserId(16);

        Usuario usuario = new Usuario();
        usuario.setId(16L);
        usuario.setRucVerified(false);

        when(orderRepository.findById(302)).thenReturn(Optional.of(order));
        when(usuarioRepository.findById(16L)).thenReturn(Optional.of(usuario));

        assertThrows(BadRequestException.class,
                () -> sunatService.emitirComprobanteParaPedido(302, 1, "20538225763", "EMPRESA SAC", "LIMA", false));
    }
}
