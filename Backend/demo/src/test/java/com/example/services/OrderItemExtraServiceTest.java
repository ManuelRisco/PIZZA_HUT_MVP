package com.example.services;

import com.example.models.OrderItemExtra;
import com.example.repositories.OrderItemExtraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Adicional.1: Gestión de Extras por Ítem - Pruebas TDD")
class OrderItemExtraServiceTest {

    @Mock
    private OrderItemExtraRepository repository;

    @InjectMocks
    private OrderItemExtraService service;

    private OrderItemExtra itemExtra;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemExtra = new OrderItemExtra();
        itemExtra.setId(1);
    }

    // Fase RED / GREEN / REFACTOR
    @Test
    @DisplayName("Adicional.2: Listar OrderItemExtras")
    void testListar() {
        List<OrderItemExtra> list = new ArrayList<>();
        list.add(itemExtra);
        when(repository.findAll()).thenReturn(list);

        List<OrderItemExtra> resultado = service.listarTodos();

        assertEquals(1, resultado.size());
        verify(repository, times(1)).findAll();
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Adicional.3: Crear OrderItemExtra")
    void testCrear() {
        when(repository.save(any(OrderItemExtra.class))).thenReturn(itemExtra);
        OrderItemExtra resultado = service.crearOrderItemExtra(itemExtra);
        assertNotNull(resultado);
        verify(repository, times(1)).save(itemExtra);
    }

    @Test
    @DisplayName("Adicional.4: Eliminar por ID")
    void testEliminar() {
        doNothing().when(repository).deleteById(1);
        service.eliminar(1);
        verify(repository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("Adicional.5: Obtener por Order Item ID")
    void testObtenerPorOrderItemId() {
        List<OrderItemExtra> list = new ArrayList<>();
        list.add(itemExtra);
        when(repository.findByOrderItemId(1)).thenReturn(list);
        List<OrderItemExtra> result = service.obtenerPorOrderItemId(1);
        assertEquals(1, result.size());
        verify(repository, times(1)).findByOrderItemId(1);
    }
}
