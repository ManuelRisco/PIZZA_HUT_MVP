package com.example.services;

import com.example.dtos.ProductionCatalogCostDTO;
import com.example.dtos.ProductionEstimateRequestDTO;
import com.example.dtos.ProductionEstimateResponseDTO;
import com.example.models.*;
import com.example.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios: ProductionCostService")
class ProductionCostServiceTest {

    @Mock
    private PizzaRepository pizzaRepository;
    @Mock
    private PizzaIngredientRepository pizzaIngredientRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private SizeRepository sizeRepository;
    @Mock
    private ExtraRepository extraRepository;

    @InjectMocks
    private ProductionCostService productionCostService;

    private Pizza pizza;
    private Size size;
    private Ingredient ingredient;
    private PizzaIngredient pizzaIngredient;

    @BeforeEach
    void setUp() {
        pizza = new Pizza();
        pizza.setId(1);
        pizza.setName("Americana");
        pizza.setPrice(new BigDecimal("30.00"));
        pizza.setIsAvailable(true);

        size = new Size();
        size.setId(1);
        size.setName("Personal");
        size.setRecipeMultiplier(new BigDecimal("1.00"));
        size.setExtraCost(BigDecimal.ZERO);
        size.setStockQuantity(50);

        ingredient = new Ingredient();
        ingredient.setId(1);
        ingredient.setName("Queso");
        ingredient.setUnit("g");
        ingredient.setUnitCost(new BigDecimal("0.05"));
        ingredient.setStockQuantity(new BigDecimal("1000.00"));
        ingredient.setAvailable(true);

        pizzaIngredient = new PizzaIngredient();
        pizzaIngredient.setId(new PizzaIngredientId(1, 1));
        pizzaIngredient.setPizza(pizza);
        pizzaIngredient.setIngredient(ingredient);
        pizzaIngredient.setQuantity(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Debe listar costos de catálogo para pizzas y extras calculando margen y costo directo")
    void testGetCatalogCosts() {
        when(pizzaRepository.findByDeletedAtIsNull()).thenReturn(List.of(pizza));
        when(sizeRepository.findByDeletedAtIsNull()).thenReturn(List.of(size));
        when(pizzaIngredientRepository.findAll()).thenReturn(List.of(pizzaIngredient));
        when(ingredientRepository.findByDeletedAtIsNull()).thenReturn(List.of(ingredient));
        when(extraRepository.findByDeletedAtIsNullOrderByDisplayOrderAsc()).thenReturn(Collections.emptyList());

        ProductionCatalogCostDTO catalogCosts = productionCostService.getCatalogCosts();

        assertNotNull(catalogCosts);
        assertFalse(catalogCosts.getPizzaCosts().isEmpty());
        // Costo directo unitario = 100g * 0.05 = 5.00
        assertEquals(new BigDecimal("5.00"), catalogCosts.getPizzaCosts().get(0).getDirectCost());
        // Precio venta = 30.00, Ganancia = 25.00
        assertEquals(new BigDecimal("25.00"), catalogCosts.getPizzaCosts().get(0).getUnitProfit());
    }

    @Test
    @DisplayName("Debe estimar requerimientos y costos de un lote de producción")
    void testEstimateBatch() {
        when(pizzaRepository.findById(1)).thenReturn(Optional.of(pizza));
        when(sizeRepository.findById(1)).thenReturn(Optional.of(size));
        when(pizzaIngredientRepository.findByPizzaId(1)).thenReturn(List.of(pizzaIngredient));
        when(ingredientRepository.findAll()).thenReturn(List.of(ingredient));

        ProductionEstimateRequestDTO request = new ProductionEstimateRequestDTO();
        ProductionEstimateRequestDTO.BatchItemRequest item = new ProductionEstimateRequestDTO.BatchItemRequest();
        item.setItemType("PIZZA");
        item.setPizzaId(1);
        item.setSizeId(1);
        item.setQuantity(4);
        request.setItems(List.of(item));

        ProductionEstimateResponseDTO response = productionCostService.estimateBatch(request);

        assertNotNull(response);
        // Costo unitario 5.00 * 4 = 20.00
        assertEquals(new BigDecimal("20.00"), response.getTotalProductionCost());
        // Se estiman 2 materiales: 400g de Queso y 4 masas de tamaño Personal
        assertEquals(2, response.getMaterials().size());
        ProductionEstimateResponseDTO.MaterialRequirementDTO ingMat = response.getMaterials().stream()
                .filter(m -> "INGREDIENT".equals(m.getItemType()))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("400.00"), ingMat.getRequiredQuantity());
    }
}
