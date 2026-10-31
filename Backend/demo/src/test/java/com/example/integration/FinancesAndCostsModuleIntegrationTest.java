package com.example.integration;

import com.example.dtos.ProfitLossDashboardDTO;
import com.example.models.*;
import com.example.repositories.*;
import com.example.services.FinancialDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Test de Integración del Módulo 5: Finanzas, Costos Directos y Rentabilidad Neta")
public class FinancesAndCostsModuleIntegrationTest {

    @Autowired
    private FinancialDashboardService financialDashboardService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductLossRepository productLossRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Cálculo íntegro de rentabilidad: Ingresos brutos - Costo de insumos vendidos - Mermas = Ganancia Neta")
    void testFinancialProfitLossCalculation() {
        Usuario usuario = usuarioRepository.findByEmail("finanzas_test@pizzahut.com")
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setName("Cliente Finanzas");
                    u.setEmail("finanzas_test_" + System.currentTimeMillis() + "@pizzahut.com");
                    u.setPasswordHash("$2a$10$N.ZOmG4byf24u5U5u5u5u.abcdefghijklmnopqrstuvw");
                    u.setRole(Usuario.Role.CUSTOMER);
                    return usuarioRepository.save(u);
                });

        // 1. Crear una orden entregada con venta y costo directo
        Order order = new Order();
        order.setUserId(usuario.getId().intValue());
        order.setStatus(Order.OrderStatus.DELIVERED);
        order.setDeliveryType(Order.DeliveryType.PICKUP);
        order.setPaymentMethodId(1);
        order.setSubtotal(new BigDecimal("100.00"));
        order.setDeliveryFee(BigDecimal.ZERO);
        order.setDiscount(BigDecimal.ZERO);
        order.setTotal(new BigDecimal("100.00"));
        order.setStockSettled("DEDUCTED");
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setItemType("PIZZA");
        item.setPizzaId(1);
        item.setSizeId(1);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setSizeExtra(BigDecimal.ZERO);
        item.setLineTotal(new BigDecimal("100.00"));
        item.setUnitCost(new BigDecimal("18.00"));
        item.setLineCost(new BigDecimal("36.00")); // Costo directo = 36.00
        item.setCreatedAt(LocalDateTime.now());
        orderItemRepository.save(item);

        // 2. Registrar una merma en la misma fecha
        ProductLoss loss = new ProductLoss();
        loss.setItemType(ProductLoss.ItemType.INGREDIENT);
        loss.setIngredientId(1);
        loss.setItemName("Insumo Test Merma");
        loss.setCategory(ProductLoss.LossCategory.DAMAGED);
        loss.setQuantity(new BigDecimal("2.00"));
        loss.setUnitCost(new BigDecimal("7.00"));
        loss.setTotalLossCost(new BigDecimal("14.00")); // Merma = 14.00
        loss.setReason("Insumo derramado en mesada");
        loss.setLossDate(LocalDateTime.now());
        loss.setCreatedAt(LocalDateTime.now());
        productLossRepository.save(loss);

        // 3. Consultar métricas del panel financiero para la fecha actual
        ProfitLossDashboardDTO dashboard = financialDashboardService.getProfitLossReport(
                LocalDate.now(), LocalDate.now(), "DAY", false);

        assertNotNull(dashboard);

        // Validar que los montos de la prueba se agregaron y están presentes
        assertTrue(dashboard.getNetRevenue().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(dashboard.getCostOfGoodsSold().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(dashboard.getTotalProductLosses().compareTo(BigDecimal.ZERO) > 0);

        // La ganancia bruta debe ser exactamente Ingresos - Costos de producción
        BigDecimal grossRevenue = dashboard.getNetRevenue();
        BigDecimal productionCosts = dashboard.getCostOfGoodsSold();
        BigDecimal grossProfit = dashboard.getGrossProfit();
        assertEquals(grossRevenue.subtract(productionCosts), grossProfit);

        // La ganancia neta debe ser exactamente Ganancia Bruta - Mermas
        BigDecimal lossCosts = dashboard.getTotalProductLosses();
        BigDecimal netProfit = dashboard.getNetProfit();
        assertEquals(grossProfit.subtract(lossCosts), netProfit);
    }
}
