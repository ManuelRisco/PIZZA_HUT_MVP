package com.example.service;

import com.example.domain.model.Pizza;
import com.example.domain.model.Category;
import com.example.domain.repository.PizzaRepository;
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

@DisplayName("UC1: Pruebas de Gestión de Pizzas - TDD")
class PizzaServiceTest {

    @Mock
    private PizzaRepository pizzaRepository;

    @InjectMocks
    private PizzaService pizzaService;

    private Pizza pizzaTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        pizzaTest = new Pizza();
        pizzaTest.setId(1);
        pizzaTest.setName("Margherita");
        pizzaTest.setDescription("Pizza clásica con queso y tomate");
        pizzaTest.setPrice(new BigDecimal("12.99"));
        pizzaTest.setIsAvailable(true);
        Category category = new Category();
        category.setId(1);
        category.setName("Clásica");
        pizzaTest.setCategory(category);
    }

    @Test
    @DisplayName("UC1.1: Crear una pizza exitosamente")
    void testCrearPizzaExitosa() {
        when(pizzaRepository.save(pizzaTest)).thenReturn(pizzaTest);
        Pizza pizzaCreada = pizzaService.crearPizza(pizzaTest);
        assertNotNull(pizzaCreada);
        assertEquals("Margherita", pizzaCreada.getName());
        assertEquals(new BigDecimal("12.99"), pizzaCreada.getPrice());
        verify(pizzaRepository, times(1)).save(pizzaTest);
    }

    @Test
    @DisplayName("UC1.2: Obtener pizza por ID")
    void testObtenerPizzaPorId() {
        when(pizzaRepository.findById(1)).thenReturn(Optional.of(pizzaTest));
        Optional<Pizza> pizzaEncontrada = pizzaService.obtenerPorId(1);
        assertTrue(pizzaEncontrada.isPresent());
        assertEquals("Margherita", pizzaEncontrada.get().getName());
        verify(pizzaRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("UC1.3: Listar todas las pizzas")
    void testListarTodasLasPizzas() {
        List<Pizza> pizzas = new ArrayList<>();
        pizzas.add(pizzaTest);
        Pizza pizza2 = new Pizza();
        pizza2.setId(2);
        pizza2.setName("Pepperoni");
        pizza2.setPrice(new BigDecimal("14.99"));
        pizzas.add(pizza2);

        when(pizzaRepository.findAll()).thenReturn(pizzas);
        List<Pizza> pizzasObtenidas = pizzaService.listarPizzas();
        assertEquals(2, pizzasObtenidas.size());
        assertEquals("Margherita", pizzasObtenidas.get(0).getName());
        verify(pizzaRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("UC1.4: Actualizar pizza existente")
    void testActualizarPizzaExistente() {
        when(pizzaRepository.findById(1)).thenReturn(Optional.of(pizzaTest));
        Pizza pizzaActualizada = new Pizza();
        pizzaActualizada.setName("Margherita Premium");
        pizzaActualizada.setPrice(new BigDecimal("15.99"));
        pizzaActualizada.setIsAvailable(true);

        when(pizzaRepository.save(any(Pizza.class))).thenReturn(pizzaActualizada);
        Pizza resultado = pizzaService.actualizarPizza(1, pizzaActualizada);
        assertNotNull(resultado);
        assertEquals("Margherita Premium", resultado.getName());
        verify(pizzaRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("UC1.5: Eliminar pizza existente")
    void testEliminarPizzaExistente() {
        when(pizzaRepository.existsById(1)).thenReturn(true);
        doNothing().when(pizzaRepository).deleteById(1);
        boolean eliminada = pizzaService.eliminarPizza(1);
        assertTrue(eliminada);
        verify(pizzaRepository, times(1)).existsById(1);
    }

    @Test
    @DisplayName("UC1.6: Validar que pizza no existe antes de eliminar")
    void testEliminarPizzaNoExistente() {
        when(pizzaRepository.existsById(999)).thenReturn(false);
        boolean eliminada = pizzaService.eliminarPizza(999);
        assertFalse(eliminada);
        verify(pizzaRepository, times(1)).existsById(999);
    }
}
