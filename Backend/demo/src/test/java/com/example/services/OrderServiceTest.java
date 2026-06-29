package com.example.services;

import com.example.models.Order;
import com.example.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CU16: Ver lista de pedidos - Pruebas TDD")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order orderTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        orderTest = new Order();
        orderTest.setId(1);
        orderTest.setUserId(10);
        orderTest.setStatus(Order.OrderStatus.PENDING);
        orderTest.setSubtotal(new BigDecimal("50.00"));
        orderTest.setDeliveryFee(new BigDecimal("5.00"));
        orderTest.setDiscount(new BigDecimal("0.00"));
        orderTest.setTotal(new BigDecimal("55.00"));
        orderTest.setCreatedAt(LocalDateTime.now());
    }

    // ==========================================
    // CU16: Ver lista de pedidos
    // ==========================================

    @Test
    @DisplayName("CU16 - Flujo Principal: Listar todas las órdenes [RF29]")
    void testListarTodasLasOrdenes() {
        List<Order> ordenes = new ArrayList<>();
        ordenes.add(orderTest);
        when(orderRepository.findAll()).thenReturn(ordenes);

        List<Order> ordenesEncontradas = orderService.listarOrders();

        assertEquals(1, ordenesEncontradas.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("CU16 - Flujo Principal: Filtrar órdenes por estado [RF30]")
    void testObtenerOrdenesPorStatus() {
        List<Order> ordenes = new ArrayList<>();
        ordenes.add(orderTest);
        when(orderRepository.findByStatus(Order.OrderStatus.PENDING)).thenReturn(ordenes);

        List<Order> ordenesEncontradas = orderService.obtenerPorStatus(Order.OrderStatus.PENDING);

        assertEquals(1, ordenesEncontradas.size());
        assertEquals(Order.OrderStatus.PENDING, ordenesEncontradas.get(0).getStatus());
        verify(orderRepository, times(1)).findByStatus(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("CU16 - Flujo Principal: Filtrar órdenes por usuario [RF30]")
    void testObtenerOrdenesPorUsuario() {
        List<Order> ordenes = new ArrayList<>();
        ordenes.add(orderTest);
        when(orderRepository.findByUserId(10)).thenReturn(ordenes);

        List<Order> ordenesEncontradas = orderService.obtenerPorUserId(10);

        assertEquals(1, ordenesEncontradas.size());
        assertEquals(10, ordenesEncontradas.get(0).getUserId());
        verify(orderRepository, times(1)).findByUserId(10);
    }

    @Test
    @DisplayName("CU16 - A1: Error/Validación si no existen pedidos en el sistema")
    void testListarOrdenesSinResultados() {
        when(orderRepository.findAll()).thenReturn(new ArrayList<>());

        List<Order> ordenesEncontradas = orderService.listarOrders();

        assertTrue(ordenesEncontradas.isEmpty());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("CU16 - A2: Filtros de búsqueda sin resultados (por estado)")
    void testFiltroOrdenesSinResultados() {
        when(orderRepository.findByStatus(Order.OrderStatus.DELIVERED)).thenReturn(new ArrayList<>());

        List<Order> ordenesEncontradas = orderService.obtenerPorStatus(Order.OrderStatus.DELIVERED);

        assertTrue(ordenesEncontradas.isEmpty());
        verify(orderRepository, times(1)).findByStatus(Order.OrderStatus.DELIVERED);
    }

    // ==========================================
    // Actualizaciones de Orden (Parte del ciclo de vida de la orden)
    // ==========================================

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU16 - Adicional: Actualizar estado de orden existente")
    void testActualizarOrdenStatus() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(orderTest));

        Order ordenActualizada = new Order();
        ordenActualizada.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.save(any(Order.class))).thenReturn(orderTest);

        Order resultado = orderService.actualizarOrder(1, ordenActualizada);

        assertNotNull(resultado);
        verify(orderRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("CU16 - Adicional: Actualizar estado de orden inexistente")
    void testActualizarOrdenNoExistente() {
        when(orderRepository.findById(999)).thenReturn(Optional.empty());
        Order ordenActualizada = new Order();
        ordenActualizada.setStatus(Order.OrderStatus.CONFIRMED);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.actualizarOrder(999, ordenActualizada));

        verify(orderRepository, times(1)).findById(999);
    }
}
