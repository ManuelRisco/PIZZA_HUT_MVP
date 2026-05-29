package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.example.domain.model.Usuario;

@Entity
@Table(name = "users")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Usuario.Role role = Usuario.Role.CUSTOMER;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ==== Ciclo de vida ====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==== Constructores ====
    public UsuarioEntity() {}

    // ==== Conversión entre dominio y entidad ====
    public static UsuarioEntity fromDomain(Usuario usuario) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.id = usuario.getId();
        entity.email = usuario.getEmail();
        entity.passwordHash = usuario.getPasswordHash();
        entity.role = usuario.getRole();
        entity.name = usuario.getName();
        entity.phone = usuario.getPhone();
        entity.createdAt = usuario.getCreatedAt();
        entity.updatedAt = usuario.getUpdatedAt();
        entity.deletedAt = usuario.getDeletedAt();
        return entity;
    }

    public Usuario toDomain() {
        Usuario usuario = new Usuario();
        usuario.setId(this.id);
        usuario.setEmail(this.email);
        usuario.setPasswordHash(this.passwordHash);
        usuario.setRole(this.role);
        usuario.setName(this.name);
        usuario.setPhone(this.phone);
        usuario.setCreatedAt(this.createdAt);
        usuario.setUpdatedAt(this.updatedAt);
        usuario.setDeletedAt(this.deletedAt);
        return usuario;
    }

    // ==== Getters y Setters ====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Usuario.Role getRole() { return role; }
    public void setRole(Usuario.Role role) { this.role = role; }

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
}