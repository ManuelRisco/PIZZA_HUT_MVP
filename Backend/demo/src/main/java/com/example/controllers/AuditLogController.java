package com.example.controllers;

import com.example.dtos.AuditLogDTO;
import com.example.models.AuditLog;
import com.example.services.AuditLogService;
import com.example.repositories.UsuarioRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "http://localhost:4200")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UsuarioRepository usuarioRepository;

    public AuditLogController(AuditLogService auditLogService, UsuarioRepository usuarioRepository) {
        this.auditLogService = auditLogService;
        this.usuarioRepository = usuarioRepository;
    }

    private List<AuditLogDTO> mapToDTOs(List<AuditLog> logs) {
        if (logs.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // Obtener todos los IDs de usuario Ãºnicos
        List<Long> userIds = logs.stream()
                .filter(l -> l.getUserId() != null)
                .map(l -> l.getUserId().longValue())
                .distinct()
                .toList();

        // Buscar todos los usuarios en una sola consulta
        java.util.Map<Long, com.example.models.Usuario> usersMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            usuarioRepository.findAllById(userIds).forEach(u -> usersMap.put(u.getId().longValue(), u));
        }

        return logs.stream().map(log -> {
            AuditLogDTO dto = new AuditLogDTO(log);
            if (log.getUserId() != null) {
                com.example.models.Usuario u = usersMap.get(log.getUserId().longValue());
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> listarTodos() {
        List<AuditLog> logs = auditLogService.listarTodos();
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(logs)));
    }

    @GetMapping("/recientes")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> listarRecientes(@RequestParam(defaultValue = "24") int horas) {
        List<AuditLog> logs = auditLogService.listarRecientes(horas);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(logs)));
    }

    @GetMapping("/usuario/{userId}")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> listarPorUsuario(@PathVariable Integer userId) {
        List<AuditLog> logs = auditLogService.listarPorUsuario(userId);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(logs)));
    }

    @GetMapping("/usuario/{userId}/rango")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> listarPorUsuarioEnRango(
            @PathVariable Integer userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<AuditLog> logs = auditLogService.listarPorUsuarioEnRango(userId, start, end);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(logs)));
    }

    @GetMapping("/tipo/{actionType}")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> listarPorTipo(@PathVariable AuditLog.ActionType actionType) {
        List<AuditLog> logs = auditLogService.listarPorTipoAccion(actionType);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(logs)));
    }

    @GetMapping("/entidad/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> listarPorEntidad(
            @PathVariable String entityType,
            @PathVariable Integer entityId) {
        
        List<AuditLog> logs = auditLogService.listarPorEntidad(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(logs)));
    }

    @GetMapping("/logins-fallidos")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> listarIntentosLoginFallidos() {
        List<AuditLog> logs = auditLogService.listarIntentosLoginFallidos();
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(logs)));
    }

    @GetMapping("/ip/{ipAddress}")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> listarPorIP(
            @PathVariable String ipAddress,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<AuditLog> logs = auditLogService.listarPorIP(ipAddress, start, end);
        return ResponseEntity.ok(ApiResponse.success(mapToDTOs(logs)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogDTO>> obtenerPorId(@PathVariable Long id) {
        Optional<AuditLog> log = auditLogService.obtenerPorId(id);
        if (log.isPresent()) {
            AuditLogDTO dto = new AuditLogDTO(log.get());
            if (log.get().getUserId() != null) {
                usuarioRepository.findById(log.get().getUserId().longValue())
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
}

