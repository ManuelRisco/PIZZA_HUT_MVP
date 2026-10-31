package com.example.services;

import com.example.dtos.ProfitLossDashboardDTO;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios: FinancialDashboardService")
class FinancialDashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductLossRepository productLossRepository;
    @Mock
    private PizzaRepository pizzaRepository;
    @Mock
    private ExtraRepository extraRepository;
    @Mock
    private SizeRepository sizeRepository;

    @InjectMocks
    private FinancialDashboardService financialDashboardService;

    private Order order;
    private OrderItem orderItem;
    private ProductLoss productLoss;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1);
        order.setStatus(Order.OrderStatus.DELIVERED);
        order.setTotal(new BigDecimal("55.00"));
        order.setSubtotal(new BigDecimal("55.00"));
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());

        orderItem = new OrderItem();
        orderItem.setId(1);
        orderItem.setOrderId(1);
        orderItem.setPizzaId(10);
        orderItem.setSizeId(1);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("27.50"));
        orderItem.setSizeExtra(BigDecimal.ZERO);
        orderItem.setLineTotal(new BigDecimal("55.00"));
        orderItem.setUnitCost(new BigDecimal("10.00"));
        orderItem.setLineCost(new BigDecimal("20.00"));
        orderItem.setCreatedAt(LocalDateTime.now());

        productLoss = new ProductLoss();
        productLoss.setId(1);
        productLoss.setTotalLossCost(new BigDecimal("15.00"));
        productLoss.setQuantity(new BigDecimal("3.00"));
        productLoss.setCategory(ProductLoss.LossCategory.EXPIRED);
        productLoss.setLossDate(LocalDateTime.now());
        productLoss.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Debe calcular correctamente métricas financieras de ingresos, costos directos, mermas y margen neto")
    void testGetDashboardMetrics() {
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderItemRepository.findAll()).thenReturn(List.of(orderItem));
        when(productLossRepository.findAll()).thenReturn(List.of(productLoss));
        when(pizzaRepository.findAll()).thenReturn(Collections.emptyList());
        when(sizeRepository.findAll()).thenReturn(Collections.emptyList());
        when(extraRepository.findAll()).thenReturn(Collections.emptyList());

        ProfitLossDashboardDTO dashboard = financialDashboardService.getProfitLossReport(
                LocalDate.now().minusDays(7), LocalDate.now(), "DAY", false);

        assertNotNull(dashboard);

        // Ingresos netos: 55.00
        assertEquals(new BigDecimal("55.00"), dashboard.getNetRevenue());
        // Costo directo producción (COGS): 20.00
        assertEquals(new BigDecimal("20.00"), dashboard.getCostOfGoodsSold());
        // Ganancia bruta: 55.00 - 20.00 = 35.00
        assertEquals(new BigDecimal("35.00"), dashboard.getGrossProfit());
        // Pérdidas / mermas: 15.00
        assertEquals(new BigDecimal("15.00"), dashboard.getTotalProductLosses());
        // Ganancia neta: 35.00 - 15.00 = 20.00
        assertEquals(new BigDecimal("20.00"), dashboard.getNetProfit());
    }
}
