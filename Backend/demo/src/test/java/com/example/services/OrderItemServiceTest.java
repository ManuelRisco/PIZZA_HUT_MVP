package com.example.services;

import com.example.models.OrderItem;
import com.example.repositories.OrderItemRepository;
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

@DisplayName("Adicional.1: Gestión de Ítems de Orden - Pruebas TDD")
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository repository;

    @InjectMocks
    private OrderItemService service;

    private OrderItem item;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        item = new OrderItem();
        item.setId(1);
    }

    // Fase RED / GREEN / REFACTOR
    @Test
    @DisplayName("Adicional.2: Listar OrderItems")
    void testListar() {
        List<OrderItem> list = new ArrayList<>();
        list.add(item);
        when(repository.findAll()).thenReturn(list);

        List<OrderItem> resultado = service.listarOrderItems();

        assertEquals(1, resultado.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Adicional.3: Crear OrderItem")
    void testCrear() {
        when(repository.save(any(OrderItem.class))).thenReturn(item);
        OrderItem resultado = service.crearOrderItem(item);
        assertNotNull(resultado);
        verify(repository, times(1)).save(item);
    }

    @Test
    @DisplayName("Adicional.4: Obtener por ID")
    void testObtenerPorId() {
        when(repository.findById(1)).thenReturn(Optional.of(item));
        Optional<OrderItem> result = service.obtenerPorId(1);
        assertTrue(result.isPresent());
        verify(repository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Adicional.5: Actualizar OrderItem")
    void testActualizar() {
        when(repository.existsById(1)).thenReturn(true);
        when(repository.save(any(OrderItem.class))).thenReturn(item);
        OrderItem result = service.actualizarOrderItem(1, item);
        assertNotNull(result);
        verify(repository, times(1)).existsById(1);
        verify(repository, times(1)).save(item);
    }

    @Test
    @DisplayName("Adicional.6: Eliminar OrderItem")
    void testEliminar() {
        when(repository.existsById(1)).thenReturn(true);
        doNothing().when(repository).deleteById(1);
        service.eliminarOrderItem(1);
        verify(repository, times(1)).existsById(1);
        verify(repository, times(1)).deleteById(1);
    }
}
