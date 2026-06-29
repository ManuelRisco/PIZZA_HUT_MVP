package com.example.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonIgnore  // Ocultar el hash de la contrase\u00f1a en las respuestas JSON
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER; // Valor por defecto

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    // ==== Campos para JWT y Seguridad ====
    @Column(name = "refresh_token", length = 512)
    @JsonIgnore
    private String refreshToken;

    @Column(name = "refresh_token_expiry")
    @JsonIgnore
    private LocalDateTime refreshTokenExpiry;
    
    @Column(name = "token_version", nullable = false)
    @JsonIgnore
    private Integer tokenVersion = 1; // Versi\u00f3n del token, incrementa al cambiar datos cr\u00edticos

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "login_attempts", nullable = false)
    @JsonIgnore
    private Integer loginAttempts = 0;

    @Column(name = "locked_until")
    @JsonIgnore
    private LocalDateTime lockedUntil;

    // ==== Campos de auditor\u00eda ====
    @Column(name = "created_at")
    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime deletedAt;

    // Constructor vac\u00edo
    public Usuario() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    // ==== Getters y Setters JWT y Seguridad ====
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public LocalDateTime getRefreshTokenExpiry() { return refreshTokenExpiry; }
    public void setRefreshTokenExpiry(LocalDateTime refreshTokenExpiry) { this.refreshTokenExpiry = refreshTokenExpiry; }
    
    public Integer getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(Integer tokenVersion) { this.tokenVersion = tokenVersion; }
    
    public void incrementTokenVersion() { 
        this.tokenVersion = (this.tokenVersion == null ? 1 : this.tokenVersion) + 1; 
    }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public Integer getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(Integer loginAttempts) { this.loginAttempts = loginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    // ==== M\u00e9todos de utilidad ====
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isRefreshTokenValid() {
        return refreshToken != null && 
               refreshTokenExpiry != null && 
               refreshTokenExpiry.isAfter(LocalDateTime.now());
    }

    public void incrementLoginAttempts() {
        this.loginAttempts = (this.loginAttempts == null ? 0 : this.loginAttempts) + 1;
        
        // Bloquear cuenta despu\u00e9s de 5 intentos fallidos
        if (this.loginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lockedUntil = null;
    }

    // Enum para los roles del usuario
    public enum Role {
        CUSTOMER, ADMIN
    }
}
