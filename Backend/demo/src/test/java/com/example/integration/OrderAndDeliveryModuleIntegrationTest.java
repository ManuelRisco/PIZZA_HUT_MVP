package com.example.integration;

import com.example.models.*;
import com.example.repositories.*;
import com.example.services.OrderService;
import com.example.services.OrderTrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Test de Integración del Módulo 4: Pedidos, Congelamiento de Costos y Seguimiento")
public class OrderAndDeliveryModuleIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderTrackingService orderTrackingService;

    @Autowired
    private OrderTrackingRepository orderTrackingRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Flujo completo: Crear orden con congelamiento histórico de costos unitarios y avanzar ciclo de delivery")
    void testOrderLifecycleAndCostFreezing() {
        Usuario usuario = usuarioRepository.findByEmail("delivery_test@pizzahut.com")
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setName("Cliente Delivery Test");
                    u.setEmail("delivery_test_" + System.currentTimeMillis() + "@pizzahut.com");
                    u.setPasswordHash("$2a$10$N.ZOmG4byf24u5U5u5u5u.abcdefghijklmnopqrstuvw");
                    u.setRole(Usuario.Role.CUSTOMER);
                    return usuarioRepository.save(u);
                });

        // 1. Cabecera de Orden
        Order order = new Order();
        order.setUserId(usuario.getId().intValue());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setDeliveryType(Order.DeliveryType.DELIVERY);
        order.setPaymentMethodId(1);
        order.setSubtotal(new BigDecimal("70.00"));
        order.setDeliveryFee(new BigDecimal("5.00"));
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(new BigDecimal("75.00"));
        order.setStockSettled("DEDUCTED");
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(40));

        Order orderGuardada = orderService.crearOrder(order);
        assertNotNull(orderGuardada.getId());

        // 2. Detalle de Items con costos unitarios directos congelados
        OrderItem item1 = new OrderItem();
        item1.setOrderId(orderGuardada.getId());
        item1.setItemType("PIZZA");
        item1.setPizzaId(1);
        item1.setSizeId(1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("35.00"));
        item1.setSizeExtra(BigDecimal.ZERO);
        item1.setLineTotal(new BigDecimal("70.00"));
        // Costo directo unitario congelado en el instante de compra
        item1.setUnitCost(new BigDecimal("12.50"));
        item1.setLineCost(new BigDecimal("25.00"));
        orderItemRepository.save(item1);

        // 3. Crear primer hito de tracking: PENDING
        OrderTracking tracking1 = new OrderTracking();
        tracking1.setOrderId(orderGuardada.getId());
        tracking1.setStatus(Order.OrderStatus.PENDING);
        tracking1.setDescription("Pedido recibido y registrado en caja");
        tracking1.setCreatedAt(LocalDateTime.now());
        orderTrackingService.crearOrderTracking(tracking1);

        // 4. Avanzar tracking a PREPARING (En Cocina)
        OrderTracking tracking2 = new OrderTracking();
        tracking2.setOrderId(orderGuardada.getId());
        tracking2.setStatus(Order.OrderStatus.PREPARING);
        tracking2.setDescription("Pizza en horno artesanal");
        tracking2.setCreatedAt(LocalDateTime.now().plusMinutes(5));
        orderTrackingService.crearOrderTracking(tracking2);

        // 5. Avanzar orden a DELIVERED (Entregado)
        orderGuardada.setStatus(Order.OrderStatus.DELIVERED);
        orderService.actualizarOrder(orderGuardada.getId(), orderGuardada);

        // 6. Validar persistencia y fidelidad histórica de costos
        Order orderFinal = orderRepository.findById(orderGuardada.getId()).orElseThrow();
        assertEquals(Order.OrderStatus.DELIVERED, orderFinal.getStatus());

        List<OrderItem> items = orderItemRepository.findByOrderId(orderGuardada.getId());
        assertEquals(1, items.size());
        assertEquals(new BigDecimal("12.50"), items.get(0).getUnitCost(), "El costo unitario debe permanecer congelado");
        assertEquals(new BigDecimal("25.00"), items.get(0).getLineCost());

        List<OrderTracking> trackings = orderTrackingRepository.findByOrderId(orderGuardada.getId());
        assertTrue(trackings.size() >= 2);
        assertTrue(trackings.stream().anyMatch(t -> t.getStatus() == Order.OrderStatus.PENDING));
        assertTrue(trackings.stream().anyMatch(t -> t.getStatus() == Order.OrderStatus.PREPARING));
    }
}
