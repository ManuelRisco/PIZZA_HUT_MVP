package com.example.services;

import com.example.models.Ingredient;
import com.example.repositories.IngredientRepository;
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

@DisplayName("CU14: Gestionar ingredientes - Pruebas TDD")
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private IngredientService ingredientService;

    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ingredient = new Ingredient();
        ingredient.setId(1);
        ingredient.setName("Tomate");
    }

    // ==========================================
    // CU14: Gestionar Ingredientes
    // ==========================================

    @Test
    @DisplayName("CU14 - Flujo Principal: Listar ingredientes [RF25]")
    void testListarIngredients() {
        List<Ingredient> list = new ArrayList<>();
        list.add(ingredient);
        when(ingredientRepository.findAll()).thenReturn(list);

        List<Ingredient> resultado = ingredientService.listarIngredientes();

        assertEquals(1, resultado.size());
        verify(ingredientRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("CU14 - Flujo Principal: Crear ingrediente exitosamente [RF25, RF26]")
    void testCrearIngredient() {
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);

        Ingredient resultado = ingredientService.crearIngrediente(ingredient);

        assertNotNull(resultado);
        verify(ingredientRepository, times(1)).save(ingredient);
    }

    @Test
    @DisplayName("CU14 - Flujo Principal: Actualizar Ingrediente [RF25, RF26]")
    void testActualizar() {
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);

        Ingredient result = ingredientService.actualizarIngrediente(1, ingredient);

        assertNotNull(result);
        verify(ingredientRepository, times(1)).findById(1);
        verify(ingredientRepository, times(1)).save(ingredient);
    }

    @Test
    @DisplayName("CU14 - Flujo Principal: Eliminar Ingrediente (Soft delete) [RF25]")
    void testEliminar() {
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);

        boolean result = ingredientService.eliminarIngrediente(1);

        assertTrue(result);
        verify(ingredientRepository, times(1)).findById(1);
        verify(ingredientRepository, times(1)).save(ingredient);
    }

    @Test
    @DisplayName("CU14 - A1: Error al intentar crear ingrediente con nombre duplicado")
    void testCrearIngredienteNombreDuplicado() {
        when(ingredientRepository.save(any(Ingredient.class)))
                .thenThrow(new IllegalArgumentException("El nombre del ingrediente ya está registrado."));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ingredientService.crearIngrediente(ingredient);
        });

        assertEquals("El nombre del ingrediente ya está registrado.", exception.getMessage());
    }
}
