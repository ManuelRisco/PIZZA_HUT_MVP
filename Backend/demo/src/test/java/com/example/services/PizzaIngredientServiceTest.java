package com.example.services;

import com.example.models.PizzaIngredient;
import com.example.repositories.PizzaIngredientRepository;
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

@DisplayName("Adicional.1: Gestión de Ingredientes de Pizza - Pruebas TDD")
class PizzaIngredientServiceTest {

    @Mock
    private PizzaIngredientRepository repository;

    @InjectMocks
    private PizzaIngredientService service;

    private PizzaIngredient pi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pi = new PizzaIngredient();
    }

    // Fase RED / GREEN / REFACTOR
    @Test
    @DisplayName("Adicional.2: Listar PizzaIngredients")
    void testListar() {
        List<PizzaIngredient> list = new ArrayList<>();
        list.add(pi);
        when(repository.findAll()).thenReturn(list);

        List<PizzaIngredient> resultado = service.listarPizzaIngredientes();

        assertEquals(1, resultado.size());
        verify(repository, times(1)).findAll();
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Adicional.3: Crear PizzaIngredient")
    void testCrear() {
        when(repository.save(any(PizzaIngredient.class))).thenReturn(pi);
        PizzaIngredient resultado = service.crearPizzaIngrediente(pi);
        assertNotNull(resultado);
        verify(repository, times(1)).save(pi);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Adicional.4: Obtener por ID")
    void testObtenerPorId() {
        when(repository.findById(any())).thenReturn(Optional.of(pi));
        Optional<PizzaIngredient> result = service.obtenerPorId(null);
        assertTrue(result.isPresent());
        verify(repository, times(1)).findById(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Adicional.5: Actualizar PizzaIngredient")
    void testActualizar() {
        when(repository.findById(any())).thenReturn(Optional.of(pi));
        when(repository.save(any(PizzaIngredient.class))).thenReturn(pi);
        PizzaIngredient result = service.actualizarPizzaIngrediente(null, pi);
        assertNotNull(result);
        verify(repository, times(1)).findById(any());
        verify(repository, times(1)).save(pi);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("Adicional.6: Eliminar PizzaIngredient")
    void testEliminar() {
        when(repository.existsById(any())).thenReturn(true);
        doNothing().when(repository).deleteById(any());
        boolean result = service.eliminarPizzaIngrediente(null);
        assertTrue(result);
        verify(repository, times(1)).existsById(any());
        verify(repository, times(1)).deleteById(any());
    }
}
