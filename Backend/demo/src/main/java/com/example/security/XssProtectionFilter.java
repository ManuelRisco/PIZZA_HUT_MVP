package com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Filtro de prevencion de Inyeccion XSS (Cross-Site Scripting) en parametros URL, cabeceras y cuerpos JSON.
 */
@Component
public class XssProtectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(XssProtectionFilter.class);

    private static final List<Pattern> XSS_DETECTION_PATTERNS = List.of(
            Pattern.compile("<\\s*script\\b[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("</\\s*script\\s*>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("data\\s*:\\s*text/html", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<\\s*(iframe|object|embed|applet|form)\\b[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<\\s*svg\\b[^>]*\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<\\s*img\\b[^>]*\\bonerror\\s*=", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("\\bon(load|error|click|mouseover|focus|blur|submit|change|keyup|keydown|animationstart|toggle)\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("document\\s*\\.\\s*(cookie|write|domain)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\beval\\s*\\(", Pattern.CASE_INSENSITIVE)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Verificar URI y Query String
        String rawUri = request.getRequestURI();
        if (rawUri != null && containsXssPayload(decodeQuietly(rawUri))) {
            rejectXssRequest(request, response, "URI");
            return;
        }
        String rawQuery = request.getQueryString();
        if (rawQuery != null && containsXssPayload(decodeQuietly(rawQuery))) {
            rejectXssRequest(request, response, "query string");
            return;
        }

        // 2. Verificar parametros de formulario / query
        Map<String, String[]> paramMap = request.getParameterMap();
        if (paramMap != null && !paramMap.isEmpty()) {
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                if (containsXssPayload(entry.getKey())) {
                    rejectXssRequest(request, response, "nombre de parametro");
                    return;
                }
                String[] values = entry.getValue();
                if (values != null) {
                    for (String val : values) {
                        if (containsXssPayload(val)) {
                            rejectXssRequest(request, response, "parametro '" + entry.getKey() + "'");
                            return;
                        }
                    }
                }
            }
        }

        // 3. Verificar cuerpo JSON si la peticion es application/json
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
            byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());
            if (bodyBytes.length > 0) {
                String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
                if (containsXssPayload(bodyStr)) {
                    rejectXssRequest(request, response, "cuerpo JSON");
                    return;
                }
            }
            CachedBodyXssRequestWrapper wrappedRequest = new CachedBodyXssRequestWrapper(request, bodyBytes);
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        filterChain.doFilter(new CachedBodyXssRequestWrapper(request, null), response);
    }

    public static boolean containsXssPayload(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String normalized = input.replace("\0", "");
        for (Pattern pattern : XSS_DETECTION_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    public static String sanitizeValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String clean = value.replace("\0", "");
        for (Pattern pattern : XSS_DETECTION_PATTERNS) {
            clean = pattern.matcher(clean).replaceAll("");
        }
        return clean;
    }

    private String decodeQuietly(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private void rejectXssRequest(HttpServletRequest request, HttpServletResponse response, String location) throws IOException {
        log.warn("Ataque XSS bloqueado desde IP {} en {} {} (ubicacion: {})",
                request.getRemoteAddr(), request.getMethod(), request.getRequestURI(), location);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Solicitud bloqueada por seguridad: se detect\\u00f3 c\\u00f3digo HTML/JavaScript no permitido (XSS).\",\"data\":null}");
    }

    private static final class CachedBodyXssRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyXssRequestWrapper(HttpServletRequest request, byte[] cachedBody) {
            super(request);
            this.cachedBody = cachedBody;
        }

        @Override
        public String getParameter(String name) {
            return sanitizeValue(super.getParameter(name));
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitizeValue(values[i]);
            }
            return sanitized;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> rawMap = super.getParameterMap();
            if (rawMap == null || rawMap.isEmpty()) {
                return rawMap;
            }
            Map<String, String[]> cleanMap = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> entry : rawMap.entrySet()) {
                String[] vals = entry.getValue();
                if (vals == null) {
                    cleanMap.put(entry.getKey(), null);
                } else {
                    String[] sanitized = new String[vals.length];
                    for (int i = 0; i < vals.length; i++) {
                        sanitized[i] = sanitizeValue(vals[i]);
                    }
                    cleanMap.put(entry.getKey(), sanitized);
                }
            }
            return cleanMap;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (cachedBody == null) {
                return super.getInputStream();
            }
            final ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Lectura sincrona en memoria
                }

                @Override
                public int read() {
                    return bais.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if (cachedBody == null) {
                return super.getReader();
            }
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
