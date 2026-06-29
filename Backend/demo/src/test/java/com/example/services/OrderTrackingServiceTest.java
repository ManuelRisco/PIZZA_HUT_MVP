package com.example.services;

import com.example.models.OrderTracking;
import com.example.repositories.OrderTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Adicional.1: Seguimiento de Pedidos - Pruebas TDD")
class OrderTrackingServiceTest {

    @Mock
    private OrderTrackingRepository repository;

    @InjectMocks
    private OrderTrackingService service;

    private OrderTracking tracking;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tracking = new OrderTracking();
        tracking.setId(1);
    }

    // Fase RED / GREEN / REFACTOR
    @Test
    @DisplayName("Adicional.2: Listar OrderTracking")
    void testListar() {
        List<OrderTracking> list = new ArrayList<>();
        list.add(tracking);
        when(repository.findAll()).thenReturn(list);

        List<OrderTracking> resultado = service.listarOrderTrackings();

        assertEquals(1, resultado.size());
        verify(repository, times(1)).findAll();
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Adicional.3: Crear OrderTracking")
    void testCrear() {
        when(repository.save(any(OrderTracking.class))).thenReturn(tracking);
        OrderTracking resultado = service.crearOrderTracking(tracking);
        assertNotNull(resultado);
        verify(repository, times(1)).save(tracking);
    }

    @Test
    @DisplayName("Adicional.4: Obtener por ID")
    void testObtenerPorId() {
        when(repository.findById(1)).thenReturn(Optional.of(tracking));
        Optional<OrderTracking> result = service.obtenerPorId(1);
        assertTrue(result.isPresent());
        verify(repository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Adicional.5: Obtener por Order ID")
    void testObtenerPorOrderId() {
        List<OrderTracking> list = new ArrayList<>();
        list.add(tracking);
        when(repository.findByOrderId(1)).thenReturn(list);
        List<OrderTracking> result = service.obtenerPorOrderId(1);
        assertEquals(1, result.size());
        verify(repository, times(1)).findByOrderId(1);
    }

    @Test
    @DisplayName("Adicional.6: Eliminar OrderTracking")
    void testEliminar() {
        when(repository.existsById(1)).thenReturn(true);
        doNothing().when(repository).deleteById(1);
        service.eliminarOrderTracking(1);
        verify(repository, times(1)).existsById(1);
        verify(repository, times(1)).deleteById(1);
    }
}
