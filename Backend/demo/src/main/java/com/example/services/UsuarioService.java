package com.example.services;

import com.example.models.Usuario;
import com.example.repositories.UsuarioRepository;
import com.example.exceptions.BadRequestException;
import com.example.patterns.users.creacionales.UsuarioFactory;
import com.example.patterns.users.comportamiento.EmailValidationStrategy;
import com.example.patterns.users.comportamiento.PasswordValidationStrategy;
import com.example.patterns.users.comportamiento.UsuarioValidator;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UsuarioValidator validator;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        // Patr\u00f3n Strategy - Configurar validador con estrategias
        this.validator = new UsuarioValidator();
        this.validator.addStrategy(new EmailValidationStrategy());
        this.validator.addStrategy(new PasswordValidationStrategy());
    }

    /**
     * Registra un usuario usando validaci\u00f3n con Strategy Pattern
     */
    public String registrarUsuario(Usuario usuario) {
        if (usuario.getEmail() != null) {
            usuario.setEmail(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (usuario.getName() != null) {
            usuario.setName(usuario.getName().trim());
        }
        if (usuario.getDni() != null && !usuario.getDni().trim().isEmpty()) {
            String cleanDni = usuario.getDni().trim();
            if (!cleanDni.matches("^\\d{8}$")) {
                return "El DNI debe contener exactamente 8 d\u00edgitos num\u00e9ricos.";
            }
            if (!Boolean.TRUE.equals(usuario.getAgeVerified())) {
                return "Debes confirmar que eres mayor de 18 a\u00f1os y titular del DNI ingresado.";
            }
            usuario.setDni(cleanDni);
            usuario.setAgeVerified(true);
        } else {
            usuario.setDni(null);
            usuario.setAgeVerified(false);
        }
        if (usuario.getTokenVersion() == null) {
            usuario.setTokenVersion(1);
        }
        if (usuario.getLoginAttempts() == null) {
            usuario.setLoginAttempts(0);
        }

        // Validar usando Strategy Pattern
        UsuarioValidator.ValidationResult validationResult = validator.validate(usuario);
        if (!validationResult.isValid()) {
            return "Errores de validaci\u00f3n: " + String.join(", ", validationResult.getErrors());
        }

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            return "El correo ya se encuentra registrado.";
        }

        if (usuarioRepository.existsByName(usuario.getName())) {
            return "El nombre de usuario ya se encuentra registrado.";
        }
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        if (guardado.getId() == null) {
            throw new IllegalStateException("No se pudo crear el usuario en la base de datos");
        }
        return "\u00a1Acabas de registrarte con \u00e9xito!";
    }

    /**
     * Registra un cliente usando Factory Pattern
     */
    public String registrarCliente(String email, String password, String name, String phone) {
        Usuario cliente = UsuarioFactory.crearCliente(email, password, name);

        // Agregar el tel\u00e9fono si se proporcion\u00f3
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
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Persiste la sesi\u00f3n sobre la fila ya guardada. El login recib\u00eda un
     * usuario desconectado y actualizarUsuario no copiaba refresh token ni
     * \u00faltimo acceso, as\u00ed que la fila quedaba sin sesi\u00f3n.
     */
    @CacheEvict(value = "users", key = "#p0")
    public Usuario guardarDatosDeSesion(@NonNull Long id, String refreshToken, LocalDateTime refreshTokenExpiry,
            boolean reiniciarIntentos) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setRefreshToken(refreshToken);
        usuario.setRefreshTokenExpiry(refreshTokenExpiry);
        if (reiniciarIntentos) {
            usuario.resetLoginAttempts();
            usuario.setLastLogin(LocalDateTime.now());
        }
        return usuarioRepository.saveAndFlush(usuario);
    }

    @CacheEvict(value = "users", key = "#p0")
    public Usuario registrarIntentoFallido(@NonNull Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.incrementLoginAttempts();
        return usuarioRepository.saveAndFlush(usuario);
    }

    public boolean validarPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Cacheable(value = "users", key = "#p0")
    public Optional<Usuario> obtenerPorId(@NonNull Long id) {
        return usuarioRepository.findById(id);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @CacheEvict(value = "users", key = "#p0")
    public Usuario actualizarUsuario(@NonNull Long id, Usuario usuarioActualizado) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();

            // Validar correo \u00fanico (excepto el mismo usuario)
            if (!usuario.getEmail().equals(usuarioActualizado.getEmail()) &&
                    usuarioRepository.existsByEmail(usuarioActualizado.getEmail())) {
                throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
            }

            // Validar nombre y apellido \u00fanicos (excepto el mismo usuario)
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
            if (usuarioActualizado.getDni() != null) {
                String cleanDni = usuarioActualizado.getDni().trim();
                if (cleanDni.isEmpty()) {
                    usuario.setDni(null);
                    usuario.setAgeVerified(false);
                } else {
                    if (!cleanDni.matches("^\\d{8}$")) {
                        throw new IllegalArgumentException("El DNI debe contener exactamente 8 d\u00edgitos num\u00e9ricos.");
                    }
                    usuario.setDni(cleanDni);
                    usuario.setAgeVerified(true);
                }
            }

            // Si el rol cambia, incrementar versi\u00f3n del token e invalidar
            // refresh_token
            if (!usuario.getRole().equals(usuarioActualizado.getRole())) {
                usuario.incrementTokenVersion();
                // Invalidar refresh token para forzar nuevo login
                usuario.setRefreshToken(null);
                usuario.setRefreshTokenExpiry(null);
            }
            usuario.setRole(usuarioActualizado.getRole()); // Permitir cambiar el rol

            // Solo actualiza la contrase\u00f1a si se env\u00eda una nueva, NO est\u00e1
            // vac\u00eda,
            // y NO empieza con $2a$ (no es un hash de BCrypt)
            if (usuarioActualizado.getPasswordHash() != null &&
                    !usuarioActualizado.getPasswordHash().isEmpty() &&
                    !usuarioActualizado.getPasswordHash().startsWith("$2a$") &&
                    !usuarioActualizado.getPasswordHash().startsWith("$2b$") &&
                    !usuarioActualizado.getPasswordHash().startsWith("$2y$")) {
                // Es una contrase\u00f1a en texto plano, encriptarla
                usuario.setPasswordHash(passwordEncoder.encode(usuarioActualizado.getPasswordHash()));
                usuario.setPasswordSet(true);

                // Si cambia la contrase\u00f1a, invalidar tokens anteriores
                usuario.incrementTokenVersion();
                usuario.setRefreshToken(null);
                usuario.setRefreshTokenExpiry(null);
            }

            return usuarioRepository.save(usuario);
        }
        return null;
    }

    // M\u00e9todo espec\u00edfico para actualizar perfil sin tocar la
    // contrase\u00f1a
    @CacheEvict(value = "users", key = "#p0")
    public Usuario actualizarPerfil(@NonNull Long id, String name, String email, String phone) {
        return actualizarPerfil(id, name, email, phone, null, null);
    }

    @CacheEvict(value = "users", key = "#p0")
    public Usuario actualizarPerfil(@NonNull Long id, String name, String email, String phone, String dni, Boolean ageVerified) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Usuario usuario = usuarioOpt.get();

        // Validar correo \u00fanico (excepto el mismo usuario)
        if (!usuario.getEmail().equals(email) && usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese correo.");
        }

        // Actualizar solo los campos del perfil (NO la contrase\u00f1a ni el role)
        usuario.setName(name);
        usuario.setEmail(email);
        usuario.setPhone(phone);

        if (dni != null) {
            String cleanDni = dni.trim();
            if (cleanDni.isEmpty()) {
                usuario.setDni(null);
                usuario.setAgeVerified(false);
            } else {
                if (!cleanDni.matches("^\\d{8}$")) {
                    throw new IllegalArgumentException("El DNI debe contener exactamente 8 d\u00edgitos num\u00e9ricos.");
                }
                if (ageVerified != null && !ageVerified && !cleanDni.equals(usuario.getDni())) {
                    throw new IllegalArgumentException("Debes confirmar que eres mayor de 18 a\u00f1os y titular del DNI ingresado.");
                }
                usuario.setDni(cleanDni);
                usuario.setAgeVerified(true);
            }
        }

        return usuarioRepository.save(usuario);
    }

    /**
     * Eliminaci\u00f3n L\u00f3gica (Soft Delete).
     * Nunca se borra f\u00edsicamente de la base de datos para mantener integridad
     * referencial e historial.
     */
    @CacheEvict(value = "users", key = "#p0")
    public boolean eliminarUsuario(@NonNull Long id) {
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
     * Inactiva un usuario (Soft Delete)
     * Marca el campo deleted_at con la fecha actual
     */
    @CacheEvict(value = "users", key = "#p0")
    public boolean inactivarUsuario(@NonNull Long id) {
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
    @CacheEvict(value = "users", key = "#p0")
    public boolean reactivarUsuario(@NonNull Long id) {
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
     * Verifica si un usuario est\u00e1 activo (no eliminado)
     */
    public boolean estaActivo(@NonNull Long id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        return usuarioOpt.isPresent() && usuarioOpt.get().getDeletedAt() == null;
    }

    /**
     * Cambia la contrase\u00f1a de un usuario (sin requerir contrase\u00f1a actual,
     * para uso administrativo/interno)
     */
    @CacheEvict(value = "users", key = "#p0")
    public boolean cambiarPassword(@NonNull Long id, String nuevaPassword) {
        return cambiarPassword(id, null, nuevaPassword, true);
    }

    /**
     * Cambia la contrase\u00f1a de un usuario validando su contrase\u00f1a actual
     * cuando corresponde
     */
    @CacheEvict(value = "users", key = "#p0")
    public boolean cambiarPassword(@NonNull Long id, String currentPassword, String nuevaPassword,
            boolean skipCurrentPasswordCheck) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (!skipCurrentPasswordCheck && !Boolean.FALSE.equals(usuario.getPasswordSet())) {
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    throw new BadRequestException("Debes ingresar tu contrase\u00f1a actual para confirmar el cambio");
                }
                if (!passwordEncoder.matches(currentPassword, usuario.getPasswordHash())) {
                    throw new BadRequestException("La contrase\u00f1a actual ingresada es incorrecta");
                }
                if (passwordEncoder.matches(nuevaPassword, usuario.getPasswordHash())) {
                    throw new BadRequestException("La nueva contrase\u00f1a debe ser diferente a la actual");
                }
            }
            usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
            usuario.setPasswordSet(true);

            // Invalidar tokens anteriores al cambiar contrase\u00f1a
            usuario.incrementTokenVersion();
            usuario.setRefreshToken(null);
            usuario.setRefreshTokenExpiry(null);

            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }

    public boolean existePorNombre(String nombre) {
        return usuarioRepository.existsByName(nombre);
    }
}
