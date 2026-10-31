package com.example.services;

import com.example.dtos.CheckoutRequestDTO;
import com.example.dtos.StockMovementDTO;
import com.example.dtos.StockOperationRequestDTO;
import com.example.models.*;
import com.example.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias para InventoryService y Stock Compartido de Masas")
public class InventoryServiceTest {

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
    private StockMovementRepository stockMovementRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderItemExtraRepository orderItemExtraRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private InventoryService inventoryService;

    private Size sizeMediana;
    private Pizza pizzaPepperoni;

    @BeforeEach
    void setUp() {
        sizeMediana = new Size();
        sizeMediana.setId(2);
        sizeMediana.setName("Mediana");
        sizeMediana.setStockQuantity(40);
        sizeMediana.setMinStock(10);
        sizeMediana.setRecipeMultiplier(BigDecimal.ONE);

        pizzaPepperoni = new Pizza();
        pizzaPepperoni.setId(1);
        pizzaPepperoni.setName("Pepperoni");
        pizzaPepperoni.setIsAvailable(true);
    }

    @Test
    @DisplayName("Descontar stock de masa compartida y registrar movimiento SIZE SALE en Kardex")
    void testDeductCheckoutStockDoughSize() {
        when(pizzaRepository.findById(1)).thenReturn(Optional.of(pizzaPepperoni));
        when(sizeRepository.findById(2)).thenReturn(Optional.of(sizeMediana));
        when(pizzaIngredientRepository.findByPizzaId(1)).thenReturn(Collections.emptyList());
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> i.getArgument(0));

        CheckoutRequestDTO.CheckoutItemDTO item = new CheckoutRequestDTO.CheckoutItemDTO();
        item.setItemType("PIZZA");
        item.setPizzaId(1);
        item.setSizeId(2);
        item.setQuantity(3);

        List<BigDecimal> costs = inventoryService.deductCheckoutStockAndCalculateUnitCosts(
                List.of(item), 100, 1);

        assertNotNull(costs);
        assertEquals(1, costs.size());

        // Verificar que el stock de masa compartida se redujo de 40 a 37
        assertEquals(37, sizeMediana.getStockQuantity());
        verify(sizeRepository, times(1)).save(sizeMediana);

        // Capturar los movimientos registrados en el Kardex
        ArgumentCaptor<StockMovement> smCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, atLeastOnce()).save(smCaptor.capture());

        List<StockMovement> savedMovements = smCaptor.getAllValues();
        boolean hasSizeSale = savedMovements.stream().anyMatch(sm -> sm.getItemType() == StockMovement.ItemType.SIZE &&
                Integer.valueOf(2).equals(sm.getSizeId()) &&
                sm.getMovementType() == StockMovement.MovementType.SALE &&
                new BigDecimal("-3").compareTo(sm.getQuantity()) == 0);

        assertTrue(hasSizeSale, "Debe registrarse un movimiento SIZE de tipo SALE con cantidad -3");
    }

    @Test
    @DisplayName("Lanzar error cuando no hay suficiente stock de masa para el tamaño solicitado")
    void testInsufficientDoughStockThrowsException() {
        sizeMediana.setStockQuantity(2); // Solo 2 masas disponibles
        when(pizzaRepository.findById(1)).thenReturn(Optional.of(pizzaPepperoni));
        when(sizeRepository.findById(2)).thenReturn(Optional.of(sizeMediana));

        CheckoutRequestDTO.CheckoutItemDTO item = new CheckoutRequestDTO.CheckoutItemDTO();
        item.setItemType("PIZZA");
        item.setPizzaId(1);
        item.setSizeId(2);
        item.setQuantity(5); // Solicita 5 pizzas

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> inventoryService.deductCheckoutStockAndCalculateUnitCosts(List.of(item), 100, 1));

        assertTrue(ex.getMessage().contains("Stock insuficiente de masa"));
        assertEquals(2, sizeMediana.getStockQuantity()); // No debe modificarse
    }

    @Test
    @DisplayName("Restaurar stock de masa compartida al cancelar pedido antes de preparación")
    void testReturnOrderStockRestoresDough() {
        Order order = new Order();
        order.setId(200);
        order.setStockSettled("DEDUCTED");

        OrderItem oi = new OrderItem();
        oi.setId(500);
        oi.setOrderId(200);
        oi.setItemType("PIZZA");
        oi.setPizzaId(1);
        oi.setSizeId(2);
        oi.setQuantity(4);

        when(orderItemRepository.findByOrderId(200)).thenReturn(List.of(oi));
        when(sizeRepository.findById(2)).thenReturn(Optional.of(sizeMediana));
        when(pizzaIngredientRepository.findByPizzaId(1)).thenReturn(Collections.emptyList());
        when(orderItemExtraRepository.findByOrderItemId(500)).thenReturn(Collections.emptyList());

        inventoryService.returnOrderStock(order, 1);

        // Se deben devolver las 4 masas: 40 + 4 = 44
        assertEquals(44, sizeMediana.getStockQuantity());
        verify(sizeRepository, times(1)).save(sizeMediana);

        ArgumentCaptor<StockMovement> smCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository, atLeastOnce()).save(smCaptor.capture());

        boolean hasSizeReturn = smCaptor.getAllValues().stream()
                .anyMatch(sm -> sm.getItemType() == StockMovement.ItemType.SIZE &&
                        Integer.valueOf(2).equals(sm.getSizeId()) &&
                        sm.getMovementType() == StockMovement.MovementType.RETURN &&
                        new BigDecimal("4").compareTo(sm.getQuantity()) == 0);

        assertTrue(hasSizeReturn, "Debe registrarse un movimiento SIZE de tipo RETURN con cantidad +4");
    }

    @Test
    @DisplayName("Reabastecer stock de masa (SIZE) manualmente")
    void testRestockDoughSize() {
        when(sizeRepository.findById(2)).thenReturn(Optional.of(sizeMediana));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> i.getArgument(0));

        StockOperationRequestDTO req = new StockOperationRequestDTO();
        req.setItemType("SIZE");
        req.setItemId(2);
        req.setQuantity(new BigDecimal("15"));
        req.setReason("Llegada de masas frescas");

        StockMovementDTO result = inventoryService.restock(req, 1);

        assertNotNull(result);
        assertEquals(55, sizeMediana.getStockQuantity()); // 40 + 15
        verify(sizeRepository, times(1)).save(sizeMediana);
    }
}
