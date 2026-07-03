package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.mappers.UsuarioMapper;
import com.example.dtos.LoginDTO;
import com.example.dtos.UsuarioCreateDTO;
import com.example.dtos.UsuarioDTO;
import com.example.models.Usuario;
import com.example.services.UsuarioService;
import com.example.services.AuditLogService;
import com.example.services.SessionLogService;
import com.example.security.JwtTokenProvider;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final SessionLogService sessionLogService;
    private final UsuarioMapper usuarioMapper;

    public UsuarioController(UsuarioService usuarioService, JwtTokenProvider jwtTokenProvider,
            AuditLogService auditLogService, SessionLogService sessionLogService, UsuarioMapper usuarioMapper) {
        this.usuarioService = usuarioService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
        this.sessionLogService = sessionLogService;
        this.usuarioMapper = usuarioMapper;
    }

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<Void>> registrarUsuario(@Valid @RequestBody UsuarioCreateDTO usuarioDTO) {
        Usuario usuario = usuarioMapper.toEntity(usuarioDTO);

        // Mapeo manual de la contrase\u00f1a para asegurar que no se pierda por
        // MapStruct
        if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty()) {
            usuario.setPasswordHash(usuarioDTO.getPassword());
        }

        // PREVENCIÓN DE ESCALADA DE PRIVILEGIOS: Todo registro por este endpoint público siempre será CUSTOMER
        usuario.setRole(Usuario.Role.CUSTOMER);

        String mensaje = usuarioService.registrarUsuario(usuario);
        if (mensaje.contains("\u00e9xito")) {
            // Log de creaci\u00f3n si se cre\u00f3 exitosamente
            Usuario guardado = usuarioService.obtenerPorEmail(usuario.getEmail()).get();
            auditLogService.registrarCreacion(
                    getCurrentUserId(),
                    "Usuario",
                    guardado.getId().intValue(),
                    "Se registr\u00f3 un nuevo usuario: " + guardado.getEmail());
            return ResponseEntity.ok(ApiResponse.success(null, mensaje));
        } else {
            throw new BadRequestException(mensaje);
        }
    }

    @PostMapping("/ingresar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginUsuario(@Valid @RequestBody LoginDTO loginDTO,
            HttpServletRequest request) {
        Usuario user = usuarioService.obtenerPorEmail(loginDTO.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getDeletedAt() != null) {
            throw new BadRequestException("Usuario inactivo. Contacte al administrador.");
        }

        if (user.isAccountLocked()) {
            throw new BadRequestException(
                    "Cuenta bloqueada por m\u00faltiples intentos fallidos. Intente m\u00e1s tarde.");
        }

        if (usuarioService.validarPassword(loginDTO.getPassword(), user.getPasswordHash())) {
            user.resetLoginAttempts();
            user.setLastLogin(LocalDateTime.now());

            String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getName(),
                    user.getId(), user.getTokenVersion());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name(),
                    user.getName(), user.getId());

            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
            usuarioService.actualizarUsuario(user.getId(), user);

            auditLogService.registrarLogin(user.getId().intValue(), obtenerIP(request), obtenerUserAgent(request));
            sessionLogService.crearSesion(user.getId().intValue(), token, obtenerIP(request),
                    obtenerUserAgent(request));

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("token", token, "refreshToken", refreshToken, "usuario", usuarioMapper.toDto(user)),
                    "Login exitoso. Bienvenido " + user.getName()));
        } else {
            user.incrementLoginAttempts();
            usuarioService.actualizarUsuario(user.getId(), user);
            auditLogService.registrarLoginFallido(loginDTO.getEmail(), obtenerIP(request), obtenerUserAgent(request),
                    "Contrase\u00f1a incorrecta");

            throw new BadRequestException(
                    "Contrase\u00f1a incorrecta. Intentos restantes: " + (5 - user.getLoginAttempts()));
        }
    }

    @GetMapping("/verificar-email")
    public ResponseEntity<ApiResponse<Boolean>> verificarEmail(@RequestParam("email") String email) {
        return ResponseEntity.ok(ApiResponse.success(usuarioService.obtenerPorEmail(email).isPresent()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)
                || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Refresh token inv\u00e1lido o expirado");
        }

        Long userId = jwtTokenProvider.getIdFromToken(refreshToken);
        Usuario user = usuarioService.obtenerPorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getDeletedAt() != null || !user.isRefreshTokenValid()
                || !refreshToken.equals(user.getRefreshToken())) {
            throw new BadRequestException(
                    "Refresh token inv\u00e1lido o expirado. Por favor, inicie sesi\u00f3n nuevamente.");
        }

        String newToken = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getName(),
                user.getId(), user.getTokenVersion());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name(),
                user.getName(), user.getId());

        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        usuarioService.actualizarUsuario(user.getId(), user);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("token", newToken, "refreshToken", newRefreshToken, "usuario", usuarioMapper.toDto(user))));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtTokenProvider.validateToken(token)) {
            throw new BadRequestException("Token inv\u00e1lido");
        }

        Long userId = jwtTokenProvider.getIdFromToken(token);
        Usuario user = usuarioService.obtenerPorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        usuarioService.actualizarUsuario(user.getId(), user);

        auditLogService.registrarLogout(user.getId().intValue(), obtenerIP(request));
        sessionLogService.cerrarSesion(token, com.example.models.SessionLog.LogoutReason.MANUAL);

        return ResponseEntity.ok(ApiResponse.success(null, "Sesi\u00f3n cerrada correctamente"));
    }

    @GetMapping("/verificar-estado-usuario")
    public ResponseEntity<ApiResponse<Boolean>> verificarEstadoUsuario(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getIdFromToken(token);
        if (userId == null)
            throw new BadRequestException("Token inv\u00e1lido");

        if (!usuarioService.estaActivo(userId)) {
            throw new BadRequestException("Usuario inactivo");
        }
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    @GetMapping("/verificar-nombre")
    public ResponseEntity<ApiResponse<Boolean>> verificarNombre(@RequestParam("nombre") String nombre) {
        return ResponseEntity.ok(ApiResponse.success(usuarioService.existePorNombre(nombre)));
    }

    @GetMapping("/usuarios/me")
    public ResponseEntity<ApiResponse<UsuarioDTO>> obtenerUsuarioActual(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getIdFromToken(token);
        if (userId == null)
            throw new BadRequestException("Token inv\u00e1lido");

        Usuario user = usuarioService.obtenerPorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return ResponseEntity.ok(ApiResponse.success(usuarioMapper.toDto(user)));
    }

    @PutMapping("/usuarios/me")
    public ResponseEntity<ApiResponse<UsuarioDTO>> actualizarPerfilUsuario(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> updates) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtTokenProvider.getIdFromToken(token);
        if (userId == null)
            throw new BadRequestException("Token inv\u00e1lido");

        Usuario usuario = usuarioService.obtenerPorId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String newName = updates.containsKey("name") ? ((String) updates.get("name")).trim() : usuario.getName();
        String newEmail = updates.containsKey("email") ? ((String) updates.get("email")).trim() : usuario.getEmail();
        String newPhone = updates.containsKey("phone") ? ((String) updates.get("phone")).trim() : usuario.getPhone();

        Usuario usuarioActualizado = usuarioService.actualizarPerfil(userId, newName, newEmail, newPhone);
        return ResponseEntity.ok(ApiResponse.success(usuarioMapper.toDto(usuarioActualizado)));
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> listarUsuarios() {
        return ResponseEntity.ok(ApiResponse.success(usuarioMapper.toDtoList(usuarioService.listarUsuarios())));
    }

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioDTO>> obtenerUsuarioPorId(@PathVariable("id") Long id) {
        Usuario user = usuarioService.obtenerPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return ResponseEntity.ok(ApiResponse.success(usuarioMapper.toDto(user)));
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UsuarioDTO>> actualizarUsuario(@PathVariable("id") Long id,
            @RequestBody UsuarioCreateDTO usuarioDTO) {
        Usuario usuarioActualizado = usuarioMapper.toEntity(usuarioDTO);
        if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty()) {
            usuarioActualizado.setPasswordHash(usuarioDTO.getPassword());
        }

        Usuario usuario = usuarioService.actualizarUsuario(id, usuarioActualizado);
        auditLogService.registrarActualizacion(
                getCurrentUserId(),
                "Usuario",
                id.intValue(),
                "{}", "{}",
                "Usuario actualizado por el administrador");
        return ResponseEntity.ok(ApiResponse.success(usuarioMapper.toDto(usuario)));
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarUsuario(@PathVariable("id") Long id) {
        if (!usuarioService.eliminarUsuario(id))
            throw new ResourceNotFoundException("Usuario no encontrado");

        auditLogService.registrarEliminacion(
                getCurrentUserId(),
                "Usuario",
                id.intValue(),
                "Usuario eliminado de forma permanente");
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado correctamente"));
    }

    @PatchMapping("/usuarios/{id}/inactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> inactivarUsuario(@PathVariable("id") Long id) {
        if (!usuarioService.inactivarUsuario(id))
            throw new ResourceNotFoundException("Usuario no encontrado");

        auditLogService.registrarActualizacion(
                getCurrentUserId(),
                "Usuario",
                id.intValue(),
                "{\"status\": \"Activo\"}", "{\"status\": \"Inactivo\"}",
                "Usuario fue inactivado (Soft Delete)");
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario inactivado correctamente"));
    }

    @PatchMapping("/usuarios/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reactivarUsuario(@PathVariable("id") Long id) {
        if (!usuarioService.reactivarUsuario(id))
            throw new ResourceNotFoundException("Usuario no encontrado");

        auditLogService.registrarActualizacion(
                getCurrentUserId(),
                "Usuario",
                id.intValue(),
                "{\"status\": \"Inactivo\"}", "{\"status\": \"Activo\"}",
                "Usuario fue reactivado");
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario reactivado correctamente"));
    }

    @GetMapping("/usuarios/{id}/activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> verificarUsuarioActivo(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(usuarioService.estaActivo(id)));
    }

    @PatchMapping("/usuarios/{id}/cambiar-password")
    public ResponseEntity<ApiResponse<Boolean>> cambiarPassword(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {

        String token = authHeader.replace("Bearer ", "");
        Long tokenUserId = jwtTokenProvider.getIdFromToken(token);
        String role = jwtTokenProvider.getRoleFromToken(token);

        if (tokenUserId == null || (!tokenUserId.equals(id) && !"ADMIN".equals(role))) {
            throw new BadRequestException("No autorizado para cambiar esta contraseña");
        }
        String nuevaPassword = body.get("password");
        if (nuevaPassword == null || nuevaPassword.trim().isEmpty()) {
            throw new BadRequestException("La nueva contraseña es obligatoria");
        }

        boolean actualizado = usuarioService.cambiarPassword(id, nuevaPassword);
        if (!actualizado) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }

        return ResponseEntity.ok(ApiResponse.success(true, "Contraseña cambiada correctamente"));
    }

    private String obtenerIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();
        return ip;
    }

    private String obtenerUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }

    private Integer getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long) {
            return ((Long) auth.getDetails()).intValue();
        }
        return null;
    }
}
