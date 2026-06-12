package com.example.infrastructure.controller;

import com.example.domain.dto.AuditLogDTO;
import com.example.domain.model.AuditLog;
import com.example.service.AuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "http://localhost:4200")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogDTO>> listarTodos() {
        List<AuditLog> logs = auditLogService.listarTodos();
        List<AuditLogDTO> dtos = logs.stream()
            .map(AuditLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<AuditLogDTO>> listarRecientes(@RequestParam(defaultValue = "24") int horas) {
        List<AuditLog> logs = auditLogService.listarRecientes(horas);
        List<AuditLogDTO> dtos = logs.stream()
            .map(AuditLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<List<AuditLogDTO>> listarPorUsuario(@PathVariable Integer userId) {
        List<AuditLog> logs = auditLogService.listarPorUsuario(userId);
        List<AuditLogDTO> dtos = logs.stream()
            .map(AuditLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/usuario/{userId}/rango")
    public ResponseEntity<List<AuditLogDTO>> listarPorUsuarioEnRango(
            @PathVariable Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<AuditLog> logs = auditLogService.listarPorUsuarioEnRango(userId, start, end);
        List<AuditLogDTO> dtos = logs.stream()
            .map(AuditLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/tipo/{actionType}")
    public ResponseEntity<List<AuditLogDTO>> listarPorTipo(@PathVariable AuditLog.ActionType actionType) {
        List<AuditLog> logs = auditLogService.listarPorTipoAccion(actionType);
        List<AuditLogDTO> dtos = logs.stream()
            .map(AuditLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/entidad/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLogDTO>> listarPorEntidad(
            @PathVariable String entityType,
            @PathVariable Integer entityId) {
        
        List<AuditLog> logs = auditLogService.listarPorEntidad(entityType, entityId);
        List<AuditLogDTO> dtos = logs.stream()
            .map(AuditLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/logins-fallidos")
    public ResponseEntity<List<AuditLogDTO>> listarIntentosLoginFallidos() {
        List<AuditLog> logs = auditLogService.listarIntentosLoginFallidos();
        List<AuditLogDTO> dtos = logs.stream()
            .map(AuditLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/ip/{ipAddress}")
    public ResponseEntity<List<AuditLogDTO>> listarPorIP(
            @PathVariable String ipAddress,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<AuditLog> logs = auditLogService.listarPorIP(ipAddress, start, end);
        List<AuditLogDTO> dtos = logs.stream()
            .map(AuditLogDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogDTO> obtenerPorId(@PathVariable Long id) {
        Optional<AuditLog> log = auditLogService.obtenerPorId(id);
        return log.map(l -> ResponseEntity.ok(new AuditLogDTO(l)))
            .orElse(ResponseEntity.notFound().build());
    }
}
