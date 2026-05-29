package com.example.domain.repository;

import com.example.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Buscar logs por usuario
    List<AuditLog> findByUserId(Integer userId);
    
    // Buscar logs por usuario en un rango de fechas
    List<AuditLog> findByUserIdAndCreatedAtBetween(Integer userId, LocalDateTime start, LocalDateTime end);
    
    // Buscar por tipo de acción
    List<AuditLog> findByActionType(AuditLog.ActionType actionType);
    
    // Buscar por tipo de acción y usuario
    List<AuditLog> findByActionTypeAndUserId(AuditLog.ActionType actionType, Integer userId);
    
    // Buscar por entidad
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Integer entityId);
    
    // Buscar por estado
    List<AuditLog> findByStatus(AuditLog.Status status);
    
    // Buscar intentos de login fallidos
    List<AuditLog> findByActionTypeAndStatusOrderByCreatedAtDesc(
        AuditLog.ActionType actionType, 
        AuditLog.Status status
    );
    
    // Buscar logs recientes (últimas 24 horas)
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLogs(LocalDateTime since);
    
    // Buscar logs por IP
    List<AuditLog> findByIpAddressAndCreatedAtBetween(String ipAddress, LocalDateTime start, LocalDateTime end);
    
    // Contar intentos fallidos por usuario en un período
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId " +
           "AND a.actionType = 'FAILED_LOGIN' " +
           "AND a.createdAt >= :since")
    Long countFailedLoginAttempts(Integer userId, LocalDateTime since);
    
    // Obtener logs ordenados por fecha (paginado)
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}
