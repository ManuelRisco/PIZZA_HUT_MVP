package com.example.integration;

import com.example.models.*;
import com.example.repositories.*;
import com.example.services.CategoryService;
import com.example.services.IngredientService;
import com.example.services.PizzaService;
import com.example.services.SizeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Test de Integración del Módulo 2: Catálogo de Pizzas, Insumos y Recetario")
public class CatalogAndRecipeModuleIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IngredientService ingredientService;

    @Autowired
    private SizeService sizeService;

    @Autowired
    private PizzaService pizzaService;

    @Autowired
    private PizzaIngredientRepository pizzaIngredientRepository;

    @Test
    @DisplayName("Flujo completo: Crear categoría, ingredientes, tamaño con multiplicador, pizza y vincular receta técnica")
    void testCatalogAndRecipeFlow() {
        // 1. Categoría
        Category cat = new Category();
        cat.setName("Gourmet Test " + System.currentTimeMillis());
        cat.setDescription("Categoría gourmet para pruebas");
        Category catGuardada = categoryService.crearCategoria(cat);
        assertNotNull(catGuardada.getId());

        // 2. Insumos
        Ingredient salsa = new Ingredient();
        salsa.setName("Salsa Trufada " + System.currentTimeMillis());
        salsa.setUnit("g");
        salsa.setUnitCost(new BigDecimal("0.08"));
        salsa.setStockQuantity(new BigDecimal("5000.00"));
        salsa.setMinStock(new BigDecimal("500.00"));
        Ingredient salsaGuardada = ingredientService.crearIngrediente(salsa);
        assertNotNull(salsaGuardada.getId());

        // 2b. Queso
        Ingredient queso = new Ingredient();
        queso.setName("Mozzarella Búfala " + System.currentTimeMillis());
        queso.setUnit("g");
        queso.setUnitCost(new BigDecimal("0.06"));
        queso.setStockQuantity(new BigDecimal("8000.00"));
        queso.setMinStock(new BigDecimal("1000.00"));
        Ingredient quesoGuardado = ingredientService.crearIngrediente(queso);
        assertNotNull(quesoGuardado.getId());

        // 3. Tamaño con multiplicador
        Size sizeFamiliar = new Size();
        sizeFamiliar.setName("Familiar Test " + System.currentTimeMillis());
        sizeFamiliar.setExtraCost(new BigDecimal("15.00"));
        sizeFamiliar.setRecipeMultiplier(new BigDecimal("2.00"));
        sizeFamiliar.setStockQuantity(30);
        sizeFamiliar.setMinStock(5);
        sizeFamiliar.setDisplayOrder(10);
        Size sizeGuardado = sizeService.crearSize(sizeFamiliar);
        assertNotNull(sizeGuardado.getId());
        assertEquals(new BigDecimal("2.00"), sizeGuardado.getRecipeMultiplier());

        // 4. Pizza
        Pizza pizza = new Pizza();
        pizza.setName("Pizza Búfala Deluxe " + System.currentTimeMillis());
        pizza.setDescription("Deliciosa pizza preparada con mozzarella de búfala y salsa trufada");
        pizza.setPrice(new BigDecimal("45.00"));
        pizza.setCategory(catGuardada);
        pizza.setIsAvailable(true);
        pizza.setIsPopular(true);
        Pizza pizzaGuardada = pizzaService.crearPizza(pizza);
        assertNotNull(pizzaGuardada.getId());

        // 5. Vincular receta técnica (PizzaIngredient)
        PizzaIngredient pi1 = new PizzaIngredient();
        pi1.setId(new PizzaIngredientId(pizzaGuardada.getId(), salsaGuardada.getId()));
        pi1.setPizza(pizzaGuardada);
        pi1.setIngredient(salsaGuardada);
        pi1.setQuantity(new BigDecimal("80.00"));
        pizzaIngredientRepository.save(pi1);

        PizzaIngredient pi2 = new PizzaIngredient();
        pi2.setId(new PizzaIngredientId(pizzaGuardada.getId(), quesoGuardado.getId()));
        pi2.setPizza(pizzaGuardada);
        pi2.setIngredient(quesoGuardado);
        pi2.setQuantity(new BigDecimal("150.00"));
        pizzaIngredientRepository.save(pi2);

        // 6. Validar que la receta fue registrada
        List<PizzaIngredient> receta = pizzaIngredientRepository.findByPizzaId(pizzaGuardada.getId());
        assertEquals(2, receta.size());

        // Costo directo unitario base: (80g * 0.08) + (150g * 0.06) = 6.40 + 9.00 = 15.40
        assertTrue(pizzaGuardada.getPrice().compareTo(new BigDecimal("15.40")) > 0);
    }
}
