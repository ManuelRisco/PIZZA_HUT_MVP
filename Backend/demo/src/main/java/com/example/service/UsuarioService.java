package com.example.service;

import com.example.domain.model.Usuario;
import com.example.domain.repository.UsuarioRepository;
import com.example.application.users.creacionales.UsuarioFactory;
import com.example.application.users.comportamiento.EmailValidationStrategy;
import com.example.application.users.comportamiento.PasswordValidationStrategy;
import com.example.application.users.comportamiento.UsuarioValidator;
import com.example.application.users.comportamiento.UsuarioSpecification;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UsuarioValidator validator;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        // Patrón Strategy - Configurar validador con estrategias
        this.validator = new UsuarioValidator();
        this.validator.addStrategy(new EmailValidationStrategy());
        this.validator.addStrategy(new PasswordValidationStrategy());
    }

    /**
     * Registra un usuario usando validación con Strategy Pattern
     */
    public String registrarUsuario(Usuario usuario) {
        // Validar usando Strategy Pattern
        UsuarioValidator.ValidationResult validationResult = validator.validate(usuario);
        if (!validationResult.isValid()) {
            return "Errores de validación: " + String.join(", ", validationResult.getErrors());
        }

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            return "El correo ya se encuentra registrado.";
        }
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        usuarioRepository.save(usuario);
        return "¡Acabas de registrarte con éxito!";
    }

    /**
     * Registra un cliente usando Factory Pattern
     */
    public String registrarCliente(String email, String password, String name, String phone) {
        Usuario cliente = UsuarioFactory.crearCliente(email, password, name);
        
        // Agregar el teléfono si se proporcionó
        if (phone != null && !phone.trim().isEmpty()) {
            cliente.setPhone(phone);
        }
        
        return registrarUsuario(cliente);
    }

    /**
     * Registra un administrador usando Factory Pattern
     */
    public String registrarAdministrador(String email, String password, String name) {
        Usuario admin = UsuarioFactory.crearAdministrador(email, password, name);
        return registrarUsuario(admin);
    }

    public Optional<Usuario> obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public boolean validarPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public Optional<Usuario> obtenerPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    /**
     * Lista usuarios filtrando por Specification Pattern
     */
    public List<Usuario> listarUsuariosPorEspecificacion(UsuarioSpecification specification) {
        return usuarioRepository.findAll().stream()
                .filter(specification::isSatisfiedBy)
                .collect(Collectors.toList());
    }

    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // Validar correo único (excepto el mismo usuario)
            if (!usuario.getEmail().equals(usuarioActualizado.getEmail()) &&
                usuarioRepository.existsByEmail(usuarioActualizado.getEmail())) {
                throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
            }

            // Validar nombre y apellido únicos (excepto el mismo usuario)
            List<Usuario> usuariosConNombre = usuarioRepository.findAll().stream()
                .filter(u -> u.getName() != null && u.getName().equalsIgnoreCase(usuarioActualizado.getName()))
                .filter(u -> !u.getId().equals(usuario.getId())) // <-- Ignora el usuario actual
                .toList();
            if (!usuariosConNombre.isEmpty()) {
                throw new IllegalArgumentException("Ya existe un usuario con ese nombre y apellido.");
            }

            usuario.setEmail(usuarioActualizado.getEmail());
            usuario.setName(usuarioActualizado.getName());
            usuario.setPhone(usuarioActualizado.getPhone());
            
            // Si el rol cambia, incrementar versión del token e invalidar refresh_token
            if (!usuario.getRole().equals(usuarioActualizado.getRole())) {
                usuario.incrementTokenVersion();
                // Invalidar refresh token para forzar nuevo login
                usuario.setRefreshToken(null);
                usuario.setRefreshTokenExpiry(null);
            }
            usuario.setRole(usuarioActualizado.getRole()); // Permitir cambiar el rol

            // Solo actualiza la contraseña si se envía una nueva, NO está vacía, 
            // y NO empieza con $2a$ (no es un hash de BCrypt)
            if (usuarioActualizado.getPasswordHash() != null &&
                !usuarioActualizado.getPasswordHash().isEmpty() &&
                !usuarioActualizado.getPasswordHash().startsWith("$2a$") &&
                !usuarioActualizado.getPasswordHash().startsWith("$2b$") &&
                !usuarioActualizado.getPasswordHash().startsWith("$2y$")) {
                // Es una contraseña en texto plano, encriptarla
                usuario.setPasswordHash(passwordEncoder.encode(usuarioActualizado.getPasswordHash()));
            }
            // Si ya es un hash (empieza con $2a$), no hacer nada con la contraseña
            
            return usuarioRepository.save(usuario);
        }
        return null;
    }

    // Método específico para actualizar perfil sin tocar la contraseña
    public Usuario actualizarPerfil(Long id, String name, String email, String phone) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        
        Usuario usuario = usuarioOpt.get();

        // Validar correo único (excepto el mismo usuario)
        if (!usuario.getEmail().equals(email) && usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
        }

        // Actualizar solo los campos del perfil (NO la contraseña ni el role)
        usuario.setName(name);
        usuario.setEmail(email);
        usuario.setPhone(phone);
        
        return usuarioRepository.save(usuario);
    }

    public boolean eliminarUsuario(Long id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Inactiva un usuario (Soft Delete)
     * Marca el campo deleted_at con la fecha actual
     */
    public boolean inactivarUsuario(Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setDeletedAt(LocalDateTime.now());
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }

    /**
     * Reactiva un usuario previamente inactivado
     * Limpia el campo deleted_at
     */
    public boolean reactivarUsuario(Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setDeletedAt(null);
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }

    /**
     * Verifica si un usuario está activo (no eliminado)
     */
    public boolean estaActivo(Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        return usuarioOpt.isPresent() && usuarioOpt.get().getDeletedAt() == null;
    }

    /**
     * Cambia la contraseña de un usuario
     */
    public boolean cambiarPassword(Long id, String nuevaPassword) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }

    public boolean existePorNombre(String nombre) {
        return usuarioRepository.existsByName(nombre);
    }
}
