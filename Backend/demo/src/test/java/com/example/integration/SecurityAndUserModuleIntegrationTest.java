package com.example.integration;

import com.example.models.SessionLog;
import com.example.models.Usuario;
import com.example.repositories.SessionLogRepository;
import com.example.services.SessionLogService;
import com.example.services.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Test de Integración del Módulo 1: Seguridad, Usuarios y Sesiones")
public class SecurityAndUserModuleIntegrationTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SessionLogService sessionLogService;

    @Autowired
    private SessionLogRepository sessionLogRepository;

    @Test
    @DisplayName("Flujo completo: Registrar usuario con factory/estrategia, encriptar contraseña, auditar sesión y verificar roles")
    void testSecurityAndUserFlow() {
        String testEmail = "test_modulo1_" + System.currentTimeMillis() + "@pizzahut.com";

        String msg = usuarioService.registrarCliente(testEmail, "PasswordSeguro123!", "Usuario Integracion", "987654321");
        assertTrue(msg.contains("éxito"), "El registro debe ser exitoso");

        Usuario guardado = usuarioService.obtenerPorEmail(testEmail).orElseThrow();

        assertNotNull(guardado.getId());
        assertEquals(Usuario.Role.CUSTOMER, guardado.getRole());
        assertTrue(guardado.getPasswordHash().startsWith("$2a$") || guardado.getPasswordHash().startsWith("$2b$"),
                "La contraseña debe estar encriptada con BCrypt");

        // Registrar inicio de sesión
        String token = "test_token_" + System.currentTimeMillis();
        SessionLog sessionCreada = sessionLogService.crearSesion(
                guardado.getId().intValue(),
                token,
                "127.0.0.1",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) IntegrationTestBrowser"
        );
        assertNotNull(sessionCreada.getId());
        assertEquals(guardado.getId().intValue(), sessionCreada.getUserId());
        assertTrue(sessionCreada.getIsActive());

        // Cerrar sesión
        sessionLogService.cerrarSesion(token, SessionLog.LogoutReason.MANUAL);

        SessionLog finalSession = sessionLogRepository.findById(sessionCreada.getId()).orElseThrow();
        assertNotNull(finalSession.getLogoutTime());
        assertFalse(finalSession.getIsActive());
        assertEquals(SessionLog.LogoutReason.MANUAL, finalSession.getLogoutReason());
    }
}
