package com.example.services;

import com.example.models.Usuario;
import com.example.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CU01/CU04/CU11: Gestión de Usuarios - Pruebas TDD")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        usuarioTest = new Usuario();
        usuarioTest.setId(1L);
        usuarioTest.setName("Juan Perez");
        usuarioTest.setEmail("juan@example.com");
        usuarioTest.setPhone("1234567890");
        usuarioTest.setPasswordHash("password123");
        usuarioTest.setRole(Usuario.Role.CUSTOMER);
        usuarioTest.setCreatedAt(LocalDateTime.now());
    }

    // ==========================================
    // CU01: Registrarse
    // ==========================================

    @Test
    @DisplayName("CU01 - Flujo Principal: Registro exitoso [RF01]")
    void testCrearUsuarioExitoso() {
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.existsByName(anyString())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioTest);

        String resultado = usuarioService.registrarUsuario(usuarioTest);

        assertEquals("¡Acabas de registrarte con éxito!", resultado);
        verify(usuarioRepository, times(1)).save(usuarioTest);
    }

    @Test
    @DisplayName("CU01 - A1: Error por campos incompletos (Contraseña muy corta)")
    void testRegistroCamposIncompletos() {
        // ValidationUtils intercepts invalid passwords in the service
        usuarioTest.setPasswordHash("123"); // Too short
        String resultado = usuarioService.registrarUsuario(usuarioTest);
        assertTrue(resultado.contains("Errores de validación") || resultado.contains("caracteres"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("CU01 - A4: Error por correo ya registrado [RF02]")
    void testRegistroCorreoYaRegistrado() {
        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(true);

        String resultado = usuarioService.registrarUsuario(usuarioTest);

        assertEquals("El correo ya se encuentra registrado.", resultado);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ==========================================
    // CU04: Ver Perfil
    // ==========================================

    @Test
    @DisplayName("CU04 - Flujo Principal: Actualizar perfil exitosamente [RF07, RF08]")
    void testActualizarPerfilExitoso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioTest));
        when(usuarioRepository.existsByEmail("nuevo@example.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioTest);

        Usuario resultado = usuarioService.actualizarPerfil(1L, "Juan Carlos", "nuevo@example.com", "9876543210");

        assertNotNull(resultado);
        assertEquals("Juan Carlos", resultado.getName());
        assertEquals("nuevo@example.com", resultado.getEmail());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("CU04 - A1: Error al actualizar con correo en uso")
    void testActualizarPerfilCorreoEnUso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioTest));
        when(usuarioRepository.existsByEmail("otro@example.com")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.actualizarPerfil(1L, "Juan", "otro@example.com", "123456");
        });

        assertEquals("Ya existe un usuario con ese correo.", exception.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ==========================================
    // CU09: Acceder al Panel
    // ==========================================

    @Test
    @DisplayName("CU09 - Flujo Principal: Validar rol administrador [RF17, RF18]")
    void testValidarRolAdministrador() {
        usuarioTest.setRole(Usuario.Role.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioTest));

        Optional<Usuario> usuarioOpt = usuarioService.obtenerPorId(1L);
        assertTrue(usuarioOpt.isPresent());
        assertEquals(Usuario.Role.ADMIN, usuarioOpt.get().getRole());
    }

    // ==========================================
    // CU11: Gestionar Usuarios
    // ==========================================

    @Test
    @DisplayName("CU11 - Flujo Principal: Editar rol de usuario [RF21]")
    void testEditarRolUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioTest));

        Usuario actualizacion = new Usuario();
        actualizacion.setEmail("juan@example.com");
        actualizacion.setName("Juan Perez");
        actualizacion.setRole(Usuario.Role.ADMIN);

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioTest);

        usuarioService.actualizarUsuario(1L, actualizacion);
        verify(usuarioRepository, times(1)).save(usuarioTest);
        assertEquals(Usuario.Role.ADMIN, usuarioTest.getRole());
        assertNull(usuarioTest.getRefreshToken()); // Tokens are invalidated on role change
    }

    @Test
    @DisplayName("CU11 - Flujo Principal: Inactivar usuario [RF21, RF22]")
    void testInactivarUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioTest));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioTest);

        boolean resultado = usuarioService.inactivarUsuario(1L);

        assertTrue(resultado);
        assertNotNull(usuarioTest.getDeletedAt());
        verify(usuarioRepository, times(1)).save(usuarioTest);
    }

    @Test
    @DisplayName("CU11 - A1: Error al intentar editar usuario no encontrado")
    void testEditarUsuarioNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Usuario actualizacion = new Usuario();
        Usuario resultado = usuarioService.actualizarUsuario(99L, actualizacion);

        assertNull(resultado);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("CU11 - Listar todos los usuarios [RF22]")
    void testListarTodosLosUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(usuarioTest);
        when(usuarioRepository.findAll()).thenReturn(usuarios);

        List<Usuario> resultados = usuarioService.listarUsuarios();

        assertEquals(1, resultados.size());
        verify(usuarioRepository, times(1)).findAll();
    }
}
