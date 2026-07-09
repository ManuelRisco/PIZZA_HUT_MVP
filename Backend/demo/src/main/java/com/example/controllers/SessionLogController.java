package com.example.controllers;

import com.example.dtos.SessionLogDTO;
import com.example.models.SessionLog;
import com.example.services.SessionLogService;
import com.example.repositories.UsuarioRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "http://localhost:4200")
public class SessionLogController {

    private static final String MSG_KEY = "message";

    private final SessionLogService sessionLogService;
    private final UsuarioRepository usuarioRepository;

    public SessionLogController(SessionLogService sessionLogService, UsuarioRepository usuarioRepository) {
        this.sessionLogService = sessionLogService;
        this.usuarioRepository = usuarioRepository;
    }

    private List<SessionLogDTO> mapToDTOs(List<SessionLog> sessions) {
        if (sessions.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // Obtener todos los IDs de usuario Ãºnicos
        List<Long> userIds = sessions.stream()
                .filter(s -> s.getUserId() != null)
                .map(s -> s.getUserId().longValue())
                .distinct()
                .toList();

        // Buscar todos los usuarios en una sola consulta (Evitar N+1)
        java.util.Map<Long, com.example.models.Usuario> usersMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            usuarioRepository.findAllById(userIds).forEach(u -> usersMap.put(u.getId().longValue(), u));
        }

        return sessions.stream().map(session -> {
            SessionLogDTO dto = new SessionLogDTO(session);
            if (session.getUserId() != null) {
                com.example.models.Usuario u = usersMap.get(session.getUserId().longValue());
                if (u != null) {
                    String displayName = (u.getName() != null && !u.getName().trim().isEmpty()) 
                                         ? u.getName() 
                                         : u.getEmail();
                    dto.setUserName(displayName);
                }
            }
            return dto;
        }).toList();
    }

    @GetMapping("/activas")
    public ResponseEntity<ApiResponse<List<SessionLogDTO>>> listarSesionesActivas() {
        List<SessionLog> sessions = sessionLogService.listarSesionesActivas();
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(sessions)));
    }

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<ApiResponse<List<SessionLogDTO>>> listarPorUsuario(@PathVariable("userId") Integer userId) {
        List<SessionLog> sessions = sessionLogService.listarSesionesPorUsuario(userId);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(sessions)));
    }

    @GetMapping("/usuario/{userId}/activas")
    public ResponseEntity<ApiResponse<List<SessionLogDTO>>> listarActivasPorUsuario(@PathVariable("userId") Integer userId) {
        List<SessionLog> sessions = sessionLogService.listarSesionesActivasPorUsuario(userId);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(sessions)));
    }

    @GetMapping("/usuario/{userId}/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> contarSesionesActivas(@PathVariable("userId") Integer userId) {
        Long count = sessionLogService.contarSesionesActivasPorUsuario(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @GetMapping("/usuario/{userId}/ultima")
    public ResponseEntity<ApiResponse<SessionLogDTO>> obtenerUltimaSesion(@PathVariable("userId") Integer userId) {
        Optional<SessionLog> session = sessionLogService.obtenerUltimaSesion(userId);
        if (session.isPresent()) {
            SessionLogDTO dto = new SessionLogDTO(session.get());
            if (session.get().getUserId() != null) {
                usuarioRepository.findById(session.get().getUserId().longValue())
                    .ifPresent(u -> {
                        String displayName = (u.getName() != null && !u.getName().trim().isEmpty()) 
                                             ? u.getName() 
                                             : u.getEmail();
                        dto.setUserName(displayName);
                    });
            }
            return ResponseEntity.ok(ApiResponse.success(dto));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/ip/{ipAddress}")
    public ResponseEntity<ApiResponse<List<SessionLogDTO>>> listarPorIP(@PathVariable("ipAddress") String ipAddress) {
        List<SessionLog> sessions = sessionLogService.listarSesionesPorIP(ipAddress);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(sessions)));
    }

    @GetMapping("/rango")
    public ResponseEntity<ApiResponse<List<SessionLogDTO>>> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<SessionLog> sessions = sessionLogService.listarSesionesPorRango(start, end);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(sessions)));
    }

    @GetMapping("/largas")
    public ResponseEntity<ApiResponse<List<SessionLogDTO>>> listarSesionesLargas(@RequestParam(value = "horas", defaultValue = "12") int horas) {
        List<SessionLog> sessions = sessionLogService.listarSesionesLargasActivas(horas);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(sessions)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionLogDTO>> obtenerPorId(@PathVariable("id") Long id) {
        Optional<SessionLog> session = sessionLogService.obtenerPorId(id);
        if (session.isPresent()) {
            SessionLogDTO dto = new SessionLogDTO(session.get());
            if (session.get().getUserId() != null) {
                usuarioRepository.findById(session.get().getUserId().longValue())
                    .ifPresent(u -> {
                        String displayName = (u.getName() != null && !u.getName().trim().isEmpty()) 
                                             ? u.getName() 
                                             : u.getEmail();
                        dto.setUserName(displayName);
                    });
            }
            return ResponseEntity.ok(ApiResponse.success(dto));
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/cerrar/{sessionToken}")
    public ResponseEntity<Object> cerrarSesion(
            @PathVariable("sessionToken") String sessionToken,
            @RequestParam(value = "reason", required = false) String reason) {
        
        SessionLog.LogoutReason logoutReason = SessionLog.LogoutReason.MANUAL;
        if (reason != null) {
            try {
                if (reason.equals("CLOSED_BY_ADMIN")) {
                    logoutReason = SessionLog.LogoutReason.FORCED;
                } else {
                    logoutReason = SessionLog.LogoutReason.valueOf(reason.toUpperCase());
                }
            } catch (IllegalArgumentException e) {
                // Ignore and use default
            }
        }
        
        sessionLogService.cerrarSesion(sessionToken, logoutReason);
        return ResponseEntity.ok().body(Map.of(MSG_KEY, "SesiÃ³n cerrada correctamente"));
    }

    @PostMapping("/cerrar-usuario/{userId}")
    public ResponseEntity<Object> cerrarSesionesPorUsuario(
            @PathVariable("userId") Integer userId,
            @RequestParam(value = "reason", required = false) String reason) {
        
        SessionLog.LogoutReason logoutReason = SessionLog.LogoutReason.FORCED;
        if (reason != null) {
            try {
                if (!reason.equals("CLOSED_BY_ADMIN")) {
                    logoutReason = SessionLog.LogoutReason.valueOf(reason.toUpperCase());
                }
            } catch (IllegalArgumentException e) {
                // Ignore and use default
            }
        }
        
        sessionLogService.cerrarSesionesPorUsuario(userId, logoutReason);
        return ResponseEntity.ok().body(Map.of(MSG_KEY, "Sesiones cerradas correctamente"));
    }

    @PostMapping("/limpiar-inactivas")
    public ResponseEntity<Object> limpiarSesionesInactivas(@RequestParam(value = "horas", defaultValue = "24") int horas) {
        sessionLogService.cerrarSesionesInactivas(horas);
        return ResponseEntity.ok().body(Map.of(MSG_KEY, "Sesiones inactivas cerradas correctamente"));
    }
}

