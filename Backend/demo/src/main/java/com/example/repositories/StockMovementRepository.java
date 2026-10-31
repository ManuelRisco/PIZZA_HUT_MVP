package com.example.repositories;

import com.example.models.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {
    List<StockMovement> findTop200ByOrderByCreatedAtDesc();
    List<StockMovement> findByIngredientId(Integer ingredientId);
    List<StockMovement> findByItemTypeOrderByCreatedAtDesc(StockMovement.ItemType itemType);
    List<StockMovement> findByMovementTypeOrderByCreatedAtDesc(StockMovement.MovementType movementType);
    List<StockMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}

