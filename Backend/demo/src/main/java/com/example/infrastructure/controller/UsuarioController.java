package com.example.infrastructure.controller;

import com.example.domain.dto.LoginDTO;
import com.example.domain.dto.UsuarioCreateDTO;
import com.example.domain.dto.UsuarioDTO;
import com.example.domain.model.Usuario;
import com.example.service.UsuarioService;
import com.example.service.AuditLogService;
import com.example.service.SessionLogService;
import com.example.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200") // <-- Permite solicitudes desde tu frontend
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private SessionLogService sessionLogService;

    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(@RequestBody UsuarioCreateDTO usuarioDTO) {
        System.out.println("Intentando registrar usuario: " + usuarioDTO.getEmail());
        
        // Crear usuario desde DTO
        Usuario usuario = new Usuario();
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setPasswordHash(usuarioDTO.getPassword()); // El servicio se encarga del hash
        usuario.setName(usuarioDTO.getName());
        usuario.setPhone(usuarioDTO.getPhone());
        usuario.setRole(usuarioDTO.getRole() != null ? usuarioDTO.getRole() : Usuario.Role.CUSTOMER);
        
        String mensaje = usuarioService.registrarUsuario(usuario);
        if (mensaje.contains("éxito")) {
            return ResponseEntity.ok(Map.of("message", mensaje));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", mensaje));
        }
    }

    @PostMapping("/ingresar")
    public ResponseEntity<?> loginUsuario(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        Optional<Usuario> userOptional = usuarioService.obtenerPorEmail(loginDTO.getEmail());
        if (userOptional.isPresent()) {
            Usuario user = userOptional.get();
            
            // Verificar si el usuario está activo (no inactivado)
            if (user.getDeletedAt() != null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Usuario inactivo. Contacte al administrador."));
            }
            
            // Verificar si la cuenta está bloqueada
            if (user.isAccountLocked()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Cuenta bloqueada por múltiples intentos fallidos. Intente más tarde.",
                    "lockedUntil", user.getLockedUntil()
                ));
            }
            
            if (usuarioService.validarPassword(loginDTO.getPassword(), user.getPasswordHash())) {
                // Reset intentos fallidos
                user.resetLoginAttempts();
                user.setLastLogin(LocalDateTime.now());
                
                // Generar JWT token con email, role, name, id y tokenVersion
                String token = jwtTokenProvider.generateToken(
                    user.getEmail(), 
                    user.getRole().name(), 
                    user.getName(), 
                    user.getId(), 
                    user.getTokenVersion()
                );
                String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name(), user.getName(), user.getId());
                
                // Guardar refreshToken en BD
                user.setRefreshToken(refreshToken);
                user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
                usuarioService.actualizarUsuario(user.getId(), user);
                
                // Registrar en audit log
                auditLogService.registrarLogin(user.getId().intValue(), obtenerIP(request), obtenerUserAgent(request));
                
                // Crear sesión log
                sessionLogService.crearSesion(user.getId().intValue(), token, obtenerIP(request), obtenerUserAgent(request));
                
                // Crear DTO de usuario sin información sensible
                UsuarioDTO usuarioDTO = new UsuarioDTO(user);
                
                return ResponseEntity.ok(Map.of(
                    "token", token,
                    "refreshToken", refreshToken,
                    "usuario", usuarioDTO,
                    "message", "Login exitoso. Bienvenido " + user.getName()
                ));
            } else {
                // Incrementar intentos fallidos
                user.incrementLoginAttempts();
                usuarioService.actualizarUsuario(user.getId(), user);
                
                // Registrar intento fallido
                auditLogService.registrarLoginFallido(loginDTO.getEmail(), obtenerIP(request), obtenerUserAgent(request), "Contraseña incorrecta");
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Contraseña incorrecta",
                    "remainingAttempts", 5 - user.getLoginAttempts()
                ));
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
        }
    }

    // Endpoints públicos para validación de duplicados
    @GetMapping("/verificar-email")
    public ResponseEntity<Map<String, Boolean>> verificarEmail(@RequestParam("email") String email) { // Corregido
        boolean existe = usuarioService.obtenerPorEmail(email).isPresent();
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Refresh token inválido o expirado"));
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Token proporcionado no es un refresh token"));
        }

        try {
            // Extraer información del refresh token
            Long userId = jwtTokenProvider.getIdFromToken(refreshToken);
            
            // Verificar que el usuario siga existiendo y esté activo
            Optional<Usuario> userOptional = usuarioService.obtenerPorId(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
            }
            
            Usuario user = userOptional.get();
            
            // Verificar si el usuario está activo (no inactivado)
            if (user.getDeletedAt() != null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "message", "Usuario inactivo. Su sesión ha sido cerrada.",
                    "userInactive", true
                ));
            }
            
            // Validar que el refreshToken coincida con el almacenado en BD
            if (!user.isRefreshTokenValid() || !refreshToken.equals(user.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Refresh token inválido o expirado. Por favor, inicie sesión nuevamente."
                ));
            }
            
            // Generar nuevos tokens CON tokenVersion actualizado
            String newToken = jwtTokenProvider.generateToken(
                user.getEmail(), 
                user.getRole().name(), 
                user.getName(), 
                user.getId(), 
                user.getTokenVersion()
            );
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name(), user.getName(), user.getId());
            
            // Actualizar refreshToken en BD
            user.setRefreshToken(newRefreshToken);
            user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
            usuarioService.actualizarUsuario(user.getId(), user);
            
            UsuarioDTO usuarioDTO = new UsuarioDTO(user);
            
            return ResponseEntity.ok(Map.of(
                "token", newToken,
                "refreshToken", newRefreshToken,
                "usuario", usuarioDTO
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al procesar refresh token"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader, HttpServletRequest request) {
        try {
            // Extraer token del header
            String token = authHeader.replace("Bearer ", "");
            
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token inválido"));
            }
            
            Long userId = jwtTokenProvider.getIdFromToken(token);
            Optional<Usuario> userOptional = usuarioService.obtenerPorId(userId);
            
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
            }
            
            Usuario user = userOptional.get();
            
            // Limpiar refreshToken en BD
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            usuarioService.actualizarUsuario(user.getId(), user);
            
            // Registrar logout en audit log
            auditLogService.registrarLogout(user.getId().intValue(), obtenerIP(request));
            
            // Cerrar sesión en session log
            sessionLogService.cerrarSesion(token, com.example.domain.model.SessionLog.LogoutReason.MANUAL);
            
            return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al cerrar sesión"));
        }
    }

    @GetMapping("/verificar-estado-usuario")
    public ResponseEntity<?> verificarEstadoUsuario(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenProvider.getIdFromToken(token);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Token inválido"));
            }
            
            boolean activo = usuarioService.estaActivo(userId);
            
            if (!activo) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "activo", false,
                    "message", "Usuario inactivo",
                    "userInactive", true
                ));
            }
            
            return ResponseEntity.ok(Map.of("activo", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al verificar estado"));
        }
    }

    @GetMapping("/verificar-nombre")
    public ResponseEntity<Map<String, Boolean>> verificarNombre(@RequestParam("nombre") String nombre) { // Corregido
        boolean existe = usuarioService.existePorNombre(nombre);
        return ResponseEntity.ok(Map.of("existe", existe));
    }

    /**
     * Obtiene la información completa del usuario autenticado actual
     * GET /api/usuarios/me
     * Accesible para cualquier usuario autenticado (CUSTOMER o ADMIN)
     */
    @GetMapping("/usuarios/me")
    public ResponseEntity<?> obtenerUsuarioActual(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenProvider.getIdFromToken(token);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Token inválido"));
            }
            
            Optional<Usuario> usuarioOpt = usuarioService.obtenerPorId(userId);
            if (usuarioOpt.isPresent()) {
                UsuarioDTO usuarioDTO = new UsuarioDTO(usuarioOpt.get());
                return ResponseEntity.ok(usuarioDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al obtener información del usuario"));
        }
    }

    /**
     * Actualiza el perfil del usuario autenticado actual
     * PUT /api/usuarios/me
     * Accesible para cualquier usuario autenticado (CUSTOMER o ADMIN)
     * Permite actualizar: name, email, phone
     */
    @PutMapping("/usuarios/me")
    public ResponseEntity<?> actualizarPerfilUsuario(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> updates) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = jwtTokenProvider.getIdFromToken(token);
            
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Token inválido"));
            }
            
            Optional<Usuario> usuarioOpt = usuarioService.obtenerPorId(userId);
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
            }
            
            Usuario usuario = usuarioOpt.get();
            
            // Extraer y validar los campos
            String newName = usuario.getName();
            String newEmail = usuario.getEmail();
            String newPhone = usuario.getPhone();
            
            if (updates.containsKey("name")) {
                newName = (String) updates.get("name");
                if (newName == null || newName.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El nombre no puede estar vacío"));
                }
                newName = newName.trim();
            }
            
            if (updates.containsKey("email")) {
                newEmail = (String) updates.get("email");
                if (newEmail == null || newEmail.trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "El email no puede estar vacío"));
                }
                newEmail = newEmail.trim();
                
                // Verificar que el email no esté en uso por otro usuario
                Optional<Usuario> existingUser = usuarioService.obtenerPorEmail(newEmail);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "El email ya está en uso"));
                }
            }
            
            if (updates.containsKey("phone")) {
                newPhone = (String) updates.get("phone");
                newPhone = (newPhone != null && !newPhone.trim().isEmpty()) ? newPhone.trim() : null;
            }
            
            // Actualizar usando el método específico de perfil que NO toca la contraseña
            Usuario usuarioActualizado = usuarioService.actualizarPerfil(userId, newName, newEmail, newPhone);
            UsuarioDTO usuarioDTO = new UsuarioDTO(usuarioActualizado);
            
            return ResponseEntity.ok(usuarioDTO);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al actualizar perfil: " + e.getMessage()));
        }
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        List<UsuarioDTO> usuariosDTO = usuarios.stream()
            .map(UsuarioDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(usuariosDTO);
    }

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable("id") Long id) { // Corregido
        Optional<Usuario> usuarioOpt = usuarioService.obtenerPorId(id);
        if (usuarioOpt.isPresent()) {
            UsuarioDTO usuarioDTO = new UsuarioDTO(usuarioOpt.get());
            return ResponseEntity.ok(usuarioDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
        }
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarUsuario(@PathVariable("id") Long id, @RequestBody UsuarioCreateDTO usuarioDTO) { // Corregido
        try {
            // Crear usuario desde DTO
            Usuario usuarioActualizado = new Usuario();
            usuarioActualizado.setEmail(usuarioDTO.getEmail());
            usuarioActualizado.setName(usuarioDTO.getName());
            usuarioActualizado.setPhone(usuarioDTO.getPhone());
            usuarioActualizado.setRole(usuarioDTO.getRole());
            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty()) {
                usuarioActualizado.setPasswordHash(usuarioDTO.getPassword());
            }
            
            Usuario usuario = usuarioService.actualizarUsuario(id, usuarioActualizado);
            if (usuario != null) {
                UsuarioDTO responseDTO = new UsuarioDTO(usuario);
                return ResponseEntity.ok(responseDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminarUsuario(@PathVariable("id") Long id) { // Corregido
        boolean eliminado = usuarioService.eliminarUsuario(id);
        if (eliminado) {
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
        }
    }

    /**
     * Inactiva un usuario (Soft Delete)
     * PATCH /api/usuarios/{id}/inactivar
     */
    @PatchMapping("/usuarios/{id}/inactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> inactivarUsuario(@PathVariable("id") Long id) { // Corregido
        boolean inactivado = usuarioService.inactivarUsuario(id);
        if (inactivado) {
            return ResponseEntity.ok(Map.of("message", "Usuario inactivado correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
        }
    }

    /**
     * Reactiva un usuario previamente inactivado
     * PATCH /api/usuarios/{id}/reactivar
     */
    @PatchMapping("/usuarios/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reactivarUsuario(@PathVariable("id") Long id) { // Corregido
        boolean reactivado = usuarioService.reactivarUsuario(id);
        if (reactivado) {
            return ResponseEntity.ok(Map.of("message", "Usuario reactivado correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
        }
    }

    /**
     * Verifica si un usuario está activo
     * GET /api/usuarios/{id}/activo
     */
    @GetMapping("/usuarios/{id}/activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> verificarUsuarioActivo(@PathVariable("id") Long id) { // Corregido
        boolean activo = usuarioService.estaActivo(id);
        return ResponseEntity.ok(Map.of("activo", activo));
    }

    /**
     * Cambia la contraseña de un usuario
     * PATCH /api/usuarios/{id}/cambiar-password
     */
    @PatchMapping("/usuarios/{id}/cambiar-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiarPassword(@PathVariable("id") Long id, @RequestBody Map<String, String> passwordData) { // Corregido
        try {
            String nuevaPassword = passwordData.get("password");
            if (nuevaPassword == null || nuevaPassword.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "La contraseña no puede estar vacía"));
            }
            
            if (nuevaPassword.length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "La contraseña debe tener al menos 6 caracteres"));
            }
            
            boolean cambiado = usuarioService.cambiarPassword(id, nuevaPassword);
            if (cambiado) {
                return ResponseEntity.ok(Map.of("message", "Contraseña cambiada correctamente"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Usuario no encontrado"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error al cambiar la contraseña: " + e.getMessage()));
        }
    }
    
    // Métodos auxiliares
    private String obtenerIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    private String obtenerUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }
}