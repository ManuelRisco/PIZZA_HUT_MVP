package com.example.domain.repository;

import com.example.domain.model.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {
    
    // Buscar por token de sesión
    Optional<SessionLog> findBySessionToken(String sessionToken);
    
    // Buscar sesiones activas de un usuario
    List<SessionLog> findByUserIdAndIsActiveTrue(Integer userId);
    
    // Buscar todas las sesiones activas
    List<SessionLog> findByIsActiveTrueOrderByLoginTimeDesc();
    
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
    
    // Buscar sesiones activas de larga duración
    @Query("SELECT s FROM SessionLog s WHERE s.isActive = true " +
           "AND s.loginTime < :thresholdTime")
    List<SessionLog> findLongActiveSessions(LocalDateTime thresholdTime);
    
    // Buscar sesiones por razón de cierre
    List<SessionLog> findByLogoutReason(SessionLog.LogoutReason logoutReason);
    
    // Buscar última sesión de un usuario
    Optional<SessionLog> findFirstByUserIdOrderByLoginTimeDesc(Integer userId);
    
    // Contar sesiones por usuario en un período
    @Query("SELECT COUNT(s) FROM SessionLog s WHERE s.userId = :userId " +
           "AND s.loginTime >= :since")
    Long countSessionsByUserSince(Integer userId, LocalDateTime since);
}
