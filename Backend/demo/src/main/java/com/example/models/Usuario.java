package com.example.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonIgnore
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "dni", length = 15)
    private String dni;

    @Column(name = "age_verified", nullable = false)
    private Boolean ageVerified = false;

    @Column(name = "refresh_token", length = 512)
    @JsonIgnore
    private String refreshToken;

    @Column(name = "refresh_token_expiry")
    @JsonIgnore
    private LocalDateTime refreshTokenExpiry;

    @Column(name = "token_version", nullable = false)
    @JsonIgnore
    private Integer tokenVersion = 1;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "login_attempts", nullable = false)
    @JsonIgnore
    private Integer loginAttempts = 0;

    @Column(name = "locked_until")
    @JsonIgnore
    private LocalDateTime lockedUntil;

    @Column(name = "google_linked", nullable = false)
    private Boolean googleLinked = false;

    @Column(name = "auth_provider", length = 30)
    private String authProvider = "LOCAL";

    @Column(name = "password_set", nullable = false)
    private Boolean passwordSet = true;

    @Column(name = "created_at")
    @JsonIgnore
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @JsonIgnore
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    @JsonIgnore
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.googleLinked == null) {
            this.googleLinked = false;
        }
        if (this.authProvider == null || this.authProvider.isBlank()) {
            this.authProvider = Boolean.TRUE.equals(this.googleLinked) ? "GOOGLE" : "LOCAL";
        }
        if (this.passwordSet == null) {
            this.passwordSet = true;
        }
        if (this.ageVerified == null) {
            this.ageVerified = (this.dni != null && !this.dni.isBlank());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.ageVerified == null) {
            this.ageVerified = (this.dni != null && !this.dni.isBlank());
        }
    }

    public void incrementTokenVersion() {
        this.tokenVersion = (this.tokenVersion == null ? 1 : this.tokenVersion) + 1;
    }

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
        if (this.loginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lockedUntil = null;
    }

    public enum Role {
        CUSTOMER, ADMIN
    }
}
