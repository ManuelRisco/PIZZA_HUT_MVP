package com.example.services;

import com.example.dtos.DniLookupDTO;
import com.example.exceptions.BadRequestException;
import com.example.exceptions.ResourceNotFoundException;
import com.example.models.Usuario;
import com.example.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReniecService {

    private static final Logger log = LoggerFactory.getLogger(ReniecService.class);

    private final JdbcTemplate jdbcTemplate;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, DniLookupDTO> memoryDniCache = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${decolecta.api.url:https://api.decolecta.com/v1}")
    private String decolectaApiUrl;

    @Value("${decolecta.api.token:}")
    private String decolectaApiToken;

    public ReniecService(JdbcTemplate jdbcTemplate, UsuarioRepository usuarioRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Consulta un DNI peruano de 8 dígitos usando Decolecta API (configurada en .env)
     * con respaldo en caché SQL Server y consulta RENIEC en vivo para garantizar disponibilidad.
     */
    public DniLookupDTO consultarDni(String rawDni) {
        if (rawDni == null || !rawDni.trim().matches("^\\d{8}$")) {
            throw new BadRequestException("El DNI debe contener exactamente 8 dígitos numéricos.");
        }
        String dni = rawDni.trim();

        // 1. Revisar caché en memoria
        DniLookupDTO cached = memoryDniCache.get(dni);
        if (cached != null) {
            return cached;
        }

        // 2. Revisar caché en SQL Server (dbo.reniec_dni_cache)
        DniLookupDTO dbCached = findInDbCache(dni);
        if (dbCached != null) {
            memoryDniCache.put(dni, dbCached);
            return dbCached;
        }

        // 3. Consultar API primaria configurada en .env (Decolecta)
        DniLookupDTO decolectaResult = tryDecolectaApi(dni);
        if (decolectaResult != null) {
            saveToCache(decolectaResult);
            return decolectaResult;
        }

        // 4. Respaldo automático en vivo RENIEC (eldni.com) por si el token de Decolecta excedió límite
        DniLookupDTO fallbackResult = tryLiveReniecFallback(dni);
        if (fallbackResult != null) {
            saveToCache(fallbackResult);
            return fallbackResult;
        }

        // 5. Revisar si algún usuario registrado en la base de datos ya tiene ese DNI
        DniLookupDTO userDbResult = findInUsersTable(dni);
        if (userDbResult != null) {
            saveToCache(userDbResult);
            return userDbResult;
        }

        throw new ResourceNotFoundException("No se encontraron datos en RENIEC para el DNI " + dni + ". Puede escribir su nombre manualmente.");
    }

    /**
     * Guarda o actualiza el DNI de un usuario en la base de datos junto con la declaración
     * de mayoría de edad (+18) y titularidad del documento.
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public Usuario configurarDniUsuario(Long userId, String rawDni, Boolean ageVerified, String optionalName) {
        if (userId == null) {
            throw new BadRequestException("Usuario no autenticado.");
        }
        if (rawDni == null || !rawDni.trim().matches("^\\d{8}$")) {
            throw new BadRequestException("El DNI debe contener exactamente 8 dígitos numéricos.");
        }
        if (!Boolean.TRUE.equals(ageVerified)) {
            throw new BadRequestException("Debe confirmar que es mayor de 18 años y titular del DNI ingresado.");
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        String cleanDni = rawDni.trim();
        usuario.setDni(cleanDni);
        usuario.setAgeVerified(true);

        if (optionalName != null && !optionalName.isBlank()) {
            String trimmedName = optionalName.trim();
            List<Usuario> conflicts = usuarioRepository.findAll().stream()
                    .filter(u -> u.getName() != null && u.getName().equalsIgnoreCase(trimmedName))
                    .filter(u -> !u.getId().equals(usuario.getId()))
                    .toList();
            if (conflicts.isEmpty()) {
                usuario.setName(trimmedName);
            }
        }
        return usuarioRepository.saveAndFlush(usuario);
    }

    private DniLookupDTO tryDecolectaApi(String dni) {
        if (decolectaApiUrl == null || decolectaApiUrl.isBlank() || decolectaApiToken == null || decolectaApiToken.isBlank()) {
            return null;
        }
        String baseUrl = decolectaApiUrl.trim().replaceAll("/+$", "");
        String token = decolectaApiToken.trim();
        String[] authHeaders = new String[]{
            "Bearer " + token,
            token.startsWith("sk_") ? "Bearer " + token : "Bearer sk_" + token
        };

        for (String authHeader : authHeaders) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/reniec/dni?numero=" + dni))
                        .header("Authorization", authHeader)
                        .header("Referer", "https://api.decolecta.com")
                        .header("Accept", "application/json")
                        .header("User-Agent", "PizzaHutMVP/1.0")
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                    JsonNode json = objectMapper.readTree(resp.body());
                    String nombres = firstNonBlank(
                            json.path("first_name").asText(null),
                            json.path("nombres").asText(null)
                    );
                    String apPaterno = firstNonBlank(
                            json.path("first_last_name").asText(null),
                            json.path("apellidoPaterno").asText(null),
                            json.path("apellido_paterno").asText(null)
                    );
                    String apMaterno = firstNonBlank(
                            json.path("second_last_name").asText(null),
                            json.path("apellidoMaterno").asText(null),
                            json.path("apellido_materno").asText(null)
                    );
                    String fullName = firstNonBlank(
                            json.path("full_name").asText(null),
                            json.path("nombreCompleto").asText(null),
                            json.path("nombre_completo").asText(null)
                    );

                    if ((fullName == null || fullName.isBlank()) && nombres != null) {
                        fullName = (nombres + " " + (apPaterno != null ? apPaterno : "") + " " + (apMaterno != null ? apMaterno : ""))
                                .trim().replaceAll("\\s+", " ");
                    }
                    if (fullName != null && !fullName.isBlank()) {
                        log.info("DNI {} consultado exitosamente desde Decolecta API: {}", dni, fullName);
                        return new DniLookupDTO(
                                dni,
                                nombres != null ? nombres.trim() : fullName.trim(),
                                apPaterno != null ? apPaterno.trim() : "",
                                apMaterno != null ? apMaterno.trim() : "",
                                fullName.trim().replaceAll("\\s+", " "),
                                "DECOLECTA_RENIEC"
                        );
                    }
                } else {
                    log.debug("Decolecta API devolvió HTTP {} para DNI {}: {}", resp.statusCode(), dni, resp.body());
                }
            } catch (Exception ex) {
                log.debug("Aviso al consultar Decolecta API para DNI {}: {}", dni, ex.getMessage());
            }
        }
        return null;
    }

    private DniLookupDTO tryLiveReniecFallback(String dni) {
        try {
            String url = "https://eldni.com/pe/buscar-datos-por-dni";
            HttpRequest getReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> getResp = httpClient.send(getReq, HttpResponse.BodyHandlers.ofString());
            if (getResp.statusCode() != 200 || getResp.body() == null) {
                return null;
            }

            Matcher tokenMatcher = Pattern.compile("name=\"_token\"\\s+value=\"([^\"]+)\"").matcher(getResp.body());
            if (!tokenMatcher.find()) {
                return null;
            }
            String csrfToken = tokenMatcher.group(1);
            List<String> setCookies = getResp.headers().allValues("Set-Cookie");
            String cookieHeader = setCookies.stream()
                    .map(c -> c.split(";", 2)[0])
                    .collect(Collectors.joining("; "));

            String formData = "_token=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8)
                    + "&dni=" + URLEncoder.encode(dni, StandardCharsets.UTF_8);

            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Cookie", cookieHeader)
                    .header("Referer", url)
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();

            HttpResponse<String> postResp = httpClient.send(postReq, HttpResponse.BodyHandlers.ofString());
            if (postResp.statusCode() == 200 && postResp.body() != null) {
                String html = postResp.body();
                String nombres = extractInputValue(html, "nombres");
                String apPaterno = extractInputValue(html, "apellidop");
                String apMaterno = extractInputValue(html, "apellidom");
                String completos = extractInputValue(html, "completos");

                if ((completos == null || completos.isBlank()) && nombres != null && !nombres.isBlank()) {
                    completos = (nombres + " " + (apPaterno != null ? apPaterno : "") + " " + (apMaterno != null ? apMaterno : ""))
                            .trim().replaceAll("\\s+", " ");
                }
                if (completos != null && !completos.isBlank()) {
                    log.info("DNI {} consultado exitosamente mediante servicio RENIEC secundario: {}", dni, completos);
                    return new DniLookupDTO(
                            dni,
                            nombres != null ? nombres.trim() : completos.trim(),
                            apPaterno != null ? apPaterno.trim() : "",
                            apMaterno != null ? apMaterno.trim() : "",
                            completos.trim().replaceAll("\\s+", " "),
                            "RENIEC_PERU"
                    );
                }
            }
        } catch (Exception ex) {
            log.debug("Aviso en consulta RENIEC secundaria para DNI {}: {}", dni, ex.getMessage());
        }
        return null;
    }

    private String extractInputValue(String html, String inputId) {
        Matcher m = Pattern.compile("<input[^>]*id=\"" + Pattern.quote(inputId) + "\"[^>]*value=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE).matcher(html);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private DniLookupDTO findInDbCache(String dni) {
        try {
            List<DniLookupDTO> rows = jdbcTemplate.query("""
                SELECT dni, nombres, apellido_paterno, apellido_materno, nombre_completo
                FROM dbo.reniec_dni_cache
                WHERE dni = ?
                """, (rs, rowNum) -> new DniLookupDTO(
                    rs.getString("dni"),
                    rs.getString("nombres"),
                    rs.getString("apellido_paterno"),
                    rs.getString("apellido_materno"),
                    rs.getString("nombre_completo"),
                    "CACHE_RENIEC"
            ), dni);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private DniLookupDTO findInUsersTable(String dni) {
        try {
            List<DniLookupDTO> rows = jdbcTemplate.query("""
                SELECT TOP 1 dni, name
                FROM dbo.users
                WHERE dni = ? AND name IS NOT NULL AND LTRIM(RTRIM(name)) <> ''
                """, (rs, rowNum) -> {
                String full = rs.getString("name").trim().toUpperCase(Locale.ROOT);
                return new DniLookupDTO(dni, full, "", "", full, "DB_USERS");
            }, dni);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveToCache(DniLookupDTO res) {
        if (res == null || res.dni() == null || res.nombreCompleto() == null) return;
        memoryDniCache.put(res.dni(), res);
        try {
            jdbcTemplate.update("""
                IF EXISTS (SELECT 1 FROM dbo.reniec_dni_cache WHERE dni = ?)
                    UPDATE dbo.reniec_dni_cache
                    SET nombres = ?, apellido_paterno = ?, apellido_materno = ?, nombre_completo = ?, updated_at = SYSDATETIME()
                    WHERE dni = ?
                ELSE
                    INSERT INTO dbo.reniec_dni_cache (dni, nombres, apellido_paterno, apellido_materno, nombre_completo)
                    VALUES (?, ?, ?, ?, ?)
                """,
                    res.dni(),
                    res.nombres(), res.apellidoPaterno(), res.apellidoMaterno(), res.nombreCompleto(), res.dni(),
                    res.dni(), res.nombres(), res.apellidoPaterno(), res.apellidoMaterno(), res.nombreCompleto()
            );
        } catch (Exception ignored) {
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
