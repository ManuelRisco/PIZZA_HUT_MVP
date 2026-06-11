package com.example.controller;

import com.example.domain.dto.SessionLogDTO;
import com.example.domain.model.SessionLog;
import com.example.service.SessionLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "http://localhost:4200")
public class SessionLogController {

    private final SessionLogService sessionLogService;

    public SessionLogController(SessionLogService sessionLogService) {
        this.sessionLogService = sessionLogService;
    }

    @GetMapping("/activas")
    public ResponseEntity<List<SessionLogDTO>> listarSesionesActivas() {
        List<SessionLog> sessions = sessionLogService.listarSesionesActivas();
        List<SessionLogDTO> dtos = sessions.stream()
            .map(SessionLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<List<SessionLogDTO>> listarPorUsuario(@PathVariable Integer userId) {
        List<SessionLog> sessions = sessionLogService.listarSesionesPorUsuario(userId);
        List<SessionLogDTO> dtos = sessions.stream()
            .map(SessionLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/usuario/{userId}/activas")
    public ResponseEntity<List<SessionLogDTO>> listarActivasPorUsuario(@PathVariable Integer userId) {
        List<SessionLog> sessions = sessionLogService.listarSesionesActivasPorUsuario(userId);
        List<SessionLogDTO> dtos = sessions.stream()
            .map(SessionLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/usuario/{userId}/count")
    public ResponseEntity<Map<String, Long>> contarSesionesActivas(@PathVariable Integer userId) {
        Long count = sessionLogService.contarSesionesActivasPorUsuario(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/usuario/{userId}/ultima")
    public ResponseEntity<SessionLogDTO> obtenerUltimaSesion(@PathVariable Integer userId) {
        Optional<SessionLog> session = sessionLogService.obtenerUltimaSesion(userId);
        return session.map(s -> ResponseEntity.ok(new SessionLogDTO(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ip/{ipAddress}")
    public ResponseEntity<List<SessionLogDTO>> listarPorIP(@PathVariable String ipAddress) {
        List<SessionLog> sessions = sessionLogService.listarSesionesPorIP(ipAddress);
        List<SessionLogDTO> dtos = sessions.stream()
            .map(SessionLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/rango")
    public ResponseEntity<List<SessionLogDTO>> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<SessionLog> sessions = sessionLogService.listarSesionesPorRango(start, end);
        List<SessionLogDTO> dtos = sessions.stream()
            .map(SessionLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/largas")
    public ResponseEntity<List<SessionLogDTO>> listarSesionesLargas(@RequestParam(defaultValue = "12") int horas) {
        List<SessionLog> sessions = sessionLogService.listarSesionesLargasActivas(horas);
        List<SessionLogDTO> dtos = sessions.stream()
            .map(SessionLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionLogDTO> obtenerPorId(@PathVariable Long id) {
        Optional<SessionLog> session = sessionLogService.obtenerPorId(id);
        return session.map(s -> ResponseEntity.ok(new SessionLogDTO(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cerrar/{sessionToken}")
    public ResponseEntity<?> cerrarSesion(
            @PathVariable String sessionToken,
            @RequestParam(defaultValue = "MANUAL") SessionLog.LogoutReason reason) {
        
        sessionLogService.cerrarSesion(sessionToken, reason);
        return ResponseEntity.ok().body("Sesión cerrada correctamente");
    }

    @PostMapping("/cerrar-usuario/{userId}")
    public ResponseEntity<?> cerrarSesionesPorUsuario(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "ADMIN_ACTION") SessionLog.LogoutReason reason) {
        
        sessionLogService.cerrarSesionesPorUsuario(userId, reason);
        return ResponseEntity.ok().body("Sesiones cerradas correctamente");
    }

    @PostMapping("/limpiar-inactivas")
    public ResponseEntity<?> limpiarSesionesInactivas(@RequestParam(defaultValue = "24") int horas) {
        sessionLogService.cerrarSesionesInactivas(horas);
        return ResponseEntity.ok().body("Sesiones inactivas cerradas correctamente");
    }
}
