package com.example.services;

import com.example.models.Pizza;
import com.example.models.Category;
import com.example.repositories.PizzaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CU05/CU12: Gestión de Pizzas y Menú - Pruebas TDD")
class PizzaServiceTest {

    @Mock
    private PizzaRepository pizzaRepository;

    @Mock
    private com.example.repositories.CategoryRepository categoryRepository;

    @InjectMocks
    private PizzaService pizzaService;

    private Pizza pizzaTest;
    private Category categoryTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        categoryTest = new Category();
        categoryTest.setId(1);
        categoryTest.setName("Clasica");

        pizzaTest = new Pizza();
        pizzaTest.setId(1);
        pizzaTest.setName("Margherita");
        pizzaTest.setDescription("Pizza clasica");
        pizzaTest.setPrice(new BigDecimal("12.99"));
        pizzaTest.setIsAvailable(true);
        pizzaTest.setCategory(categoryTest);
    }

    // ==========================================
    // CU05: Ver menú de pizzas
    // ==========================================

    @Test
    @DisplayName("CU05 - Flujo Principal: Listar todas las pizzas [RF09, RF10]")
    void testListarTodasLasPizzas() {
        List<Pizza> pizzas = new ArrayList<>();
        pizzas.add(pizzaTest);

        when(pizzaRepository.findAll()).thenReturn(pizzas);
        List<Pizza> pizzasObtenidas = pizzaService.listarPizzas();

        assertEquals(1, pizzasObtenidas.size());
        assertEquals("Margherita", pizzasObtenidas.get(0).getName());
        verify(pizzaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("CU05 - A2: Búsqueda sin resultados (lista vacía)")
    void testBuscarPizzasSinResultados() {
        when(pizzaRepository.findAll()).thenReturn(new ArrayList<>());
        List<Pizza> pizzasObtenidas = pizzaService.listarPizzas();

        assertTrue(pizzasObtenidas.isEmpty());
        verify(pizzaRepository, times(1)).findAll();
    }

    // ==========================================
    // CU12: Gestionar Pizzas
    // ==========================================

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU12 - Flujo Principal: Crear una pizza exitosamente [RF23]")
    void testCrearPizzaExitosa() {
        when(categoryRepository.existsById(1)).thenReturn(true);
        when(pizzaRepository.save(pizzaTest)).thenReturn(pizzaTest);

        Pizza pizzaCreada = pizzaService.crearPizza(pizzaTest);

        assertNotNull(pizzaCreada);
        assertEquals("Margherita", pizzaCreada.getName());
        assertEquals(new BigDecimal("12.99"), pizzaCreada.getPrice());
        verify(pizzaRepository, times(1)).save(pizzaTest);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU12 - Flujo Principal: Editar una pizza existente [RF24]")
    void testActualizarPizzaExistente() {
        when(categoryRepository.existsById(1)).thenReturn(true);
        when(pizzaRepository.findById(1)).thenReturn(Optional.of(pizzaTest));

        Pizza pizzaActualizada = new Pizza();
        pizzaActualizada.setName("Margherita Premium");
        pizzaActualizada.setPrice(new BigDecimal("15.99"));
        pizzaActualizada.setIsAvailable(true);
        pizzaActualizada.setCategory(categoryTest);

        when(pizzaRepository.save(any(Pizza.class))).thenReturn(pizzaActualizada);

        Pizza resultado = pizzaService.actualizarPizza(1, pizzaActualizada);

        assertNotNull(resultado);
        assertEquals("Margherita Premium", resultado.getName());
        verify(pizzaRepository, times(1)).findById(1);
        verify(pizzaRepository, times(1)).save(any(Pizza.class));
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU12 - Flujo Principal: Eliminar pizza (Soft Delete) [RF24]")
    void testEliminarPizzaExistente() {
        when(pizzaRepository.findById(1)).thenReturn(Optional.of(pizzaTest));
        when(pizzaRepository.save(any(Pizza.class))).thenReturn(pizzaTest);

        boolean eliminada = pizzaService.eliminarPizza(1);

        assertTrue(eliminada);
        assertNotNull(pizzaTest.getDeletedAt());
        verify(pizzaRepository, times(1)).findById(1);
        verify(pizzaRepository, times(1)).save(pizzaTest);
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU12 - A1: Error al guardar con datos incompletos")
    void testCrearPizzaDatosIncompletos() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            pizzaService.crearPizza(new Pizza());
        });

        assertTrue(exception.getMessage().contains("requeridos"));
        verify(pizzaRepository, never()).save(any(Pizza.class));
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("CU12 - A3: Cancelar eliminación / Eliminar pizza que no existe")
    void testEliminarPizzaNoExistente() {
        when(pizzaRepository.findById(999)).thenReturn(Optional.empty());

        boolean eliminada = pizzaService.eliminarPizza(999);

        assertFalse(eliminada);
        verify(pizzaRepository, times(1)).findById(999);
        verify(pizzaRepository, never()).save(any());
    }
}
