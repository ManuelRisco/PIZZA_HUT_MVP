package com.example.services;

import com.example.models.Ingredient;
import com.example.repositories.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test de Servicios: IngredientService")
public class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private IngredientService ingredientService;

    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        ingredient = new Ingredient();
        ingredient.setId(1);
        ingredient.setName("Queso Mozzarella");
        ingredient.setExtraCost(new BigDecimal("2.50"));
        ingredient.setAvailable(true);
        ingredient.setUnit("g");
        ingredient.setStockQuantity(new BigDecimal("1000.00"));
        ingredient.setMinStock(new BigDecimal("200.00"));
        ingredient.setUnitCost(new BigDecimal("0.05"));
        ingredient.setExtraPortionQuantity(new BigDecimal("50.00"));
        ingredient.setAllowAsExtra(true);
    }

    @Test
    @DisplayName("CU14 - Flujo Principal: Listar ingredientes [RF25]")
    void testListarIngredientes() {
        List<Ingredient> list = new ArrayList<>();
        list.add(ingredient);
        when(ingredientRepository.findByDeletedAtIsNull()).thenReturn(list);

        List<Ingredient> resultado = ingredientService.listarIngredientes();

        assertEquals(1, resultado.size());
        verify(ingredientRepository, times(1)).findByDeletedAtIsNull();
    }

    @Test
    @DisplayName("CU14 - Flujo Principal: Crear ingrediente exitosamente [RF25, RF26]")
    void testCrearIngredient() {
        when(ingredientRepository.findByName(any())).thenReturn(Optional.empty());
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
        assertNotNull(ingredient.getDeletedAt());
        verify(ingredientRepository, times(1)).findById(1);
        verify(ingredientRepository, times(1)).save(ingredient);
        verify(ingredientRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CU14 - Flujo Alternativo: Restaurar Ingrediente soft-deleted [RF25]")
    void testRestaurar() {
        ingredient.setDeletedAt(LocalDateTime.now());
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.save(any(Ingredient.class))).thenReturn(ingredient);

        boolean result = ingredientService.restaurarIngrediente(1);

        assertTrue(result);
        assertNull(ingredient.getDeletedAt());
        assertTrue(ingredient.isAvailable());
        verify(ingredientRepository, times(1)).findById(1);
        verify(ingredientRepository, times(1)).save(ingredient);
    }

    @Test
    @DisplayName("CU14 - A1: Error al intentar crear ingrediente con nombre duplicado")
    void testCrearIngredienteNombreDuplicado() {
        when(ingredientRepository.findByName("Queso Mozzarella")).thenReturn(Optional.of(ingredient));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ingredientService.crearIngrediente(ingredient);
        });

        assertEquals("El nombre del ingrediente ya está registrado.", exception.getMessage());
    }
}
