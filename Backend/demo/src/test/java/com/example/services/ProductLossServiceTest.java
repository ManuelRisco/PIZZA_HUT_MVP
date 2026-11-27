package com.example.services;

import com.example.dtos.OrderLossCandidateDTO;
import com.example.dtos.OrderLossCreateDTO;
import com.example.dtos.ProductLossCreateDTO;
import com.example.dtos.ProductLossDTO;
import com.example.exceptions.BadRequestException;
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
import java.util.concurrent.atomic.AtomicInteger;

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
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderItemExtraRepository orderItemExtraRepository;
    @Mock
    private PaymentRepository paymentRepository;
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

    @Test
    @DisplayName("Debe jalar un pedido, anularlo y registrar pérdidas parciales devolviendo stock de productos no perdidos")
    void testCreateLossesFromOrderCancelsOrderAndRestoresUnselectedItems() {
        Order order = new Order();
        order.setId(42);
        order.setUserId(5);
        order.setStatus(Order.OrderStatus.PREPARING);
        order.setStockSettled("DEDUCTED");
        order.setTotal(new BigDecimal("60.00"));

        OrderItem pizzaItem = new OrderItem();
        pizzaItem.setId(101);
        pizzaItem.setOrderId(42);
        pizzaItem.setItemType("PIZZA");
        pizzaItem.setPizzaId(10);
        pizzaItem.setSizeId(2);
        pizzaItem.setQuantity(1);
        pizzaItem.setUnitCost(new BigDecimal("18.50"));
        pizzaItem.setLineCost(new BigDecimal("18.50"));

        OrderItem sodaItem = new OrderItem();
        sodaItem.setId(102);
        sodaItem.setOrderId(42);
        sodaItem.setItemType("EXTRA");
        sodaItem.setExtraId(20);
        sodaItem.setQuantity(2);
        sodaItem.setUnitCost(new BigDecimal("3.00"));
        sodaItem.setLineCost(new BigDecimal("6.00"));

        Pizza pizza = new Pizza();
        pizza.setId(10);
        pizza.setName("Pepperoni");

        Size size = new Size();
        size.setId(2);
        size.setName("Familiar");

        Extra soda = new Extra();
        soda.setId(20);
        soda.setName("Coca-Cola 1.5L");
        soda.setStockQuantity(8);
        soda.setUnitCost(new BigDecimal("3.00"));

        Payment payment = new Payment();
        payment.setId(99);
        payment.setOrderId(42);
        payment.setStatus(Payment.PaymentStatus.PAID);

        when(orderRepository.findById(42)).thenReturn(Optional.of(order));
        when(productLossRepository.findByOrderId(42)).thenReturn(List.of());
        when(orderItemRepository.findByOrderId(42)).thenReturn(List.of(pizzaItem, sodaItem));
        when(orderItemExtraRepository.findByOrderItemId(101)).thenReturn(List.of());
        when(pizzaRepository.findById(10)).thenReturn(Optional.of(pizza));
        when(sizeRepository.findById(2)).thenReturn(Optional.of(size));
        when(extraRepository.findById(20)).thenReturn(Optional.of(soda));
        when(paymentRepository.findByOrderId(42)).thenReturn(Optional.of(payment));

        AtomicInteger seq = new AtomicInteger(500);
        when(productLossRepository.save(any(ProductLoss.class))).thenAnswer(inv -> {
            ProductLoss pl = inv.getArgument(0);
            pl.setId(seq.incrementAndGet());
            return pl;
        });

        OrderLossCreateDTO req = new OrderLossCreateDTO();
        req.setOrderId(42);
        req.setCategory("CANCELLED_ORDER");
        req.setReason("Cliente canceló cuando la pizza ya estaba horneada");
        req.setRestoreUnselectedStock(true);
        // Solo la pizza fue pérdida; las 2 gaseosas selladas se devuelven al almacén
        req.setItems(List.of(new OrderLossCreateDTO.OrderLossItemDTO(101, 1)));

        List<ProductLossDTO> created = productLossService.createLossesFromOrder(req, 1);

        assertEquals(1, created.size());
        assertEquals(42, created.get(0).getOrderId());
        assertEquals("Pepperoni (Familiar)", created.get(0).getItemName());
        assertEquals(new BigDecimal("18.50"), created.get(0).getTotalLossCost());

        // Pedido anulado y pago marcado FAILED
        assertEquals(Order.OrderStatus.CANCELLED, order.getStatus());
        assertEquals("LOSS_RECORDED", order.getStockSettled());
        assertEquals(Payment.PaymentStatus.FAILED, payment.getStatus());
        verify(orderRepository).save(order);
        verify(paymentRepository).save(payment);

        // Stock de las 2 gaseosas no perdidas devuelto al almacén (8 + 2 = 10)
        assertEquals(10, soda.getStockQuantity());
        verify(extraRepository).save(soda);
        verify(stockMovementRepository).save(any(StockMovement.class));
        verify(inventoryService).evictCatalogCaches();
    }

    @Test
    @DisplayName("Debe rechazar el registro de pérdida desde pedido si ya se encuentra anulado")
    void testCreateLossesFromOrderRejectsAlreadyCancelledOrder() {
        Order cancelled = new Order();
        cancelled.setId(55);
        cancelled.setStatus(Order.OrderStatus.CANCELLED);

        when(orderRepository.findById(55)).thenReturn(Optional.of(cancelled));

        OrderLossCreateDTO req = new OrderLossCreateDTO();
        req.setOrderId(55);

        assertThrows(BadRequestException.class, () -> productLossService.createLossesFromOrder(req, 1));
    }

    @Test
    @DisplayName("Debe listar pedidos activos elegibles para jalar con sus productos y costos")
    void testListPullableOrders() {
        Order active = new Order();
        active.setId(70);
        active.setUserId(3);
        active.setStatus(Order.OrderStatus.CONFIRMED);
        active.setDeliveryType(Order.DeliveryType.DELIVERY);
        active.setStockSettled("DEDUCTED");
        active.setTotal(new BigDecimal("49.90"));

        Usuario cliente = new Usuario();
        cliente.setId(3L);
        cliente.setName("María Gómez");
        cliente.setEmail("maria@correo.com");

        OrderItem item = new OrderItem();
        item.setId(201);
        item.setOrderId(70);
        item.setItemType("EXTRA");
        item.setExtraId(5);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("5.00"));
        item.setLineTotal(new BigDecimal("10.00"));
        item.setUnitCost(new BigDecimal("2.50"));

        Extra ext = new Extra();
        ext.setId(5);
        ext.setName("Agua San Luis 625ml");

        when(orderRepository.findTop150ByStatusNotOrderByCreatedAtDesc(Order.OrderStatus.CANCELLED))
                .thenReturn(List.of(active));
        when(productLossRepository.findAll()).thenReturn(List.of());
        when(usuarioRepository.findAll()).thenReturn(List.of(cliente));
        when(pizzaRepository.findAll()).thenReturn(List.of());
        when(extraRepository.findAll()).thenReturn(List.of(ext));
        when(sizeRepository.findAll()).thenReturn(List.of());
        when(orderItemRepository.findByOrderId(70)).thenReturn(List.of(item));

        List<OrderLossCandidateDTO> result = productLossService.listPullableOrders();

        assertEquals(1, result.size());
        assertEquals(70, result.get(0).getOrderId());
        assertEquals("María Gómez", result.get(0).getCustomerName());
        assertEquals(1, result.get(0).getItems().size());
        assertEquals("Agua San Luis 625ml", result.get(0).getItems().get(0).getItemName());
        assertEquals(new BigDecimal("5.00"), result.get(0).getItems().get(0).getLineCost());
    }
}
