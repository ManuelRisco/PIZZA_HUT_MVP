package com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro Anti-DDoS y Fuerza Bruta basado en ventana deslizante / Token Bucket por IP y categoria de ruta.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_TRACKED_BUCKETS = 10_000;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.security.rate-limit.auth-per-minute:15}")
    private int authLimitPerMinute;

    @Value("${app.security.rate-limit.write-per-minute:60}")
    private int writeLimitPerMinute;

    @Value("${app.security.rate-limit.general-per-minute:240}")
    private int generalLimitPerMinute;

    private final ConcurrentHashMap<String, RateBucket> buckets = new ConcurrentHashMap<>();

    private static final class RateBucket {
        private volatile long windowStartMs;
        private final AtomicInteger count;

        RateBucket(long now) {
            this.windowStartMs = now;
            this.count = new AtomicInteger(0);
        }

        synchronized int incrementAndGetRemaining(long now, int limit) {
            if (now - windowStartMs >= WINDOW_MS) {
                windowStartMs = now;
                count.set(0);
            }
            int current = count.incrementAndGet();
            return limit - current;
        }

        long secondsUntilReset(long now) {
            long elapsed = now - windowStartMs;
            long remainingMs = Math.max(1000L, WINDOW_MS - elapsed);
            return Math.max(1L, (remainingMs + 999L) / 1000L);
        }

        boolean isStale(long now) {
            return (now - windowStartMs) > (WINDOW_MS * 2);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!rateLimitEnabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // No aplicar rate limit a recursos estaticos de imagenes
        if ("GET".equalsIgnoreCase(method) && path != null && path.startsWith("/uploads/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String tier = resolveTier(path, method);
        int limit = resolveLimit(tier);
        String clientIp = extractClientIp(request);
        String bucketKey = clientIp + ":" + tier;
        long now = System.currentTimeMillis();

        cleanupBucketsIfNeeded(now);

        RateBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new RateBucket(now));
        int remaining = bucket.incrementAndGetRemaining(now, limit);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));

        if (remaining < 0) {
            long retryAfterSeconds = bucket.secondsUntilReset(now);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            log.warn("Alerta Anti-DDoS / RateLimit: IP {} excedio el limite ({}) en tier {} para {} {}",
                    clientIp, limit, tier, method, path);

            String jsonBody = "{\"success\":false,\"message\":\"Demasiadas solicitudes desde tu direcci\\u00f3n IP. Por favor, espera "
                    + retryAfterSeconds + " segundos antes de volver a intentarlo.\",\"data\":null}";
            response.getWriter().write(jsonBody);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveTier(String path, String method) {
        if (path == null) {
            return "GENERAL";
        }
        if (path.startsWith("/api/ingresar")
                || path.startsWith("/api/registro")
                || path.startsWith("/api/recuperar-password")
                || path.startsWith("/api/sunat/dni")
                || (path.startsWith("/api/sunat/boleta") && "POST".equalsIgnoreCase(method))
                || (path.startsWith("/api/auth/google") && "POST".equalsIgnoreCase(method))) {
            return "AUTH";
        }
        if ("POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)) {
            return "WRITE";
        }
        return "GENERAL";
    }

    private int resolveLimit(String tier) {
        return switch (tier) {
            case "AUTH" -> Math.max(5, authLimitPerMinute);
            case "WRITE" -> Math.max(15, writeLimitPerMinute);
            default -> Math.max(60, generalLimitPerMinute);
        };
    }

    private void cleanupBucketsIfNeeded(long now) {
        if (buckets.size() > MAX_TRACKED_BUCKETS) {
            buckets.entrySet().removeIf(entry -> entry.getValue().isStale(now));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            int commaIdx = xff.indexOf(',');
            return (commaIdx > 0 ? xff.substring(0, commaIdx) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
