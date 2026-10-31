package com.example.services;

import com.example.models.PaymentMethod;
import com.example.repositories.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test de Métodos de Pago - Casos de Uso")
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository repository;

    @Mock
    private com.example.repositories.PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentMethodService service;

    private PaymentMethod method;

    @BeforeEach
    void setUp() {
        method = new PaymentMethod();
        method.setId(1);
        method.setName("Tarjeta de Crédito");
        method.setDescription("Visa / Mastercard");
        method.setActive(true);
        method.setDisplayOrder(1);
    }

    @Test
    @DisplayName("CU18 - Flujo Principal: Listar métodos de pago activos [RF33]")
    void testListarMetodos() {
        when(repository.findByDeletedAtIsNullOrderByDisplayOrderAsc()).thenReturn(List.of(method));

        List<PaymentMethod> result = service.obtenerTodos();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(repository, times(1)).findByDeletedAtIsNullOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("CU18 - Flujo Principal: Crear método de pago [RF33]")
    void testCrearMetodo() {
        when(repository.findByName("Tarjeta de Crédito")).thenReturn(Optional.empty());
        when(repository.existsByDisplayOrderAndDeletedAtIsNull(1)).thenReturn(false);
        when(repository.save(any(PaymentMethod.class))).thenReturn(method);

        PaymentMethod result = service.crearMetodoPago(method);

        assertNotNull(result);
        assertEquals("Tarjeta de Crédito", result.getName());
        verify(repository, times(1)).save(method);
    }

    @Test
    @DisplayName("CU18 - Flujo Principal: Actualizar método de pago [RF33, RF34]")
    void testActualizar() {
        when(repository.findById(1)).thenReturn(Optional.of(method));
        when(repository.save(any(PaymentMethod.class))).thenReturn(method);

        PaymentMethod result = service.actualizarMetodoPago(1, method);

        assertNotNull(result);
        verify(repository, times(1)).findById(1);
        verify(repository, times(1)).save(method);
    }

    @Test
    @DisplayName("CU18 - Flujo Principal: Eliminar método de pago lógicamente (Soft Delete) [RF33]")
    void testEliminar() {
        when(repository.findById(1)).thenReturn(Optional.of(method));
        when(repository.save(any(PaymentMethod.class))).thenReturn(method);

        boolean result = service.eliminarMetodoPago(1);

        assertTrue(result);
        assertNotNull(method.getDeletedAt());
        assertFalse(method.isActive());
        verify(repository, times(1)).findById(1);
        verify(repository, times(1)).save(method);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("CU18 - Flujo Alternativo: Restaurar método de pago soft-deleted [RF33]")
    void testRestaurar() {
        method.setDeletedAt(java.time.LocalDateTime.now());
        method.setActive(false);
        when(repository.findById(1)).thenReturn(Optional.of(method));
        when(repository.save(any(PaymentMethod.class))).thenReturn(method);

        boolean result = service.restaurarMetodoPago(1);

        assertTrue(result);
        assertNull(method.getDeletedAt());
        assertTrue(method.isActive());
        verify(repository, times(1)).findById(1);
        verify(repository, times(1)).save(method);
    }

    @Test
    @DisplayName("CU18 - A1: Error al guardar con datos incompletos (sin nombre)")
    void testCrearMetodoDatosIncompletos() {
        PaymentMethod metodoInvalido = new PaymentMethod();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.crearMetodoPago(metodoInvalido);
        });

        assertEquals("Debe ingresar un nombre válido.", exception.getMessage());
    }

    @Test
    @DisplayName("CU18 - A2: Error al intentar crear método con nombre duplicado")
    void testCrearMetodoNombreDuplicado() {
        when(repository.findByName("Tarjeta de Crédito")).thenReturn(Optional.of(method));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.crearMetodoPago(method);
        });

        assertEquals("El método de pago ya existe.", exception.getMessage());
    }
}
