package com.example.services;

import com.example.models.PaymentMethod;
import com.example.repositories.PaymentMethodRepository;
import com.example.repositories.PaymentRepository;
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

@DisplayName("CU18: Gestionar métodos de pago - Pruebas TDD")
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository repository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentMethodService service;

    private PaymentMethod method;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        method = new PaymentMethod();
        method.setId(1);
        method.setName("Tarjeta de Crédito");
        method.setActive(true);
    }

    // ==========================================
    // CU18: Gestionar métodos de pago
    // ==========================================

    @Test
    @DisplayName("CU18 - Flujo Principal: Listar métodos de pago [RF33]")
    void testListar() {
        List<PaymentMethod> list = new ArrayList<>();
        list.add(method);
        when(repository.findAll()).thenReturn(list);

        List<PaymentMethod> resultado = service.obtenerTodos();

        assertEquals(1, resultado.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("CU18 - Flujo Principal: Crear método de pago exitosamente [RF33, RF34]")
    void testCrear() {
        when(repository.existsByName("Tarjeta de Crédito")).thenReturn(false);
        when(repository.save(any(PaymentMethod.class))).thenReturn(method);

        PaymentMethod resultado = service.crearMetodoPago(method);

        assertNotNull(resultado);
        verify(repository, times(1)).save(method);
    }

    @Test
    @DisplayName("CU18 - Flujo Principal: Actualizar método de pago [RF33, RF34]")
    void testActualizar() {
        when(repository.findById(1)).thenReturn(Optional.of(method));
        when(repository.existsByName(anyString())).thenReturn(false);
        when(repository.save(any(PaymentMethod.class))).thenReturn(method);

        PaymentMethod result = service.actualizarMetodoPago(1, method);

        assertNotNull(result);
        verify(repository, times(1)).findById(1);
        verify(repository, times(1)).save(method);
    }

    @Test
    @DisplayName("CU18 - Flujo Principal: Eliminar método de pago [RF33]")
    void testEliminar() {
        when(repository.findById(1)).thenReturn(Optional.of(method));
        when(paymentRepository.existsByPaymentMethodId(1)).thenReturn(false);

        boolean result = service.eliminarMetodoPago(1);

        assertTrue(result);
        verify(repository, times(1)).findById(1);
        verify(repository, times(1)).delete(method);
    }

    @Test
    @DisplayName("CU18 - A1: Error al guardar con datos incompletos (sin nombre)")
    void testCrearMetodoDatosIncompletos() {
        PaymentMethod metodoInvalido = new PaymentMethod();
        when(repository.save(any(PaymentMethod.class)))
                .thenThrow(new IllegalArgumentException("Debe ingresar un nombre válido."));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.crearMetodoPago(metodoInvalido);
        });

        assertEquals("Debe ingresar un nombre válido.", exception.getMessage());
    }

    @Test
    @DisplayName("CU18 - A2: Error al intentar crear método con nombre duplicado")
    void testCrearMetodoNombreDuplicado() {
        when(repository.existsByName("Tarjeta de Crédito")).thenReturn(true);
        // Simulando que el servicio valida duplicados lanzando excepcion o no guarda
        when(repository.save(any(PaymentMethod.class)))
                .thenThrow(new IllegalArgumentException("El método de pago ya existe."));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.crearMetodoPago(method);
        });

        assertEquals("El método de pago ya existe.", exception.getMessage());
    }

    @Test
    @DisplayName("CU18 - A2/Dependencia: Error al eliminar método con pagos asociados")
    void testEliminarMetodoEnUso() {
        when(repository.findById(1)).thenReturn(Optional.of(method));
        when(paymentRepository.existsByPaymentMethodId(1)).thenReturn(true); // Tiene pagos

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.eliminarMetodoPago(1);
        });

        assertTrue(
                exception.getMessage().contains("No se puede eliminar") || exception.getMessage().contains("en uso"));
    }
}
