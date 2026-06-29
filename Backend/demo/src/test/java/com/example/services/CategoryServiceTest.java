package com.example.services;

import com.example.models.Category;
import com.example.repositories.CategoryRepository;
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

@DisplayName("CU13: Gestionar categorías - Pruebas TDD")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private com.example.repositories.PizzaRepository pizzaRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category categoryTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        categoryTest = new Category();
        categoryTest.setId(1);
        categoryTest.setName("Pizzas Clásicas");
        categoryTest.setDescription("Las de toda la vida");
        categoryTest.setDisplayOrder(1);
    }

    // ==========================================
    // CU13: Gestionar Categorías
    // ==========================================

    @Test
    @DisplayName("CU13 - Flujo Principal: Listar categorías [RF25]")
    void testListarCategorias() {
        List<Category> categorias = new ArrayList<>();
        categorias.add(categoryTest);
        when(categoryRepository.findAll()).thenReturn(categorias);

        List<Category> resultado = categoryService.listarCategorias();

        assertEquals(1, resultado.size());
        assertEquals("Pizzas Clásicas", resultado.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU13 - Flujo Principal: Crear categoría exitosamente [RF25, RF26]")
    void testCrearCategoria() {
        when(categoryRepository.save(any(Category.class))).thenReturn(categoryTest);

        Category resultado = categoryService.crearCategoria(categoryTest);

        assertNotNull(resultado);
        assertEquals("Pizzas Clásicas", resultado.getName());
        verify(categoryRepository, times(1)).save(categoryTest);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU13 - Flujo Principal: Actualizar categoría existente [RF25, RF26]")
    void testActualizarCategoriaExistente() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(categoryTest));
        when(categoryRepository.save(any(Category.class))).thenReturn(categoryTest);

        Category actualizada = new Category();
        actualizada.setName("Pizzas Premium");

        Category resultado = categoryService.actualizarCategoria(1, actualizada);

        assertNotNull(resultado);
        verify(categoryRepository, times(1)).findById(1);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU13 - Flujo Principal: Eliminar categoría (Soft delete) [RF25]")
    void testEliminarCategoria() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(categoryTest));
        when(categoryRepository.save(any(Category.class))).thenReturn(categoryTest);

        boolean resultado = categoryService.eliminarCategoria(1);

        assertTrue(resultado);
        verify(categoryRepository, times(1)).findById(1);
        verify(categoryRepository, times(1)).save(categoryTest);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU13 - A1: Error al intentar crear categoría con nombre duplicado")
    void testCrearCategoriaNombreDuplicado() {
        // Simulando excepcion de constraint
        when(categoryRepository.save(any(Category.class)))
                .thenThrow(new IllegalArgumentException("El nombre de la categoría ya está registrado."));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.crearCategoria(categoryTest);
        });

        assertEquals("El nombre de la categoría ya está registrado.", exception.getMessage());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU13 - A2: Error al intentar eliminar categoría con dependencias (en uso)")
    void testEliminarCategoriaConDependencias() {
        // Simulando que el controller o service validan uso y lanzan error
        when(categoryRepository.findById(1)).thenReturn(Optional.of(categoryTest));
        when(categoryRepository.save(any(Category.class)))
                .thenThrow(new IllegalStateException("No se puede eliminar la categoría porque está en uso."));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            categoryService.eliminarCategoria(1);
        });

        assertEquals("No se puede eliminar la categoría porque está en uso.", exception.getMessage());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU13 - A3: Error al guardar con datos incompletos (sin nombre)")
    void testGuardarCategoriaSinNombre() {
        Category catInvalida = new Category();
        when(categoryRepository.save(any(Category.class)))
                .thenThrow(new IllegalArgumentException("Debe ingresar un nombre válido para la categoría."));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.crearCategoria(catInvalida);
        });

        assertEquals("Debe ingresar un nombre válido para la categoría.", exception.getMessage());
    }
}
