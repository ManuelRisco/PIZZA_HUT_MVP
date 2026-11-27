package com.ecopizza.controllers;

import com.ecopizza.dtos.ApiResponse;
import com.ecopizza.dtos.GoogleAuthRequestDTO;
import com.ecopizza.dtos.GoogleAuthResponseDTO;
import com.ecopizza.dtos.GoogleAuthResultDTO;
import com.ecopizza.dtos.GoogleConfigDTO;
import com.ecopizza.dtos.PasswordResetConfirmRequestDTO;
import com.ecopizza.dtos.PasswordResetConfirmResponseDTO;
import com.ecopizza.dtos.PasswordResetRequestDTO;
import com.ecopizza.dtos.PasswordResetRequestResultDTO;
import com.ecopizza.dtos.PasswordResetVerifyRequestDTO;
import com.ecopizza.dtos.PasswordResetVerifyResponseDTO;
import com.ecopizza.dtos.SetPasswordRequestDTO;
import com.ecopizza.dtos.UsuarioDTO;
import com.ecopizza.exceptions.BadRequestException;
import com.ecopizza.mappers.UsuarioMapper;
import com.ecopizza.models.Usuario;
import com.ecopizza.security.JwtTokenProvider;
import com.ecopizza.services.AuditLogService;
import com.ecopizza.services.AuthRecoveryAndGoogleService;
import com.ecopizza.services.SessionLogService;
import com.ecopizza.services.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

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
    public ResponseEntity<ApiResponse<GoogleConfigDTO>> obtenerConfiguracionGoogle() {
        return ResponseEntity.ok(ApiResponse.success(
                authRecoveryAndGoogleService.obtenerConfiguracionGoogle()
        ));
    }

    @PostMapping("/auth/google")
    public ResponseEntity<ApiResponse<GoogleAuthResponseDTO>> loginConGoogle(
            @RequestBody GoogleAuthRequestDTO payload,
            HttpServletRequest request) {

        GoogleAuthResultDTO result = authRecoveryAndGoogleService.autenticarConGoogle(payload);
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
                    "Cuenta creada automáticamente con Google OAuth: " + user.getEmail()
            );
        }
        auditLogService.registrarLogin(user.getId().intValue(), obtenerIP(request), obtenerUserAgent(request));
        sessionLogService.crearSesion(user.getId().intValue(), token, obtenerIP(request), obtenerUserAgent(request));

        GoogleAuthResponseDTO responseDto = new GoogleAuthResponseDTO(
                token,
                refreshToken,
                usuarioMapper.toDto(user),
                result.newlyCreated(),
                Boolean.FALSE.equals(user.getPasswordSet())
        );

        return ResponseEntity.ok(ApiResponse.success(
                responseDto,
                "Inicio de sesión con Google exitoso. Bienvenido " + user.getName()
        ));
    }

    @PostMapping("/usuarios/me/vincular-google")
    public ResponseEntity<ApiResponse<UsuarioDTO>> vincularCuentaConGoogle(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) GoogleAuthRequestDTO payload) {

        String token = authHeader != null ? authHeader.replace("Bearer ", "").trim() : "";
        Long userId = jwtTokenProvider.getIdFromToken(token);
        if (userId == null) {
            throw new BadRequestException("Token inválido");
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
            @RequestBody SetPasswordRequestDTO body,
            HttpServletRequest request) {

        String token = authHeader != null ? authHeader.replace("Bearer ", "").trim() : "";
        Long userId = jwtTokenProvider.getIdFromToken(token);
        if (userId == null) {
            throw new BadRequestException("Token inválido");
        }

        String nuevaPassword = body != null ? body.password() : null;
        Usuario actualizado = authRecoveryAndGoogleService.definirPasswordCuentaGoogle(userId, nuevaPassword);
        auditLogService.registrarActualizacion(
                actualizado.getId().intValue(),
                "Usuario",
                actualizado.getId().intValue(),
                "{\"passwordSet\":false}",
                "{\"passwordSet\":true}",
                "Usuario definió contraseña para su cuenta de Google (" + obtenerIP(request) + ")"
        );
        return ResponseEntity.ok(ApiResponse.success(
                usuarioMapper.toDto(actualizado),
                "Contraseña configurada correctamente. Ahora también puedes iniciar sesión con tu correo y contraseña."
        ));
    }

    @PostMapping("/recuperar-password/solicitar")
    public ResponseEntity<ApiResponse<PasswordResetRequestResultDTO>> solicitarRecuperacionPassword(
            @RequestBody PasswordResetRequestDTO body,
            HttpServletRequest request) {

        String email = body != null ? body.email() : null;
        PasswordResetRequestResultDTO result = authRecoveryAndGoogleService.solicitarRecuperacionPassword(email);

        return ResponseEntity.ok(ApiResponse.success(result, result.message()));
    }

    @PostMapping("/recuperar-password/verificar-codigo")
    public ResponseEntity<ApiResponse<PasswordResetVerifyResponseDTO>> verificarCodigoRecuperacion(
            @RequestBody PasswordResetVerifyRequestDTO body) {

        String email = body != null ? body.email() : null;
        String code = body != null ? body.code() : null;
        boolean valid = authRecoveryAndGoogleService.verificarCodigoRecuperacion(email, code);

        return ResponseEntity.ok(ApiResponse.success(
                new PasswordResetVerifyResponseDTO(valid),
                "Código verificado correctamente."
        ));
    }

    @PostMapping("/recuperar-password/confirmar")
    public ResponseEntity<ApiResponse<PasswordResetConfirmResponseDTO>> confirmarRecuperacionPassword(
            @RequestBody PasswordResetConfirmRequestDTO body,
            HttpServletRequest request) {

        String email = body != null ? body.email() : null;
        String code = body != null ? body.code() : null;
        String newPassword = body != null ? body.newPassword() : null;

        Usuario actualizado = authRecoveryAndGoogleService.confirmarRecuperacionPassword(email, code, newPassword);

        auditLogService.registrarActualizacion(
                actualizado.getId().intValue(),
                "Usuario",
                actualizado.getId().intValue(),
                "{\"password\":\"***\"}",
                "{\"password\":\"reset_by_otp\"}",
                "Contraseña restablecida mediante código OTP enviado al correo (" + obtenerIP(request) + ")"
        );

        return ResponseEntity.ok(ApiResponse.success(
                new PasswordResetConfirmResponseDTO(true, actualizado.getEmail()),
                "Tu contraseña ha sido restablecida con éxito. Ya puedes iniciar sesión."
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
