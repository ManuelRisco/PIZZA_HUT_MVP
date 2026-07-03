package com.example.services;

import com.example.models.AuditLog;
import com.example.repositories.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Adicional.1: Auditoría y Seguridad - Pruebas TDD")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog log;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        log = new AuditLog();
        log.setId(1L);
        log.setUserId(1);
        log.setActionType(AuditLog.ActionType.LOGIN);
    }

    // Fase RED / GREEN / REFACTOR
    @Test
    @DisplayName("Adicional.2: Listar logs de auditoría")
    void testListarLogs() {
        List<AuditLog> list = new ArrayList<>();
        list.add(log);
        when(auditLogRepository.findAll()).thenReturn(list);

        List<AuditLog> resultado = auditLogService.listarTodos();

        assertEquals(1, resultado.size());
        verify(auditLogRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Adicional.3: Registrar log")
    void testRegistrarLog() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(log);
        AuditLog resultado = auditLogService.registrar(log);
        assertNotNull(resultado);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Adicional.4: Obtener por ID")
    void testObtenerPorId() {
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(log));
        Optional<AuditLog> result = auditLogService.obtenerPorId(1L);
        assertTrue(result.isPresent());
        verify(auditLogRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Adicional.5: Listar por Usuario")
    void testListarPorUsuario() {
        List<AuditLog> list = new ArrayList<>();
        list.add(log);
        when(auditLogRepository.findByUserId(1)).thenReturn(list);
        List<AuditLog> result = auditLogService.listarPorUsuario(1);
        assertEquals(1, result.size());
        verify(auditLogRepository, times(1)).findByUserId(1);
    }
}
