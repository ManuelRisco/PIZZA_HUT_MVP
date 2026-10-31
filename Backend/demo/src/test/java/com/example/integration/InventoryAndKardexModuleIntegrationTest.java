package com.example.integration;

import com.example.dtos.ProductLossCreateDTO;
import com.example.dtos.ProductLossDTO;
import com.example.dtos.StockMovementDTO;
import com.example.dtos.StockOperationRequestDTO;
import com.example.models.Ingredient;
import com.example.models.StockMovement;
import com.example.repositories.IngredientRepository;
import com.example.repositories.StockMovementRepository;
import com.example.services.InventoryService;
import com.example.services.ProductLossService;
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
@DisplayName("Test de Integración del Módulo 3: Inventario, Kardex Valorizado y Mermas")
public class InventoryAndKardexModuleIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductLossService productLossService;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    @DisplayName("Flujo completo: Reabastecimiento de stock, ajuste manual, registro de mermas y auditoría en Kardex")
    void testInventoryAndKardexFlow() {
        // 1. Crear insumo de prueba
        Ingredient harina = new Ingredient();
        harina.setName("Harina Especial Test " + System.currentTimeMillis());
        harina.setUnit("g");
        harina.setUnitCost(new BigDecimal("0.01"));
        harina.setStockQuantity(new BigDecimal("1000.00"));
        harina.setMinStock(new BigDecimal("500.00"));
        harina = ingredientRepository.save(harina);
        Integer ingId = harina.getId();

        // 2. Operación de Reabastecimiento (PURCHASE)
        StockOperationRequestDTO compraReq = new StockOperationRequestDTO();
        compraReq.setItemType("INGREDIENT");
        compraReq.setItemId(ingId);
        compraReq.setQuantity(new BigDecimal("2000.00"));
        compraReq.setReason("Compra de insumo con factura F001-123");

        StockMovementDTO movCompra = inventoryService.restock(compraReq, 1);
        assertNotNull(movCompra.getId());
        assertEquals("PURCHASE", movCompra.getMovementType());

        // Verificar que el stock aumentó a 3000g
        Ingredient postCompra = ingredientRepository.findById(ingId).orElseThrow();
        assertEquals(new BigDecimal("3000.00"), postCompra.getStockQuantity());

        // 3. Ajuste Manual de Inventario (ADJUSTMENT)
        StockOperationRequestDTO ajusteReq = new StockOperationRequestDTO();
        ajusteReq.setItemType("INGREDIENT");
        ajusteReq.setItemId(ingId);
        ajusteReq.setQuantity(new BigDecimal("2800.00")); // Nuevo stock físico real
        ajusteReq.setReason("Ajuste por conteo físico mensual");

        StockMovementDTO movAjuste = inventoryService.adjustStock(ajusteReq, 1);
        assertNotNull(movAjuste.getId());
        assertEquals("ADJUSTMENT", movAjuste.getMovementType());

        Ingredient postAjuste = ingredientRepository.findById(ingId).orElseThrow();
        assertEquals(new BigDecimal("2800.00"), postAjuste.getStockQuantity());

        // 4. Registro de Merma (LOSS)
        ProductLossCreateDTO lossDTO = new ProductLossCreateDTO();
        lossDTO.setItemType("INGREDIENT");
        lossDTO.setIngredientId(ingId);
        lossDTO.setQuantity(new BigDecimal("300.00"));
        lossDTO.setCategory("EXPIRED");
        lossDTO.setReason("Humedad en almacén");

        ProductLossDTO lossResult = productLossService.createLoss(lossDTO, 1);
        assertNotNull(lossResult.getId());
        assertEquals(new BigDecimal("300.00"), lossResult.getQuantity());

        // Stock final: 2800 - 300 = 2500g
        Ingredient postLoss = ingredientRepository.findById(ingId).orElseThrow();
        assertEquals(new BigDecimal("2500.00"), postLoss.getStockQuantity());

        // 5. Verificar que el Kardex contiene todos los movimientos registrados para este insumo
        List<StockMovement> movimientos = stockMovementRepository.findByIngredientId(ingId);
        assertTrue(movimientos.size() >= 3, "El Kardex debe registrar compra, ajuste y merma");
    }
}
