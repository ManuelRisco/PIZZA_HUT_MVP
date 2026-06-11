package com.example.domain.repository;

import com.example.domain.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    
    // Buscar todas las no eliminadas
    List<Promotion> findByDeletedAtIsNull();
    
    // Buscar por código
    Optional<Promotion> findByCodeAndDeletedAtIsNull(String code);
    
    // Verificar si existe código
    boolean existsByCode(String code);
    
    // Buscar promociones activas actualmente
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.startDate <= :now AND p.endDate >= :now " +
           "AND p.deletedAt IS NULL " +
           "AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);
    
    // Buscar promociones activas por tipo
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.startDate <= :now AND p.endDate >= :now " +
           "AND p.applicableTo = :applicableTo " +
           "AND p.deletedAt IS NULL " +
           "AND (p.usageLimit IS NULL OR p.usageCount < p.usageLimit)")
    List<Promotion> findActivePromotionsByType(@Param("now") LocalDateTime now, @Param("applicableTo") Promotion.ApplicableTo applicableTo);
    
    // Buscar todas las activas (configuración)
    List<Promotion> findByIsActiveTrueAndDeletedAtIsNull();
    
    // Buscar promociones por rango de fechas
    @Query("SELECT p FROM Promotion p WHERE p.deletedAt IS NULL " +
           "AND p.startDate >= :startDate AND p.endDate <= :endDate")
    List<Promotion> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Buscar promociones próximas a vencer
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.endDate BETWEEN :now AND :futureDate " +
           "AND p.deletedAt IS NULL")
    List<Promotion> findExpiringPromotions(@Param("now") LocalDateTime now, @Param("futureDate") LocalDateTime futureDate);
}
