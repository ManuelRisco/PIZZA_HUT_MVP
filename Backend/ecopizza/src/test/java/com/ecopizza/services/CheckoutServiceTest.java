package com.ecopizza.services;

import com.ecopizza.dtos.CheckoutRequestDTO;
import com.ecopizza.dtos.OrderDTO;
import com.ecopizza.models.Address;
import com.ecopizza.models.Order;
import com.ecopizza.models.OrderItem;
import com.ecopizza.models.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CU07: Agregar al Carrito y Checkout - Pruebas TDD")
public class CheckoutServiceTest {

    @Mock
    private OrderService orderService;
    @Mock
    private OrderItemService orderItemService;
    @Mock
    private AddressService addressService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private PromotionService promotionService;
    @Mock
    private OrderItemExtraService orderItemExtraService;
    @Mock
    private IngredientService ingredientService;
    @Mock
    private PizzaService pizzaService;
    @Mock
    private ExtraService extraService;
    @Mock
    private SizeService sizeService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private SunatService sunatService;

    @InjectMocks
    private CheckoutService checkoutService;

    private CheckoutRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new CheckoutRequestDTO();

        // Configurar Order DTO
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setUserId(1);
        orderDTO.setDeliveryType("DELIVERY");
        orderDTO.setSubtotal(new BigDecimal("50.00"));
        orderDTO.setDeliveryFee(new BigDecimal("5.00"));
        request.setOrder(orderDTO);

        // Configurar Address DTO
        CheckoutRequestDTO.AddressDTO addressDTO = new CheckoutRequestDTO.AddressDTO();
        addressDTO.setLine1("123 Main St");
        addressDTO.setCity("Lima");
        addressDTO.setDistrict("Miraflores");
        request.setAddress(addressDTO);

        // Configurar Payment DTO
        CheckoutRequestDTO.PaymentDTO paymentDTO = new CheckoutRequestDTO.PaymentDTO();
        paymentDTO.setPaymentMethodId(1);
        paymentDTO.setAmount(new BigDecimal("55.00"));
        request.setPayment(paymentDTO);

        // Configurar Items
        CheckoutRequestDTO.CheckoutItemDTO itemDTO = new CheckoutRequestDTO.CheckoutItemDTO();
        itemDTO.setItemType("PIZZA");
        itemDTO.setPizzaId(1);
        itemDTO.setSizeId(2);
        itemDTO.setQuantity(2);
        itemDTO.setUnitPrice(new BigDecimal("25.00"));
        itemDTO.setLineTotal(new BigDecimal("50.00"));
        request.setItems(Collections.singletonList(itemDTO));
    }

    // ==========================================
    // CU07: Agregar al Carrito y Checkout
    // ==========================================

    @Test
    @DisplayName("CU07 - Flujo Principal: Procesar checkout atómicamente, descontar stock y congelar costos [RF13, RF14]")
    void testCheckoutSuccess() {
        // Arrange
        Address savedAddress = new Address();
        savedAddress.setId(10);
        when(addressService.crearAddress(any(Address.class))).thenReturn(savedAddress);

        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setTotal(new BigDecimal("55.00"));
        savedOrder.setDeliveryFee(new BigDecimal("5.00"));
        when(orderService.crearOrder(any(Order.class))).thenReturn(savedOrder);

        when(inventoryService.deductCheckoutStockAndCalculateUnitCosts(any(), eq(100), eq(1)))
                .thenReturn(List.of(new BigDecimal("7.23")));

        OrderItem savedItem = new OrderItem();
        savedItem.setId(1000);
        when(orderItemService.crearOrderItem(any(OrderItem.class))).thenReturn(savedItem);

        com.ecopizza.models.Pizza mockPizza = new com.ecopizza.models.Pizza();
        mockPizza.setId(1);
        mockPizza.setPrice(new BigDecimal("20.00"));
        when(pizzaService.obtenerPorId(1)).thenReturn(java.util.Optional.of(mockPizza));

        com.ecopizza.models.Size mockSize = new com.ecopizza.models.Size();
        mockSize.setId(2);
        mockSize.setExtraCost(new BigDecimal("5.00"));
        when(sizeService.obtenerPorId(2)).thenReturn(java.util.Optional.of(mockSize));

        when(orderService.actualizarOrder(eq(100), any(Order.class))).thenReturn(savedOrder);

        // Act
        Order result = checkoutService.checkout(request);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals(new BigDecimal("55.00"), result.getTotal());

        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemService, times(1)).crearOrderItem(itemCaptor.capture());
        assertEquals(new BigDecimal("7.23"), itemCaptor.getValue().getUnitCost());
        assertEquals(new BigDecimal("14.46"), itemCaptor.getValue().getLineCost());

        verify(inventoryService, times(1)).deductCheckoutStockAndCalculateUnitCosts(any(), eq(100), eq(1));
        verify(addressService, times(1)).crearAddress(any(Address.class));
        verify(orderService, times(1)).crearOrder(any(Order.class));
        verify(paymentService, times(1)).crearPayment(any(Payment.class));
    }

    @Test
    @DisplayName("CU07 - A2: Error por cantidad no válida")
    void testCheckoutCantidadInvalida() {
        Address savedAddress = new Address();
        savedAddress.setId(10);
        when(addressService.crearAddress(any(Address.class))).thenReturn(savedAddress);
        // Simulando que la cantidad es <= 0, el servicio ahora valida esto directamente
        request.getItems().get(0).setQuantity(0);

        // Arrange order creation
        Order savedOrder = new Order();
        savedOrder.setId(100);
        when(orderService.crearOrder(any(Order.class))).thenReturn(savedOrder);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            checkoutService.checkout(request);
        });

        assertEquals("Cantidad inválida. Debe ser mayor a cero.", exception.getMessage());
    }

    @Test
    @DisplayName("CU07 - A4: Producto sin stock suficiente validado por InventoryService")
    void testCheckoutSinStock() {
        Address savedAddress = new Address();
        savedAddress.setId(10);
        when(addressService.crearAddress(any(Address.class))).thenReturn(savedAddress);

        Order savedOrder = new Order();
        savedOrder.setId(100);
        when(orderService.crearOrder(any(Order.class))).thenReturn(savedOrder);

        when(inventoryService.deductCheckoutStockAndCalculateUnitCosts(any(), eq(100), eq(1)))
                .thenThrow(new IllegalStateException("Stock insuficiente para la cantidad seleccionada."));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            checkoutService.checkout(request);
        });

        assertEquals("Stock insuficiente para la cantidad seleccionada.", exception.getMessage());
    }

    @Test
    @DisplayName("Boleta híbrida: Efectivo (PENDING) difiere emisión SUNAT a PENDIENTE_ENTREGA")
    void testCheckoutCashDefersSunatBoletaUntilDelivery() {
        Address savedAddress = new Address();
        savedAddress.setId(10);
        when(addressService.crearAddress(any(Address.class))).thenReturn(savedAddress);

        Order savedOrder = new Order();
        savedOrder.setId(201);
        savedOrder.setTotal(new BigDecimal("55.00"));
        savedOrder.setDeliveryFee(new BigDecimal("5.00"));
        when(orderService.crearOrder(any(Order.class))).thenReturn(savedOrder);
        when(orderService.actualizarOrder(eq(201), any(Order.class))).thenReturn(savedOrder);
        when(orderService.obtenerPorId(201)).thenReturn(java.util.Optional.of(savedOrder));

        when(inventoryService.deductCheckoutStockAndCalculateUnitCosts(any(), eq(201), eq(1)))
                .thenReturn(List.of(new BigDecimal("10.00")));
        when(orderItemService.crearOrderItem(any(OrderItem.class))).thenReturn(new OrderItem());

        com.ecopizza.models.Pizza mockPizza = new com.ecopizza.models.Pizza();
        mockPizza.setId(1);
        mockPizza.setPrice(new BigDecimal("20.00"));
        when(pizzaService.obtenerPorId(1)).thenReturn(java.util.Optional.of(mockPizza));

        com.ecopizza.models.Size mockSize = new com.ecopizza.models.Size();
        mockSize.setId(2);
        mockSize.setExtraCost(new BigDecimal("5.00"));
        when(sizeService.obtenerPorId(2)).thenReturn(java.util.Optional.of(mockSize));

        request.getPayment().setStatus("PENDING");
        request.setWantsBoleta(true);
        request.setClienteDni("12345678");
        request.setClienteNombre("JUAN PEREZ");

        checkoutService.checkout(request);

        verify(sunatService, times(1)).registrarSolicitudBoletaPendiente(201, "12345678", "JUAN PEREZ");
        verify(sunatService, never()).emitirBoletaParaPedido(any(), any(), any());
    }

    @Test
    @DisplayName("Boleta híbrida: Pago adelantado (PAID) emite boleta SUNAT inmediatamente en checkout")
    void testCheckoutPrepaidEmitsSunatBoletaImmediately() {
        Address savedAddress = new Address();
        savedAddress.setId(10);
        when(addressService.crearAddress(any(Address.class))).thenReturn(savedAddress);

        Order savedOrder = new Order();
        savedOrder.setId(202);
        savedOrder.setTotal(new BigDecimal("55.00"));
        savedOrder.setDeliveryFee(new BigDecimal("5.00"));
        when(orderService.crearOrder(any(Order.class))).thenReturn(savedOrder);
        when(orderService.actualizarOrder(eq(202), any(Order.class))).thenReturn(savedOrder);
        when(orderService.obtenerPorId(202)).thenReturn(java.util.Optional.of(savedOrder));

        when(inventoryService.deductCheckoutStockAndCalculateUnitCosts(any(), eq(202), eq(1)))
                .thenReturn(List.of(new BigDecimal("10.00")));
        when(orderItemService.crearOrderItem(any(OrderItem.class))).thenReturn(new OrderItem());

        com.ecopizza.models.Pizza mockPizza = new com.ecopizza.models.Pizza();
        mockPizza.setId(1);
        mockPizza.setPrice(new BigDecimal("20.00"));
        when(pizzaService.obtenerPorId(1)).thenReturn(java.util.Optional.of(mockPizza));

        com.ecopizza.models.Size mockSize = new com.ecopizza.models.Size();
        mockSize.setId(2);
        mockSize.setExtraCost(new BigDecimal("5.00"));
        when(sizeService.obtenerPorId(2)).thenReturn(java.util.Optional.of(mockSize));

        request.getPayment().setStatus("PAID");
        request.setWantsBoleta(true);
        request.setClienteDni("87654321");
        request.setClienteNombre("MARIA LOPEZ");

        checkoutService.checkout(request);

        verify(sunatService, times(1)).emitirBoletaParaPedido(202, "87654321", "MARIA LOPEZ");
        verify(sunatService, never()).registrarSolicitudBoletaPendiente(any(), any(), any());
    }

    @Test
    @DisplayName("Factura FFF1: Pago adelantado (PAID) emite Factura FFF1 inmediatamente con rucVerified=true")
    void testCheckoutPrepaidEmitsSunatFacturaFFF1() {
        Address savedAddress = new Address();
        savedAddress.setId(10);
        when(addressService.crearAddress(any(Address.class))).thenReturn(savedAddress);

        Order savedOrder = new Order();
        savedOrder.setId(203);
        savedOrder.setTotal(new BigDecimal("55.00"));
        savedOrder.setDeliveryFee(new BigDecimal("5.00"));
        when(orderService.crearOrder(any(Order.class))).thenReturn(savedOrder);
        when(orderService.actualizarOrder(eq(203), any(Order.class))).thenReturn(savedOrder);
        when(orderService.obtenerPorId(203)).thenReturn(java.util.Optional.of(savedOrder));

        when(inventoryService.deductCheckoutStockAndCalculateUnitCosts(any(), eq(203), eq(1)))
                .thenReturn(List.of(new BigDecimal("10.00")));
        when(orderItemService.crearOrderItem(any(OrderItem.class))).thenReturn(new OrderItem());

        com.ecopizza.models.Pizza mockPizza = new com.ecopizza.models.Pizza();
        mockPizza.setId(1);
        mockPizza.setPrice(new BigDecimal("20.00"));
        when(pizzaService.obtenerPorId(1)).thenReturn(java.util.Optional.of(mockPizza));

        com.ecopizza.models.Size mockSize = new com.ecopizza.models.Size();
        mockSize.setId(2);
        mockSize.setExtraCost(new BigDecimal("5.00"));
        when(sizeService.obtenerPorId(2)).thenReturn(java.util.Optional.of(mockSize));

        request.getPayment().setStatus("PAID");
        request.setWantsBoleta(true);
        request.setTipoComprobante(1);
        request.setClienteRuc("20538225763");
        request.setClienteNombre("ECOPIZZA S.A.C.");
        request.setClienteDireccion("AV. JAVIER PRADO 4200");
        request.setRucVerified(true);

        checkoutService.checkout(request);

        verify(sunatService, times(1)).emitirComprobanteParaPedido(
                203, 1, "20538225763", "ECOPIZZA S.A.C.", "AV. JAVIER PRADO 4200", true);
        verify(sunatService, never()).registrarSolicitudComprobantePendiente(any(), any(), any(), any(), any(), any());
    }
}
