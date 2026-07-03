package com.example.repositories;

import com.example.models.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    
    // Buscar todas las no eliminadas
    List<Promotion> findByDeletedAtIsNull();
    
    // Buscar por código (solo activas)
    Optional<Promotion> findByCodeAndDeletedAtIsNull(String code);

    // Buscar por código con bloqueo pesimista para evitar race conditions al usar promociones limitadas
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.code = :code AND p.deletedAt IS NULL")
    Optional<Promotion> findByCodeAndDeletedAtIsNullForUpdate(@Param("code") String code);
    
    // Buscar por código (incluyendo eliminadas para restaurar)
    Optional<Promotion> findByCode(String code);
    
    // Verificar si existe código activo
    boolean existsByCodeAndDeletedAtIsNull(String code);
    
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
    
    // Buscar todas las activas (configuraci\u00f3n)
    List<Promotion> findByIsActiveTrueAndDeletedAtIsNull();
    
    // Buscar promociones por rango de fechas
    @Query("SELECT p FROM Promotion p WHERE p.deletedAt IS NULL " +
           "AND p.startDate >= :startDate AND p.endDate <= :endDate")
    List<Promotion> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Buscar promociones pr\u00f3ximas a vencer
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true " +
           "AND p.endDate BETWEEN :now AND :futureDate " +
           "AND p.deletedAt IS NULL")
    List<Promotion> findExpiringPromotions(@Param("now") LocalDateTime now, @Param("futureDate") LocalDateTime futureDate);
}
