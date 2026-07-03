package com.example.services;

import com.example.models.SessionLog;
import com.example.repositories.SessionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SessionLogService {

    private final SessionLogRepository sessionLogRepository;

    public SessionLogService(SessionLogRepository sessionLogRepository) {
        this.sessionLogRepository = sessionLogRepository;
    }

    public SessionLog crearSesion(Integer userId, String sessionToken, String ipAddress, String userAgent) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new IllegalArgumentException("El token de sesión no puede estar vacío");
        }
        
        SessionLog sessionLog = new SessionLog(userId, sessionToken, ipAddress, userAgent);
        
        // Detectar tipo de dispositivo y navegador desde userAgent
        sessionLog.setDeviceType(detectarTipoDispositivo(userAgent));
        sessionLog.setBrowser(detectarNavegador(userAgent));
        
        return sessionLogRepository.save(sessionLog);
    }

    public void cerrarSesion(String sessionToken, SessionLog.LogoutReason reason) {
        Optional<SessionLog> sessionOpt = sessionLogRepository.findBySessionToken(sessionToken);
        if (sessionOpt.isPresent()) {
            SessionLog session = sessionOpt.get();
            session.endSession(reason);
            sessionLogRepository.save(session);
        }
    }

    public void cerrarSesionesPorUsuario(Integer userId, SessionLog.LogoutReason reason) {
        List<SessionLog> sessions = sessionLogRepository.findByUserIdAndIsActiveTrue(userId);
        for (SessionLog session : sessions) {
            session.endSession(reason);
            sessionLogRepository.save(session);
        }
    }

    public List<SessionLog> listarSesionesActivas() {
        return sessionLogRepository.findTop100ByIsActiveTrueOrderByLoginTimeDesc();
    }

    public List<SessionLog> listarSesionesPorUsuario(Integer userId) {
        return sessionLogRepository.findByUserIdOrderByLoginTimeDesc(userId);
    }

    public List<SessionLog> listarSesionesActivasPorUsuario(Integer userId) {
        return sessionLogRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public Long contarSesionesActivasPorUsuario(Integer userId) {
        return sessionLogRepository.countByUserIdAndIsActiveTrue(userId);
    }

    public Optional<SessionLog> obtenerUltimaSesion(Integer userId) {
        return sessionLogRepository.findFirstByUserIdOrderByLoginTimeDesc(userId);
    }

    public List<SessionLog> listarSesionesPorIP(String ipAddress) {
        return sessionLogRepository.findByIpAddress(ipAddress);
    }

    public List<SessionLog> listarSesionesPorRango(LocalDateTime start, LocalDateTime end) {
        return sessionLogRepository.findByLoginTimeBetween(start, end);
    }

    public List<SessionLog> listarSesionesLargasActivas(int horasUmbral) {
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(horasUmbral);
        return sessionLogRepository.findLongActiveSessions(thresholdTime);
    }

    public void cerrarSesionesInactivas(int horasInactividad) {
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(horasInactividad);
        List<SessionLog> sessions = sessionLogRepository.findLongActiveSessions(thresholdTime);
        
        for (SessionLog session : sessions) {
            session.endSession(SessionLog.LogoutReason.TIMEOUT);
            sessionLogRepository.save(session);
        }
    }

    public Long contarSesionesPorUsuarioDesde(Integer userId, LocalDateTime since) {
        return sessionLogRepository.countSessionsByUserSince(userId, since);
    }

    public Optional<SessionLog> obtenerPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return sessionLogRepository.findById(id);
    }

    public Optional<SessionLog> obtenerPorToken(String sessionToken) {
        return sessionLogRepository.findBySessionToken(sessionToken);
    }

    // Métodos de utilidad para detección
    private String detectarTipoDispositivo(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile") || userAgent.contains("android") || userAgent.contains("iphone")) {
            return "Mobile";
        } else if (userAgent.contains("tablet") || userAgent.contains("ipad")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }

    private String detectarNavegador(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("edg")) {
            return "Edge";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari")) {
            return "Safari";
        } else if (userAgent.contains("opera") || userAgent.contains("opr")) {
            return "Opera";
        } else {
            return "Other";
        }
    }
}
