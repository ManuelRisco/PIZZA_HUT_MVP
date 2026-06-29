package com.example.services;

import com.example.dtos.CheckoutRequestDTO;
import com.example.dtos.OrderDTO;
import com.example.models.Address;
import com.example.models.Order;
import com.example.models.OrderItem;
import com.example.models.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

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
    @DisplayName("CU07 - Flujo Principal: Procesar checkout atómicamente y actualizar total [RF13, RF14]")
    void testCheckoutSuccess() {
        // Arrange
        Address savedAddress = new Address();
        savedAddress.setId(10);
        when(addressService.crearAddress(any(Address.class))).thenReturn(savedAddress);

        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setTotal(new BigDecimal("55.00"));
        when(orderService.crearOrder(any(Order.class))).thenReturn(savedOrder);

        OrderItem savedItem = new OrderItem();
        savedItem.setId(1000);
        when(orderItemService.crearOrderItem(any(OrderItem.class))).thenReturn(savedItem);

        // Act
        Order result = checkoutService.checkout(request);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals(new BigDecimal("55.00"), result.getTotal());

        verify(addressService, times(1)).crearAddress(any(Address.class));
        verify(orderService, times(1)).crearOrder(any(Order.class));
        verify(orderItemService, times(1)).crearOrderItem(any(OrderItem.class));
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
    @DisplayName("CU07 - A4: Producto sin stock suficiente (Simulado)")
    void testCheckoutSinStock() {
        Address savedAddress = new Address();
        savedAddress.setId(10);
        when(addressService.crearAddress(any(Address.class))).thenReturn(savedAddress);
        // Simulando excepcion de stock
        when(orderService.crearOrder(any(Order.class))).thenThrow(new IllegalStateException("Stock insuficiente para la cantidad seleccionada."));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            checkoutService.checkout(request);
        });

        assertEquals("Stock insuficiente para la cantidad seleccionada.", exception.getMessage());
    }
}
