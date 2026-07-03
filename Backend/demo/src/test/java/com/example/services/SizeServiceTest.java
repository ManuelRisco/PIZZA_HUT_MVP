package com.example.services;

import com.example.models.Size;
import com.example.repositories.SizeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CU15: Gestionar tamaños de pizza - Pruebas TDD")
class SizeServiceTest {

    @Mock
    private SizeRepository repository;

    @InjectMocks
    private SizeService service;

    private Size size;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        size = new Size();
        size.setId(1);
        size.setName("Familiar");
        size.setExtraCost(new BigDecimal("1.50"));
    }

    // ==========================================
    // CU15: Gestionar Tamaños
    // ==========================================

    @Test
    @DisplayName("CU15 - Flujo Principal: Listar tamaños [RF27]")
    void testListar() {
        List<Size> list = new ArrayList<>();
        list.add(size);
        when(repository.findByDeletedAtIsNull()).thenReturn(List.of(size));

        List<Size> result = service.listarSizes();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository, times(1)).findByDeletedAtIsNull();
    }

    @Test
    @DisplayName("CU15 - Flujo Principal: Crear tamaño exitosamente [RF27, RF28]")
    void testCrearSize() {
        when(repository.existsByNameAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(repository.findByName(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(Size.class))).thenReturn(size);

        Size resultado = service.crearSize(size);

        assertNotNull(resultado);
        verify(repository, times(1)).save(size);
    }

    @Test
    @DisplayName("CU15 - Flujo Principal: Actualizar tamaño [RF27, RF28]")
    void testActualizar() {
        when(repository.findById(1)).thenReturn(Optional.of(size));
        when(repository.save(any(Size.class))).thenReturn(size);

        Size result = service.actualizarSize(1, size);

        assertNotNull(result);
        verify(repository, times(1)).findById(1);
        verify(repository, times(1)).save(size);
    }

    @Test
    @DisplayName("CU15 - Flujo Principal: Eliminar tamaño (Soft Delete) [RF27]")
    void testEliminar() {
        when(repository.findById(1)).thenReturn(Optional.of(size));
        when(repository.save(any(Size.class))).thenReturn(size);

        service.eliminarSize(1);
        verify(repository, times(1)).findById(1);
        verify(repository, times(1)).save(size);
    }

    @Test
    @DisplayName("CU15 - A1: Error al intentar crear tamaño con nombre duplicado")
    void testCrearSizeNombreDuplicado() {
        when(repository.existsByNameAndDeletedAtIsNull(anyString())).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.crearSize(size);
        });

        assertEquals("Ya existe un tamaño con ese nombre", exception.getMessage());
        verify(repository, never()).save(any(Size.class));
    }

    @Test
    @DisplayName("CU15 - A2: Error al intentar crear tamaño con multiplicador de precio negativo")
    void testCrearSizePrecioNegativo() {
        size.setExtraCost(new BigDecimal("-1.00"));

        // Simulando excepcion de constraint o validación de servicio
        when(repository.existsByNameAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(repository.findByName(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(Size.class)))
                .thenThrow(new IllegalArgumentException("El multiplicador de precio no puede ser negativo."));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.crearSize(size);
        });

        assertEquals("El multiplicador de precio no puede ser negativo.", exception.getMessage());
    }
}
