package com.example.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ID = "id";
    private static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_TYPE_REFRESH = "refresh";

    private final SecretKey jwtSecret;

    public JwtTokenProvider(@Value("${app.jwtSecret}") String jwtSecretString) {
        this.jwtSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretString));
    }

    @Value("${app.jwtExpirationInMs:900000}") // 15 minutos por defecto (900,000 ms)
    private int jwtExpirationInMs;

    @Value("${app.jwtRefreshExpirationInMs:86400000}") // 24 horas por defecto (86,400,000 ms)
    private int jwtRefreshExpirationInMs;

    public String generateToken(String email, String role, String name) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_EMAIL, email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecret)
                .compact();
    }

    public String generateToken(String email, String role, String name, Long id) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_ID, id)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_EMAIL, email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecret)
                .compact();
    }
    
    // Método sobrecargado con tokenVersion
    public String generateToken(String email, String role, String name, Long id, Integer tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_ID, id)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion) // Agregar versión del token
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecret)
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get(CLAIM_ROLE, String.class);
    }

    public String getNameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get(CLAIM_NAME, String.class);
    }

    public Long getIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object idClaim = claims.get(CLAIM_ID);
        if (idClaim instanceof Integer) {
            return ((Integer) idClaim).longValue();
        } else if (idClaim instanceof Long) {
            return (Long) idClaim;
        }
        return null;
    }
    
    public Integer getTokenVersionFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object versionClaim = claims.get(CLAIM_TOKEN_VERSION);
        if (versionClaim instanceof Integer) {
            return (Integer) versionClaim;
        }
        return null; // Tokens antiguos sin versión
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateRefreshToken(String email, String role, String name, Long id) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationInMs);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_ID, id)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_NAME, name)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_TYPE, CLAIM_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtSecret)
                .compact();
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return CLAIM_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecret)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
