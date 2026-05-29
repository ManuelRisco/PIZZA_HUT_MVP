package com.example.service;

import com.example.domain.model.Order;
import com.example.domain.repository.OrderRepository;
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

@DisplayName("UC2: Pruebas de Gestión de Órdenes - TDD")
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

    @Test
    @DisplayName("UC2.1: Crear nueva orden correctamente")
    void testCrearOrdenExitosa() {
        when(orderRepository.save(orderTest)).thenReturn(orderTest);
        Order ordenCreada = orderService.crearOrder(orderTest);
        assertNotNull(ordenCreada);
        assertEquals(Order.OrderStatus.PENDING, ordenCreada.getStatus());
        assertEquals(new BigDecimal("55.00"), ordenCreada.getTotal());
        verify(orderRepository, times(1)).save(orderTest);
    }

    @Test
    @DisplayName("UC2.2: Obtener orden por ID")
    void testObtenerOrdenPorId() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(orderTest));
        Optional<Order> ordenEncontrada = orderService.obtenerPorId(1);
        assertTrue(ordenEncontrada.isPresent());
        assertEquals(10, ordenEncontrada.get().getUserId());
        verify(orderRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("UC2.3: Obtener órdenes por estado")
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
    @DisplayName("UC2.4: Actualizar estado de orden")
    void testActualizarOrdenStatus() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(orderTest));
        Order ordenActualizada = new Order();
        ordenActualizada.setStatus(Order.OrderStatus.CONFIRMED);
        ordenActualizada.setTotal(new BigDecimal("55.00"));
        when(orderRepository.save(any(Order.class))).thenReturn(orderTest);
        Order resultado = orderService.actualizarOrder(1, ordenActualizada);
        assertNotNull(resultado);
        verify(orderRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("UC2.5: Validar error cuando orden no existe")
    void testActualizarOrdenNoExistente() {
        when(orderRepository.findById(999)).thenReturn(Optional.empty());
        Order ordenActualizada = new Order();
        ordenActualizada.setStatus(Order.OrderStatus.CONFIRMED);
        assertThrows(IllegalArgumentException.class, 
            () -> orderService.actualizarOrder(999, ordenActualizada));
        verify(orderRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("UC2.6: Obtener órdenes por usuario")
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
    @DisplayName("UC2.7: Listar todas las órdenes")
    void testListarTodasLasOrdenes() {
        List<Order> ordenes = new ArrayList<>();
        ordenes.add(orderTest);
        when(orderRepository.findAll()).thenReturn(ordenes);
        List<Order> ordenesEncontradas = orderService.listarOrders();
        assertEquals(1, ordenesEncontradas.size());
        verify(orderRepository, times(1)).findAll();
    }
}
