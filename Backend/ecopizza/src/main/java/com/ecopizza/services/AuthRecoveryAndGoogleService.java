package com.ecopizza.services;

import com.ecopizza.dtos.GoogleAuthRequestDTO;
import com.ecopizza.dtos.GoogleAuthResultDTO;
import com.ecopizza.dtos.GoogleConfigDTO;
import com.ecopizza.dtos.GoogleIdentityPayloadDTO;
import com.ecopizza.dtos.PasswordResetRequestResultDTO;
import com.ecopizza.exceptions.BadRequestException;
import com.ecopizza.exceptions.ResourceNotFoundException;
import com.ecopizza.models.Usuario;
import com.ecopizza.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRecoveryAndGoogleService {

    private static final Logger log = LoggerFactory.getLogger(AuthRecoveryAndGoogleService.class);
    private static final int OTP_EXPIRATION_MINUTES = 15;
    private static final int MAX_OTP_ATTEMPTS = 5;

    private static final class InMemoryOtpEntry {
        final String codeHash;
        final LocalDateTime expiresAt;
        int attempts;
        boolean used;

        InMemoryOtpEntry(String codeHash, LocalDateTime expiresAt) {
            this.codeHash = codeHash;
            this.expiresAt = expiresAt;
            this.attempts = 0;
            this.used = false;
        }
    }

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, InMemoryOtpEntry> memoryOtpStore = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Value("${app.google.client-secret:}")
    private String googleClientSecret;

    @Value("${app.google.allow-dev-mode:true}")
    private boolean allowGoogleDevMode;

    public AuthRecoveryAndGoogleService(UsuarioRepository usuarioRepository,
                                        EmailService emailService,
                                        JdbcTemplate jdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.jdbcTemplate = jdbcTemplate;
    }

    private boolean isGoogleCloudConfigured() {
        String cleanClientId = googleClientId != null ? googleClientId.trim() : "";
        return !cleanClientId.isEmpty() && !cleanClientId.startsWith("TU_GOOGLE_CLIENT_ID");
    }

    public GoogleConfigDTO obtenerConfiguracionGoogle() {
        String cleanClientId = googleClientId != null ? googleClientId.trim() : "";
        boolean secretConfigured = googleClientSecret != null && !googleClientSecret.isBlank();
        boolean cloudConfigured = isGoogleCloudConfigured();
        return new GoogleConfigDTO(
                cloudConfigured ? cleanClientId : "",
                cloudConfigured,
                secretConfigured,
                allowGoogleDevMode && !cloudConfigured
        );
    }

    @Transactional
    public PasswordResetRequestResultDTO solicitarRecuperacionPassword(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new BadRequestException("Ingrese el correo electr\u00f3nico de su cuenta.");
        }
        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("El correo ingresado no se encuentra registrado en el sistema."));

        if (usuario.getDeletedAt() != null) {
            throw new BadRequestException("Esta cuenta se encuentra inactiva. Contacte al administrador.");
        }

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        String codeHash = passwordEncoder.encode(otpCode);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES);

        memoryOtpStore.put(normalizedEmail, new InMemoryOtpEntry(codeHash, expiresAt));
        persistOtpInDatabaseQuietly(usuario.getId(), normalizedEmail, codeHash, expiresAt);

        boolean sent = emailService.sendPasswordResetEmail(
                usuario.getEmail(),
                usuario.getName(),
                otpCode,
                OTP_EXPIRATION_MINUTES
        );

        String masked = maskEmail(normalizedEmail);
        String msg = sent
                ? "Enviamos un c\u00f3digo de 6 d\u00edgitos a " + masked + ". Revisa tu bandeja de entrada."
                : "Se gener\u00f3 la solicitud de recuperaci\u00f3n para " + masked + ".";

        return new PasswordResetRequestResultDTO(sent, masked, OTP_EXPIRATION_MINUTES, msg);
    }

    public boolean verificarCodigoRecuperacion(String rawEmail, String rawCode) {
        if (rawEmail == null || rawEmail.isBlank() || rawCode == null || rawCode.isBlank()) {
            throw new BadRequestException("Debe proporcionar su correo y el c\u00f3digo de 6 d\u00edgitos.");
        }
        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        String code = rawCode.trim();
        if (!code.matches("^\\d{6}$")) {
            throw new BadRequestException("El c\u00f3digo de verificaci\u00f3n debe tener exactamente 6 d\u00edgitos num\u00e9ricos.");
        }

        validateOtpInternal(normalizedEmail, code, false);
        return true;
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public Usuario confirmarRecuperacionPassword(String rawEmail, String rawCode, String newPassword) {
        if (rawEmail == null || rawEmail.isBlank() || rawCode == null || rawCode.isBlank()) {
            throw new BadRequestException("Debe completar el correo y el c\u00f3digo de verificaci\u00f3n.");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BadRequestException("La nueva contrase\u00f1a debe tener entre 6 y 20 caracteres.");
        }

        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        String code = rawCode.trim();
        if (!code.matches("^\\d{6}$")) {
            throw new BadRequestException("El c\u00f3digo de verificaci\u00f3n debe tener 6 d\u00edgitos.");
        }

        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        if (usuario.getDeletedAt() != null) {
            throw new BadRequestException("Usuario inactivo. Contacte al administrador.");
        }

        validateOtpInternal(normalizedEmail, code, true);

        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuario.setPasswordSet(true);
        usuario.incrementTokenVersion();
        usuario.setRefreshToken(null);
        usuario.setRefreshTokenExpiry(null);
        usuario.resetLoginAttempts();

        Usuario actualizado = usuarioRepository.saveAndFlush(usuario);
        emailService.sendPasswordChangedConfirmationEmailAsync(actualizado.getEmail(), actualizado.getName());
        return actualizado;
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public GoogleAuthResultDTO autenticarConGoogle(GoogleAuthRequestDTO payload) {
        if (payload == null) {
            throw new BadRequestException("Datos de autenticaci\u00f3n de Google incompletos.");
        }

        String idToken = firstNonBlank(payload.credential(), payload.idToken());
        String accessToken = payload.accessToken();
        String directEmail = payload.email();
        String directName = payload.name();

        GoogleIdentityPayloadDTO googleUser;
        if (idToken != null && !idToken.isBlank()) {
            googleUser = verifyGoogleIdToken(idToken.trim());
        } else if (accessToken != null && !accessToken.isBlank()) {
            googleUser = verifyGoogleAccessToken(accessToken.trim());
        } else if (allowGoogleDevMode && !isGoogleCloudConfigured() && directEmail != null && !directEmail.isBlank()) {
            String normalized = directEmail.trim().toLowerCase(Locale.ROOT);
            if (!normalized.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new BadRequestException("Seleccione o ingrese un correo de Google v\u00e1lido.");
            }
            String resolvedName = (directName != null && !directName.isBlank())
                    ? directName.trim()
                    : deriveDisplayNameFromEmail(normalized);
            googleUser = new GoogleIdentityPayloadDTO(normalized, resolvedName, "google-local-" + normalized, true);
        } else {
            throw new BadRequestException("No se recibi\u00f3 un token de Google v\u00e1lido.");
        }

        if (!googleUser.verified() || googleUser.email() == null || googleUser.email().isBlank()) {
            throw new BadRequestException("La cuenta de Google no posee un correo electr\u00f3nico verificado.");
        }

        String email = googleUser.email().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> existenteOpt = usuarioRepository.findByEmail(email);

        if (existenteOpt.isPresent()) {
            Usuario existente = existenteOpt.get();
            if (existente.getDeletedAt() != null) {
                throw new BadRequestException("Su cuenta se encuentra inactiva. Contacte al administrador.");
            }
            existente.setGoogleLinked(true);
            existente.setAuthProvider("GOOGLE");
            if (existente.getPasswordSet() == null) {
                existente.setPasswordSet(true);
            }
            existente.resetLoginAttempts();
            existente.setLastLogin(LocalDateTime.now());
            Usuario guardado = usuarioRepository.saveAndFlush(existente);
            return new GoogleAuthResultDTO(guardado, false);
        }

        // Crear nuevo usuario CUSTOMER automaticamente con su cuenta de Google (sin password local definida)
        Usuario nuevo = new Usuario();
        nuevo.setEmail(email);
        nuevo.setRole(Usuario.Role.CUSTOMER);
        nuevo.setName(resolveUniqueUserName(googleUser.name(), email));
        nuevo.setPasswordHash(passwordEncoder.encode("GoogleOAuth#" + UUID.randomUUID()));
        nuevo.setGoogleLinked(true);
        nuevo.setAuthProvider("GOOGLE");
        nuevo.setPasswordSet(false);
        nuevo.setTokenVersion(1);
        nuevo.setLoginAttempts(0);
        nuevo.setLastLogin(LocalDateTime.now());

        Usuario creado = usuarioRepository.saveAndFlush(nuevo);
        log.info("Nuevo usuario registrado autom\u00e1ticamente mediante Google OAuth: {}", email);
        return new GoogleAuthResultDTO(creado, true);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public Usuario definirPasswordCuentaGoogle(Long userId, String newPassword) {
        if (userId == null) {
            throw new BadRequestException("Usuario no identificado.");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BadRequestException("La contrase\u00f1a debe tener entre 6 y 20 caracteres.");
        }
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        if (usuario.getDeletedAt() != null) {
            throw new BadRequestException("Usuario inactivo.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuario.setPasswordSet(true);
        usuario.resetLoginAttempts();
        return usuarioRepository.saveAndFlush(usuario);
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public Usuario vincularCuentaConGoogle(Long userId, GoogleAuthRequestDTO payload) {
        if (userId == null) {
            throw new BadRequestException("Usuario no identificado.");
        }
        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        if (usuario.getDeletedAt() != null) {
            throw new BadRequestException("La cuenta se encuentra inactiva.");
        }

        if (payload != null) {
            String idToken = firstNonBlank(payload.credential(), payload.idToken());
            String accessToken = payload.accessToken();
            GoogleIdentityPayloadDTO googleUser = null;
            if (idToken != null && !idToken.isBlank()) {
                googleUser = verifyGoogleIdToken(idToken.trim());
            } else if (accessToken != null && !accessToken.isBlank()) {
                googleUser = verifyGoogleAccessToken(accessToken.trim());
            }
            if (googleUser != null && googleUser.email() != null && !googleUser.email().isBlank()) {
                String gEmail = googleUser.email().trim().toLowerCase(Locale.ROOT);
                if (!gEmail.equalsIgnoreCase(usuario.getEmail())) {
                    throw new BadRequestException(
                            "El correo de Google (" + gEmail + ") debe coincidir con el correo de tu perfil (" + usuario.getEmail() + ").");
                }
            }
        }

        usuario.setGoogleLinked(true);
        usuario.setAuthProvider("GOOGLE");
        return usuarioRepository.saveAndFlush(usuario);
    }

    private GoogleIdentityPayloadDTO verifyGoogleIdToken(String idToken) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                    + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new BadRequestException("El token de Google no es v\u00e1lido o ha expirado.");
            }
            JsonNode json = objectMapper.readTree(res.body());
            String aud = json.path("aud").asText("");
            String cleanClientId = googleClientId != null ? googleClientId.trim() : "";
            if (!cleanClientId.isEmpty() && !cleanClientId.startsWith("TU_GOOGLE_CLIENT_ID") && !cleanClientId.equals(aud)) {
                throw new BadRequestException("El token de Google no corresponde a este cliente OAuth.");
            }
            String email = json.path("email").asText("").trim().toLowerCase(Locale.ROOT);
            boolean verified = "true".equalsIgnoreCase(json.path("email_verified").asText("false"));
            String name = json.path("name").asText("").trim();
            if (name.isEmpty()) {
                name = deriveDisplayNameFromEmail(email);
            }
            String sub = json.path("sub").asText("");
            return new GoogleIdentityPayloadDTO(email, name, sub, verified);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al verificar id_token con Google: {}", ex.getMessage());
            throw new BadRequestException("No se pudo verificar la credencial con los servidores de Google.");
        }
    }

    private GoogleIdentityPayloadDTO verifyGoogleAccessToken(String accessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                throw new BadRequestException("El token de acceso de Google es inv\u00e1lido o ha expirado.");
            }
            JsonNode json = objectMapper.readTree(res.body());
            String email = json.path("email").asText("").trim().toLowerCase(Locale.ROOT);
            boolean verified = json.path("email_verified").asBoolean(true);
            String name = json.path("name").asText("").trim();
            if (name.isEmpty()) {
                name = deriveDisplayNameFromEmail(email);
            }
            String sub = json.path("sub").asText("");
            return new GoogleIdentityPayloadDTO(email, name, sub, verified);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al consultar userinfo de Google: {}", ex.getMessage());
            throw new BadRequestException("No se pudo obtener el perfil de Google.");
        }
    }

    private void validateOtpInternal(String email, String rawCode, boolean consume) {
        // 1. Verificar primero en tabla SQL Server si esta disponible
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT TOP 1 id, codeHash, expiresAt, attempts, used
                    FROM dbo.password_reset_tokens
                    WHERE email = ? AND used = 0
                    ORDER BY id DESC
                    """, email);
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                long tokenId = ((Number) row.get("id")).longValue();
                String codeHash = (String) row.get("codeHash");
                Timestamp expiresTs = (Timestamp) row.get("expiresAt");
                int attempts = ((Number) row.get("attempts")).intValue();

                if (attempts >= MAX_OTP_ATTEMPTS) {
                    throw new BadRequestException("Demasiados intentos fallidos. Solicite un nuevo c\u00f3digo de recuperaci\u00f3n.");
                }
                if (expiresTs == null || expiresTs.toLocalDateTime().isBefore(LocalDateTime.now())) {
                    throw new BadRequestException("El c\u00f3digo de verificaci\u00f3n ha expirado. Solicite uno nuevo.");
                }
                if (!passwordEncoder.matches(rawCode, codeHash)) {
                    int nextAttempts = attempts + 1;
                    jdbcTemplate.update(
                            "UPDATE dbo.password_reset_tokens SET attempts = ?, used = CASE WHEN ? >= ? THEN 1 ELSE used END WHERE id = ?",
                            nextAttempts, nextAttempts, MAX_OTP_ATTEMPTS, tokenId
                    );
                    int remaining = Math.max(0, MAX_OTP_ATTEMPTS - nextAttempts);
                    throw new BadRequestException("C\u00f3digo incorrecto. Intentos restantes: " + remaining);
                }
                if (consume) {
                    jdbcTemplate.update(
                            "UPDATE dbo.password_reset_tokens SET used = 1, usedAt = SYSDATETIME() WHERE id = ?",
                            tokenId
                    );
                    memoryOtpStore.remove(email);
                }
                return;
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ignored) {
            // Fallback a memoria si la tabla no existe en entorno de test
        }

        // 2. Fallback en memoria
        InMemoryOtpEntry entry = memoryOtpStore.get(email);
        if (entry == null || entry.used) {
            throw new BadRequestException("No existe un c\u00f3digo de recuperaci\u00f3n activo para este correo. Solicite uno nuevo.");
        }
        if (entry.attempts >= MAX_OTP_ATTEMPTS) {
            entry.used = true;
            throw new BadRequestException("Demasiados intentos fallidos. Solicite un nuevo c\u00f3digo.");
        }
        if (entry.expiresAt.isBefore(LocalDateTime.now())) {
            entry.used = true;
            throw new BadRequestException("El c\u00f3digo de verificaci\u00f3n ha expirado. Solicite uno nuevo.");
        }
        if (!passwordEncoder.matches(rawCode, entry.codeHash)) {
            entry.attempts++;
            if (entry.attempts >= MAX_OTP_ATTEMPTS) {
                entry.used = true;
            }
            int remaining = Math.max(0, MAX_OTP_ATTEMPTS - entry.attempts);
            throw new BadRequestException("C\u00f3digo incorrecto. Intentos restantes: " + remaining);
        }
        if (consume) {
            entry.used = true;
            memoryOtpStore.remove(email);
        }
    }

    private void persistOtpInDatabaseQuietly(Long userId, String email, String codeHash, LocalDateTime expiresAt) {
        try {
            jdbcTemplate.update(
                    "DELETE FROM dbo.password_reset_tokens WHERE expiresAt < DATEADD(DAY, -1, SYSDATETIME())"
            );
            jdbcTemplate.update(
                    "UPDATE dbo.password_reset_tokens SET used = 1 WHERE email = ? AND used = 0",
                    email
            );
            jdbcTemplate.update("""
                    INSERT INTO dbo.password_reset_tokens (userId, email, codeHash, expiresAt, attempts, used)
                    VALUES (?, ?, ?, ?, 0, 0)
                    """,
                    userId, email, codeHash, Timestamp.valueOf(expiresAt)
            );
        } catch (Exception ex) {
            log.debug("Persistencia en dbo.password_reset_tokens omitida (usando respaldo en memoria): {}", ex.getMessage());
        }
    }

    private String resolveUniqueUserName(String rawName, String email) {
        String base = (rawName != null && !rawName.isBlank()) ? rawName.trim() : deriveDisplayNameFromEmail(email);
        base = base.replaceAll("[^a-zA-Z\u00e1\u00e9\u00ed\u00f3\u00fa\u00c1\u00c9\u00cd\u00d3\u00da\u00f1\u00d1\\s]", "").trim();
        if (base.length() < 4) {
            base = base + " Usuario";
        }
        if (base.length() > 45) {
            base = base.substring(0, 45).trim();
        }
        String candidate = base;
        int suffix = 1;
        while (usuarioRepository.existsByName(candidate)) {
            candidate = base + " " + suffix;
            suffix++;
        }
        return candidate;
    }

    private String deriveDisplayNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "Usuario Google";
        }
        String local = email.substring(0, email.indexOf('@'))
                .replace('.', ' ')
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\d+", " ")
                .trim();
        if (local.isBlank()) {
            return "Usuario Google";
        }
        String[] parts = local.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) {
                    sb.append(p.substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        String result = sb.toString().trim();
        return result.length() >= 4 ? result : result + " Google";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.substring(0, 2) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
