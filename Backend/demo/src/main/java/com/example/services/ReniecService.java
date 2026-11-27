package com.example.services;

import com.example.dtos.DniLookupDTO;
import com.example.dtos.RucLookupDTO;
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
    private final ConcurrentHashMap<String, RucLookupDTO> memoryRucCache = new ConcurrentHashMap<>();

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

    /**
     * Consulta un RUC peruano de 11 dígitos usando caché en memoria, caché SQL Server (dbo.sunat_ruc_cache),
     * Decolecta API SUNAT y respaldo público, guardando automáticamente cada resultado en caché y BD.
     */
    public RucLookupDTO consultarRuc(String rawRuc) {
        if (rawRuc == null || !rawRuc.trim().matches("^(10|15|16|17|20)\\d{9}$")) {
            throw new BadRequestException("El RUC debe contener exactamente 11 dígitos numéricos válidos (ej. 10... o 20...).");
        }
        String ruc = rawRuc.trim();

        RucLookupDTO cached = memoryRucCache.get(ruc);
        if (cached != null) {
            return cached;
        }

        RucLookupDTO dbCached = findRucInDbCache(ruc);
        if (dbCached != null) {
            memoryRucCache.put(ruc, dbCached);
            return dbCached;
        }

        RucLookupDTO decolectaResult = tryDecolectaRucApi(ruc);
        if (decolectaResult != null) {
            saveRucToCache(decolectaResult);
            return decolectaResult;
        }

        RucLookupDTO fallbackResult = tryPublicRucFallback(ruc);
        if (fallbackResult != null) {
            saveRucToCache(fallbackResult);
            return fallbackResult;
        }

        RucLookupDTO userDbResult = findRucInUsersTable(ruc);
        if (userDbResult != null) {
            saveRucToCache(userDbResult);
            return userDbResult;
        }

        throw new ResourceNotFoundException("No se encontraron datos en SUNAT para el RUC " + ruc + ". Puede escribir la razón social manualmente.");
    }

    /**
     * Guarda o actualiza el RUC del usuario en la tabla users junto con la declaración obligatoria
     * de que el RUC le pertenece (o está autorizado) y es mayor de 18 años.
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public Usuario configurarRucUsuario(Long userId,
                                        String rawRuc,
                                        Boolean rucVerified,
                                        String razonSocial,
                                        String direccionFiscal) {
        if (userId == null) {
            throw new BadRequestException("Usuario no autenticado.");
        }
        if (rawRuc == null || !rawRuc.trim().matches("^(10|15|16|17|20)\\d{9}$")) {
            throw new BadRequestException("El RUC debe contener exactamente 11 dígitos numéricos válidos (ej. 10... o 20...).");
        }
        if (!Boolean.TRUE.equals(rucVerified)) {
            throw new BadRequestException("Debe confirmar que es mayor de 18 años y titular o representante autorizado del RUC ingresado.");
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        String cleanRuc = rawRuc.trim();
        String cleanRazon = razonSocial != null && !razonSocial.isBlank()
                ? razonSocial.trim().toUpperCase(Locale.ROOT)
                : null;
        String cleanDireccion = direccionFiscal != null && !direccionFiscal.isBlank()
                ? direccionFiscal.trim().toUpperCase(Locale.ROOT)
                : null;

        if (cleanRazon == null) {
            try {
                RucLookupDTO lookup = consultarRuc(cleanRuc);
                if (lookup != null) {
                    cleanRazon = lookup.razonSocial();
                    if (cleanDireccion == null && lookup.direccion() != null && !lookup.direccion().isBlank()) {
                        cleanDireccion = lookup.direccion();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        usuario.setRuc(cleanRuc);
        if (cleanRazon != null) {
            usuario.setRucRazonSocial(cleanRazon);
        }
        if (cleanDireccion != null) {
            usuario.setRucDireccion(cleanDireccion);
        }
        usuario.setRucVerified(true);
        usuario.setAgeVerified(true);

        if (cleanRazon != null) {
            saveRucToCache(new RucLookupDTO(
                    cleanRuc,
                    cleanRazon,
                    cleanDireccion != null ? cleanDireccion : "",
                    "ACTIVO",
                    "HABIDO",
                    "",
                    "",
                    "",
                    "USER_CONFIG"
            ));
        }

        return usuarioRepository.saveAndFlush(usuario);
    }

    private RucLookupDTO tryDecolectaRucApi(String ruc) {
        if (decolectaApiUrl == null || decolectaApiUrl.isBlank() || decolectaApiToken == null || decolectaApiToken.isBlank()) {
            return null;
        }
        String baseUrl = decolectaApiUrl.trim().replaceAll("/+$", "");
        String token = decolectaApiToken.trim();
        String[] authHeaders = new String[]{
            "Bearer " + token,
            token.startsWith("sk_") ? "Bearer " + token : "Bearer sk_" + token
        };
        String[] paths = new String[]{
            "/sunat/ruc?numero=" + ruc,
            "/sunat/ruc/full?numero=" + ruc
        };

        for (String path : paths) {
            for (String authHeader : authHeaders) {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + path))
                            .header("Authorization", authHeader)
                            .header("Referer", "https://api.decolecta.com")
                            .header("Accept", "application/json")
                            .header("User-Agent", "EcoPizzaMVP/1.0")
                            .timeout(Duration.ofSeconds(8))
                            .GET()
                            .build();

                    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                        RucLookupDTO parsed = parseRucJson(ruc, objectMapper.readTree(resp.body()), "DECOLECTA_SUNAT");
                        if (parsed != null) {
                            log.info("RUC {} consultado exitosamente desde Decolecta API: {}", ruc, parsed.razonSocial());
                            return parsed;
                        }
                    } else {
                        log.debug("Decolecta API devolvió HTTP {} para RUC {} ({}): {}", resp.statusCode(), ruc, path, resp.body());
                    }
                } catch (Exception ex) {
                    log.debug("Aviso al consultar Decolecta API para RUC {}: {}", ruc, ex.getMessage());
                }
            }
        }
        return null;
    }

    private RucLookupDTO tryPublicRucFallback(String ruc) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.apis.net.pe/v1/ruc?numero=" + ruc))
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                RucLookupDTO parsed = parseRucJson(ruc, objectMapper.readTree(resp.body()), "SUNAT_PERU");
                if (parsed != null) {
                    log.info("RUC {} consultado exitosamente mediante servicio SUNAT secundario: {}", ruc, parsed.razonSocial());
                    return parsed;
                }
            }
        } catch (Exception ex) {
            log.debug("Aviso en consulta SUNAT secundaria para RUC {}: {}", ruc, ex.getMessage());
        }
        return null;
    }

    private RucLookupDTO parseRucJson(String ruc, JsonNode json, String source) {
        if (json == null || json.isMissingNode()) return null;
        String razonSocial = firstNonBlank(
                json.path("razon_social").asText(null),
                json.path("razonSocial").asText(null),
                json.path("nombre").asText(null),
                json.path("nombre_o_razon_social").asText(null)
        );
        if (razonSocial == null || razonSocial.isBlank()) {
            return null;
        }
        String distrito = firstNonBlank(json.path("distrito").asText(null));
        String provincia = firstNonBlank(json.path("provincia").asText(null));
        String departamento = firstNonBlank(json.path("departamento").asText(null));
        String direccion = firstNonBlank(
                json.path("direccion").asText(null),
                json.path("direccion_completa").asText(null),
                json.path("domicilio_fiscal").asText(null)
        );
        if (direccion != null && (direccion.equals("-") || direccion.equals("--"))) {
            direccion = null;
        }
        if (direccion != null && distrito != null && !direccion.toUpperCase(Locale.ROOT).contains(distrito.toUpperCase(Locale.ROOT))) {
            StringBuilder fullAddr = new StringBuilder(direccion.trim());
            fullAddr.append(" - ").append(distrito.trim());
            if (provincia != null && !provincia.isBlank()) {
                fullAddr.append(" - ").append(provincia.trim());
            }
            if (departamento != null && !departamento.isBlank()) {
                fullAddr.append(" - ").append(departamento.trim());
            }
            direccion = fullAddr.toString();
        }
        String estado = firstNonBlank(json.path("estado").asText(null));
        String condicion = firstNonBlank(json.path("condicion").asText(null));

        return new RucLookupDTO(
                ruc,
                razonSocial.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT),
                direccion != null ? direccion.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT) : "",
                estado != null ? estado.trim().toUpperCase(Locale.ROOT) : "",
                condicion != null ? condicion.trim().toUpperCase(Locale.ROOT) : "",
                distrito != null ? distrito.trim().toUpperCase(Locale.ROOT) : "",
                provincia != null ? provincia.trim().toUpperCase(Locale.ROOT) : "",
                departamento != null ? departamento.trim().toUpperCase(Locale.ROOT) : "",
                source
        );
    }

    private RucLookupDTO findRucInDbCache(String ruc) {
        try {
            List<RucLookupDTO> rows = jdbcTemplate.query("""
                SELECT ruc, razon_social, direccion, estado, condicion, distrito, provincia, departamento
                FROM dbo.sunat_ruc_cache
                WHERE ruc = ?
                """, (rs, rowNum) -> new RucLookupDTO(
                    rs.getString("ruc"),
                    rs.getString("razon_social"),
                    rs.getString("direccion"),
                    rs.getString("estado"),
                    rs.getString("condicion"),
                    rs.getString("distrito"),
                    rs.getString("provincia"),
                    rs.getString("departamento"),
                    "CACHE_SUNAT"
            ), ruc);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private RucLookupDTO findRucInUsersTable(String ruc) {
        try {
            List<RucLookupDTO> rows = jdbcTemplate.query("""
                SELECT TOP 1 ruc, ruc_razon_social, ruc_direccion
                FROM dbo.users
                WHERE ruc = ? AND ruc_razon_social IS NOT NULL AND LTRIM(RTRIM(ruc_razon_social)) <> ''
                """, (rs, rowNum) -> new RucLookupDTO(
                    ruc,
                    rs.getString("ruc_razon_social").trim().toUpperCase(Locale.ROOT),
                    rs.getString("ruc_direccion") != null ? rs.getString("ruc_direccion").trim().toUpperCase(Locale.ROOT) : "",
                    "ACTIVO",
                    "HABIDO",
                    "",
                    "",
                    "",
                    "DB_USERS"
            ), ruc);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveRucToCache(RucLookupDTO res) {
        if (res == null || res.ruc() == null || res.razonSocial() == null || res.razonSocial().isBlank()) return;
        memoryRucCache.put(res.ruc(), res);
        try {
            jdbcTemplate.update("""
                IF EXISTS (SELECT 1 FROM dbo.sunat_ruc_cache WHERE ruc = ?)
                    UPDATE dbo.sunat_ruc_cache
                    SET razon_social = ?, direccion = ?, estado = ?, condicion = ?, distrito = ?, provincia = ?, departamento = ?, updated_at = SYSDATETIME()
                    WHERE ruc = ?
                ELSE
                    INSERT INTO dbo.sunat_ruc_cache (ruc, razon_social, direccion, estado, condicion, distrito, provincia, departamento)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    res.ruc(),
                    res.razonSocial(), res.direccion(), res.estado(), res.condicion(), res.distrito(), res.provincia(), res.departamento(), res.ruc(),
                    res.ruc(), res.razonSocial(), res.direccion(), res.estado(), res.condicion(), res.distrito(), res.provincia(), res.departamento()
            );
        } catch (Exception ignored) {
        }
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
