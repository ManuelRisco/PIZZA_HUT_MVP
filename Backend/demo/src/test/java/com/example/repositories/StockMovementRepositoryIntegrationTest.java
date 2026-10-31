package com.example.repositories;

import com.example.models.StockMovement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Test de Integración en Base de Datos: StockMovement con ItemType.SIZE")
public class StockMovementRepositoryIntegrationTest {

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    @DisplayName("Insertar y recuperar un movimiento SIZE en stock_movements sin truncamiento de columna")
    void testInsertAndRetrieveSizeStockMovement() {
        StockMovement sm = new StockMovement();
        sm.setItemType(StockMovement.ItemType.SIZE);
        sm.setSizeId(1);
        sm.setMovementType(StockMovement.MovementType.SALE);
        sm.setQuantity(new BigDecimal("-2.00"));
        sm.setPreviousStock(new BigDecimal("50.00"));
        sm.setNewStock(new BigDecimal("48.00"));
        sm.setUnitCost(BigDecimal.ZERO);
        sm.setTotalCost(BigDecimal.ZERO);
        sm.setReferenceType("ORDER");
        sm.setReferenceId(9999);
        sm.setReason("Prueba de integración: consumo de masa SIZE");
        sm.setCreatedAt(LocalDateTime.now());

        StockMovement saved = stockMovementRepository.save(sm);

        assertNotNull(saved.getId(), "El ID del movimiento debe ser generado por la BD");
        assertEquals(StockMovement.ItemType.SIZE, saved.getItemType());
        assertEquals(1, saved.getSizeId());
        assertEquals(StockMovement.MovementType.SALE, saved.getMovementType());
        assertEquals(new BigDecimal("-2.00"), saved.getQuantity());

        StockMovement retrieved = stockMovementRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(StockMovement.ItemType.SIZE, retrieved.getItemType());
    }
}

