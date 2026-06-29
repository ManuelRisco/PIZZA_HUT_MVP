package com.example.services;

import com.example.models.AuditLog;
import com.example.repositories.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog registrar(AuditLog auditLog) {
        if (auditLog == null) {
            throw new IllegalArgumentException("AuditLog no puede ser null");
        }
        return auditLogRepository.save(auditLog);
    }

    public List<AuditLog> listarTodos() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> listarRecientes() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public List<AuditLog> listarRecientes(int horas) {
        LocalDateTime since = LocalDateTime.now().minusHours(horas);
        return auditLogRepository.findRecentLogs(since);
    }

    public List<AuditLog> listarPorUsuario(Integer userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> listarPorUsuarioEnRango(Integer userId, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
    }

    public List<AuditLog> listarPorTipoAccion(AuditLog.ActionType actionType) {
        return auditLogRepository.findByActionType(actionType);
    }

    public List<AuditLog> listarPorEntidad(String entityType, Integer entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public List<AuditLog> listarIntentosLoginFallidos() {
        return auditLogRepository.findByActionTypeAndStatusOrderByCreatedAtDesc(
            AuditLog.ActionType.FAILED_LOGIN,
            AuditLog.Status.FAILED
        );
    }

    public Long contarIntentosLoginFallidos(Integer userId, int minutos) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutos);
        return auditLogRepository.countFailedLoginAttempts(userId, since);
    }

    public List<AuditLog> listarPorIP(String ipAddress, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByIpAddressAndCreatedAtBetween(ipAddress, start, end);
    }

    public Optional<AuditLog> obtenerPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return auditLogRepository.findById(id);
    }

    // M\u00e9todos de utilidad para registrar acciones comunes
    public void registrarLogin(Integer userId, String ipAddress, String userAgent) {
        AuditLog log = AuditLog.builder()
            .userId(userId)
            .actionType(AuditLog.ActionType.LOGIN)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .description("Usuario inici\u00f3 sesi\u00f3n exitosamente")
            .status(AuditLog.Status.SUCCESS)
            .build();
        registrar(log);
    }

    public void registrarLoginFallido(String email, String ipAddress, String userAgent, String reason) {
        AuditLog log = AuditLog.builder()
            .actionType(AuditLog.ActionType.FAILED_LOGIN)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .description("Intento de login fallido para: " + email + ". Raz\u00f3n: " + reason)
            .status(AuditLog.Status.FAILED)
            .build();
        registrar(log);
    }

    public void registrarLogout(Integer userId, String ipAddress) {
        AuditLog log = AuditLog.builder()
            .userId(userId)
            .actionType(AuditLog.ActionType.LOGOUT)
            .ipAddress(ipAddress)
            .description("Usuario cerr\u00f3 sesi\u00f3n")
            .status(AuditLog.Status.SUCCESS)
            .build();
        registrar(log);
    }

    public void registrarCreacion(Integer userId, String entityType, Integer entityId, String description) {
        AuditLog log = AuditLog.builder()
            .userId(userId)
            .actionType(AuditLog.ActionType.CREATE)
            .entityType(entityType)
            .entityId(entityId)
            .description(description)
            .status(AuditLog.Status.SUCCESS)
            .build();
        registrar(log);
    }

    public void registrarActualizacion(Integer userId, String entityType, Integer entityId, 
                                      String oldValues, String newValues, String description) {
        AuditLog log = AuditLog.builder()
            .userId(userId)
            .actionType(AuditLog.ActionType.UPDATE)
            .entityType(entityType)
            .entityId(entityId)
            .oldValues(oldValues)
            .newValues(newValues)
            .description(description)
            .status(AuditLog.Status.SUCCESS)
            .build();
        registrar(log);
    }

    public void registrarEliminacion(Integer userId, String entityType, Integer entityId, String description) {
        AuditLog log = AuditLog.builder()
            .userId(userId)
            .actionType(AuditLog.ActionType.DELETE)
            .entityType(entityType)
            .entityId(entityId)
            .description(description)
            .status(AuditLog.Status.SUCCESS)
            .build();
        registrar(log);
    }
}



