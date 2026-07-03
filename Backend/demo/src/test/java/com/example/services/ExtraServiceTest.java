package com.example.services;

import com.example.models.Extra;
import com.example.repositories.ExtraRepository;
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

@DisplayName("Adicional.1: Gestión de Extras - Pruebas TDD")
class ExtraServiceTest {

    @Mock
    private ExtraRepository extraRepository;

    @InjectMocks
    private ExtraService extraService;

    private Extra extra;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        extra = new Extra();
        extra.setId(1);
        extra.setName("Queso Extra");
        extra.setPrice(new BigDecimal("2.50"));
    }

    // Fase RED / GREEN / REFACTOR
    @Test
    @DisplayName("Adicional.2: Listar extras")
    void testListarExtras() {
        List<Extra> list = new ArrayList<>();
        list.add(extra);
        when(extraRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc()).thenReturn(list);

        List<Extra> resultado = extraService.listarExtras();

        assertEquals(1, resultado.size());
        verify(extraRepository, times(1)).findByDeletedAtIsNullOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("Adicional.3: Crear extra")
    void testCrearExtra() {
        when(extraRepository.existsByName(extra.getName())).thenReturn(false);
        when(extraRepository.save(any(Extra.class))).thenReturn(extra);
        Extra resultado = extraService.crearExtra(extra);
        assertNotNull(resultado);
        verify(extraRepository, times(1)).save(extra);
    }

    @Test
    @DisplayName("Adicional.4: Obtener por ID")
    void testObtenerPorId() {
        when(extraRepository.findById(1)).thenReturn(Optional.of(extra));
        Optional<Extra> result = extraService.obtenerPorId(1);
        assertTrue(result.isPresent());
        verify(extraRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Adicional.5: Actualizar Extra")
    void testActualizar() {
        when(extraRepository.findById(1)).thenReturn(Optional.of(extra));
        when(extraRepository.save(any(Extra.class))).thenReturn(extra);
        Extra result = extraService.actualizarExtra(1, extra);
        assertNotNull(result);
        verify(extraRepository, times(1)).findById(1);
        verify(extraRepository, times(1)).save(extra);
    }

    @Test
    @DisplayName("Adicional.6: Eliminar Extra")
    void testEliminar() {
        when(extraRepository.findById(1)).thenReturn(Optional.of(extra));
        when(extraRepository.save(any(Extra.class))).thenReturn(extra);
        extraService.eliminarExtra(1);
        verify(extraRepository, times(1)).findById(1);
        verify(extraRepository, times(1)).save(extra);
    }
}
