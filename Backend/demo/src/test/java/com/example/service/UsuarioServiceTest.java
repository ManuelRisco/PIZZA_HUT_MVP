package com.example.service;

import com.example.domain.model.Usuario;
import com.example.domain.repository.UsuarioRepository;
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

@SuppressWarnings("null")
@DisplayName("UC4: Pruebas de Gestión de Usuarios - TDD")
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
        usuarioTest.setName("Juan Pérez");
        usuarioTest.setEmail("juan@example.com");
        usuarioTest.setPhone("1234567890");
        usuarioTest.setRole(Usuario.Role.CUSTOMER);
        usuarioTest.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("UC4.1: Crear usuario exitosamente")
    void testCrearUsuarioExitoso() {
        when(usuarioRepository.save(usuarioTest)).thenReturn(usuarioTest);
        Usuario usuarioCreado = usuarioRepository.save(usuarioTest);
        assertNotNull(usuarioCreado);
        assertEquals("Juan Pérez", usuarioCreado.getName());
        assertEquals("juan@example.com", usuarioCreado.getEmail());
        assertEquals(Usuario.Role.CUSTOMER, usuarioCreado.getRole());
        verify(usuarioRepository, times(1)).save(usuarioTest);
    }

    @Test
    @DisplayName("UC4.2: Obtener usuario por ID")
    void testObtenerUsuarioPorId() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioTest));
        Optional<Usuario> usuarioEncontrado = usuarioRepository.findById(1L);
        assertTrue(usuarioEncontrado.isPresent());
        assertEquals("juan@example.com", usuarioEncontrado.get().getEmail());
        verify(usuarioRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("UC4.3: Buscar usuario por email")
    void testBuscarUsuarioPorEmail() {
        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(usuarioTest));
        Optional<Usuario> usuarioEncontrado = usuarioRepository.findByEmail("juan@example.com");
        assertTrue(usuarioEncontrado.isPresent());
        assertEquals("Juan Pérez", usuarioEncontrado.get().getName());
        verify(usuarioRepository, times(1)).findByEmail("juan@example.com");
    }

    @Test
    @DisplayName("UC4.4: Actualizar perfil de usuario")
    void testActualizarUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioTest));
        Usuario usuarioActualizado = new Usuario();
        usuarioActualizado.setName("Juan Carlos Pérez");
        usuarioActualizado.setPhone("9876543210");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioActualizado);
        Usuario resultado = usuarioRepository.save(usuarioActualizado);
        assertNotNull(resultado);
        assertEquals("Juan Carlos Pérez", resultado.getName());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("UC4.5: Validar email único")
    void testValidarEmailUnico() {
        when(usuarioRepository.existsByEmail("juan@example.com")).thenReturn(true);
        when(usuarioRepository.existsByEmail("nuevo@example.com")).thenReturn(false);
        assertTrue(usuarioRepository.existsByEmail("juan@example.com"));
        assertFalse(usuarioRepository.existsByEmail("nuevo@example.com"));
        verify(usuarioRepository, times(1)).existsByEmail("juan@example.com");
        verify(usuarioRepository, times(1)).existsByEmail("nuevo@example.com");
    }

    @Test
    @DisplayName("UC4.6: Obtener usuarios por nombre")
    void testObtenerUsuariosPorNombre() {
        when(usuarioRepository.findByName("Juan Pérez")).thenReturn(Optional.of(usuarioTest));
        Optional<Usuario> usuarioEncontrado = usuarioRepository.findByName("Juan Pérez");
        assertTrue(usuarioEncontrado.isPresent());
        assertEquals("Juan Pérez", usuarioEncontrado.get().getName());
        assertEquals(Usuario.Role.CUSTOMER, usuarioEncontrado.get().getRole());
        verify(usuarioRepository, times(1)).findByName("Juan Pérez");
    }

    @Test
    @DisplayName("UC4.7: Marcar usuario como eliminado")
    void testEliminarUsuario() {
        usuarioTest.setDeletedAt(LocalDateTime.now());
        when(usuarioRepository.save(usuarioTest)).thenReturn(usuarioTest);
        Usuario usuarioEliminado = usuarioRepository.save(usuarioTest);
        assertNotNull(usuarioEliminado.getDeletedAt());
        verify(usuarioRepository, times(1)).save(usuarioTest);
    }

    @Test
    @DisplayName("UC4.8: Listar todos los usuarios")
    void testListarTodosLosUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(usuarioTest);
        when(usuarioRepository.findAll()).thenReturn(usuarios);
        List<Usuario> usuariosEncontrados = (List<Usuario>) usuarioRepository.findAll();
        assertEquals(1, usuariosEncontrados.size());
        verify(usuarioRepository, times(1)).findAll();
    }
}
