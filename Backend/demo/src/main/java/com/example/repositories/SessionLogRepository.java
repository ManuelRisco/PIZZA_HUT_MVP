package com.example.repositories;

import com.example.models.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {
    
    // Buscar por token de sesi\u00f3n
    Optional<SessionLog> findBySessionToken(String sessionToken);
    
    // Buscar sesiones activas de un usuario
    List<SessionLog> findByUserIdAndIsActiveTrue(Integer userId);
    
    // Buscar todas las sesiones activas
    List<SessionLog> findByIsActiveTrueOrderByLoginTimeDesc();
    
    // Buscar top 100 sesiones activas
    List<SessionLog> findTop100ByIsActiveTrueOrderByLoginTimeDesc();
    
    // Buscar sesiones de un usuario (todas)
    List<SessionLog> findByUserIdOrderByLoginTimeDesc(Integer userId);
    
    // Buscar sesiones por rango de fechas
    List<SessionLog> findByLoginTimeBetween(LocalDateTime start, LocalDateTime end);
    
    // Buscar sesiones por IP
    List<SessionLog> findByIpAddress(String ipAddress);
    
    // Buscar sesiones por IP y usuario
    List<SessionLog> findByUserIdAndIpAddress(Integer userId, String ipAddress);
    
    // Contar sesiones activas por usuario
    Long countByUserIdAndIsActiveTrue(Integer userId);
    
    // Buscar sesiones activas de larga duraci\u00f3n
    @Query("SELECT s FROM SessionLog s WHERE s.isActive = true " +
           "AND s.loginTime < :thresholdTime")
    List<SessionLog> findLongActiveSessions(@Param("thresholdTime") LocalDateTime thresholdTime);
    
    // Buscar sesiones por raz\u00f3n de cierre
    List<SessionLog> findByLogoutReason(SessionLog.LogoutReason logoutReason);
    
    // Buscar \u00faltima sesi\u00f3n de un usuario
    Optional<SessionLog> findFirstByUserIdOrderByLoginTimeDesc(Integer userId);
    
    // Contar sesiones por usuario en un per\u00edodo
    @Query("SELECT COUNT(s) FROM SessionLog s WHERE s.userId = :userId " +
           "AND s.loginTime >= :since")
    Long countSessionsByUserSince(@Param("userId") Integer userId, @Param("since") LocalDateTime since);
}
