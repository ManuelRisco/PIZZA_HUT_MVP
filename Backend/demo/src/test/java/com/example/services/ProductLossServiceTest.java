package com.example.services;

import com.example.dtos.ProductLossCreateDTO;
import com.example.dtos.ProductLossDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios: ProductLossService")
class ProductLossServiceTest {

    @Mock
    private ProductLossRepository productLossRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private ExtraRepository extraRepository;
    @Mock
    private PizzaRepository pizzaRepository;
    @Mock
    private PizzaIngredientRepository pizzaIngredientRepository;
    @Mock
    private SizeRepository sizeRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private ProductLossService productLossService;

    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        ingredient = new Ingredient();
        ingredient.setId(1);
        ingredient.setName("Tomate");
        ingredient.setUnit("g");
        ingredient.setStockQuantity(new BigDecimal("500.00"));
        ingredient.setUnitCost(new BigDecimal("0.02"));
    }

    @Test
    @DisplayName("Debe registrar merma de un ingrediente y descontar stock registrando movimiento LOSS")
    void testRegistrarMermaIngrediente() {
        when(ingredientRepository.findById(1)).thenReturn(Optional.of(ingredient));
        when(productLossRepository.save(any(ProductLoss.class))).thenAnswer(invocation -> {
            ProductLoss pl = invocation.getArgument(0);
            pl.setId(100);
            return pl;
        });

        ProductLossCreateDTO dto = new ProductLossCreateDTO();
        dto.setItemType("INGREDIENT");
        dto.setIngredientId(1);
        dto.setQuantity(new BigDecimal("100.00"));
        dto.setCategory("EXPIRED");
        dto.setReason("Lote vencido");

        ProductLossDTO result = productLossService.createLoss(dto, 1);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals(new BigDecimal("100.00"), result.getQuantity());
        // Costo = 100g * 0.02 = 2.00
        assertEquals(new BigDecimal("2.00"), result.getTotalLossCost());

        // Verificar que el stock fue descontado: 500 - 100 = 400
        assertEquals(new BigDecimal("400.00"), ingredient.getStockQuantity());
        verify(ingredientRepository).save(ingredient);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Debe calcular resumen de pérdidas por categoría y costos totales")
    void testGetLossSummary() {
        ProductLoss pl1 = new ProductLoss();
        pl1.setId(1);
        pl1.setTotalLossCost(new BigDecimal("10.00"));
        pl1.setQuantity(new BigDecimal("2.00"));
        pl1.setCategory(ProductLoss.LossCategory.DAMAGED);
        pl1.setLossDate(LocalDateTime.now());

        ProductLoss pl2 = new ProductLoss();
        pl2.setId(2);
        pl2.setTotalLossCost(new BigDecimal("15.00"));
        pl2.setQuantity(new BigDecimal("3.00"));
        pl2.setCategory(ProductLoss.LossCategory.EXPIRED);
        pl2.setLossDate(LocalDateTime.now());

        when(productLossRepository.findByLossDateBetweenOrderByLossDateDesc(any(), any())).thenReturn(List.of(pl1, pl2));

        Map<String, Object> summary = productLossService.getLossesSummary(LocalDate.now().minusDays(1), LocalDate.now());

        assertNotNull(summary);
        assertEquals(new BigDecimal("25.00"), summary.get("totalLossCost"));
        assertEquals(2, summary.get("totalCount"));
    }
}
