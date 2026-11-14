package com.example.controllers;

import com.example.dtos.ApiResponse;
import com.example.dtos.UsuarioDTO;
import com.example.exceptions.BadRequestException;
import com.example.mappers.UsuarioMapper;
import com.example.models.Usuario;
import com.example.security.JwtTokenProvider;
import com.example.services.AuditLogService;
import com.example.services.AuthRecoveryAndGoogleService;
import com.example.services.SessionLogService;
import com.example.services.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthRecoveryAndGoogleController {

    private final AuthRecoveryAndGoogleService authRecoveryAndGoogleService;
    private final UsuarioService usuarioService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;
    private final SessionLogService sessionLogService;
    private final UsuarioMapper usuarioMapper;

    public AuthRecoveryAndGoogleController(AuthRecoveryAndGoogleService authRecoveryAndGoogleService,
                                           UsuarioService usuarioService,
                                           JwtTokenProvider jwtTokenProvider,
                                           AuditLogService auditLogService,
                                           SessionLogService sessionLogService,
                                           UsuarioMapper usuarioMapper) {
        this.authRecoveryAndGoogleService = authRecoveryAndGoogleService;
        this.usuarioService = usuarioService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
        this.sessionLogService = sessionLogService;
        this.usuarioMapper = usuarioMapper;
    }

    @GetMapping("/auth/google/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerConfiguracionGoogle() {
        return ResponseEntity.ok(ApiResponse.success(
                authRecoveryAndGoogleService.obtenerConfiguracionGoogle()
        ));
    }

    @PostMapping("/auth/google")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginConGoogle(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {

        AuthRecoveryAndGoogleService.GoogleAuthResult result =
                authRecoveryAndGoogleService.autenticarConGoogle(payload);
        Usuario user = result.usuario();

        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getName(),
                user.getId(),
                user.getTokenVersion()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getEmail(),
                user.getRole().name(),
                user.getName(),
                user.getId()
        );

        usuarioService.guardarDatosDeSesion(user.getId(), refreshToken, LocalDateTime.now().plusDays(7), true);

        if (result.newlyCreated()) {
            auditLogService.registrarCreacion(
                    user.getId().intValue(),
                    "Usuario",
                    user.getId().intValue(),
                    "Cuenta creada autom\u00e1ticamente con Google OAuth: " + user.getEmail()
            );
        }
        auditLogService.registrarLogin(user.getId().intValue(), obtenerIP(request), obtenerUserAgent(request));
        sessionLogService.crearSesion(user.getId().intValue(), token, obtenerIP(request), obtenerUserAgent(request));

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "token", token,
                        "refreshToken", refreshToken,
                        "usuario", usuarioMapper.toDto(user),
                        "newlyCreated", result.newlyCreated(),
                        "requiresPasswordSetup", Boolean.FALSE.equals(user.getPasswordSet())
                ),
                "Inicio de sesi\u00f3n con Google exitoso. Bienvenido " + user.getName()
        ));
    }

    @PostMapping("/usuarios/me/vincular-google")
    public ResponseEntity<ApiResponse<UsuarioDTO>> vincularCuentaConGoogle(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, String> payload) {

        String token = authHeader != null ? authHeader.replace("Bearer ", "").trim() : "";
        Long userId = jwtTokenProvider.getIdFromToken(token);
        if (userId == null) {
            throw new BadRequestException("Token inv\u00e1lido");
        }

        Usuario actualizado = authRecoveryAndGoogleService.vincularCuentaConGoogle(userId, payload);
        return ResponseEntity.ok(ApiResponse.success(
                usuarioMapper.toDto(actualizado),
                "Cuenta vinculada con Google correctamente."
        ));
    }

    @PostMapping("/usuarios/me/definir-password")
    public ResponseEntity<ApiResponse<UsuarioDTO>> definirPasswordCuentaGoogle(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String token = authHeader != null ? authHeader.replace("Bearer ", "").trim() : "";
        Long userId = jwtTokenProvider.getIdFromToken(token);
        if (userId == null) {
            throw new BadRequestException("Token inv\u00e1lido");
        }

        String nuevaPassword = body != null ? body.get("password") : null;
        Usuario actualizado = authRecoveryAndGoogleService.definirPasswordCuentaGoogle(userId, nuevaPassword);
        auditLogService.registrarActualizacion(
                actualizado.getId().intValue(),
                "Usuario",
                actualizado.getId().intValue(),
                "{\"passwordSet\":false}",
                "{\"passwordSet\":true}",
                "Usuario defini\u00f3 contrase\u00f1a para su cuenta de Google (" + obtenerIP(request) + ")"
        );
        return ResponseEntity.ok(ApiResponse.success(
                usuarioMapper.toDto(actualizado),
                "Contrase\u00f1a configurada correctamente. Ahora tambi\u00e9n puedes iniciar sesi\u00f3n con tu correo y contrase\u00f1a."
        ));
    }

    @PostMapping("/recuperar-password/solicitar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> solicitarRecuperacionPassword(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String email = body != null ? body.get("email") : null;
        AuthRecoveryAndGoogleService.PasswordResetRequestResult result =
                authRecoveryAndGoogleService.solicitarRecuperacionPassword(email);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "emailSent", result.emailSent(),
                        "maskedEmail", result.maskedEmail(),
                        "expirationMinutes", result.expirationMinutes()
                ),
                result.message()
        ));
    }

    @PostMapping("/recuperar-password/verificar-codigo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verificarCodigoRecuperacion(
            @RequestBody Map<String, String> body) {

        String email = body != null ? body.get("email") : null;
        String code = body != null ? body.get("code") : null;
        boolean valid = authRecoveryAndGoogleService.verificarCodigoRecuperacion(email, code);

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("valid", valid),
                "C\u00f3digo verificado correctamente."
        ));
    }

    @PostMapping("/recuperar-password/confirmar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmarRecuperacionPassword(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String email = body != null ? body.get("email") : null;
        String code = body != null ? body.get("code") : null;
        String newPassword = body != null ? body.get("newPassword") : null;

        Usuario actualizado = authRecoveryAndGoogleService.confirmarRecuperacionPassword(email, code, newPassword);

        auditLogService.registrarActualizacion(
                actualizado.getId().intValue(),
                "Usuario",
                actualizado.getId().intValue(),
                "{\"password\":\"***\"}",
                "{\"password\":\"reset_by_otp\"}",
                "Contrase\u00f1a restablecida mediante c\u00f3digo OTP enviado al correo (" + obtenerIP(request) + ")"
        );

        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "reset", true,
                        "email", actualizado.getEmail()
                ),
                "Tu contrase\u00f1a ha sido restablecida con \u00e9xito. Ya puedes iniciar sesi\u00f3n."
        ));
    }

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
