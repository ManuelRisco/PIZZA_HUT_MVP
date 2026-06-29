package com.example.dtos;

import com.example.models.Usuario;
import java.time.LocalDateTime;

public class UsuarioDTO {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private Usuario.Role role;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private LocalDateTime lastLogin;
    private Integer loginAttempts;
    private LocalDateTime lockedUntil;

    // Constructor vac\u00edo
    public UsuarioDTO() {}

    // Constructor desde entidad (sin informaci\u00f3n sensible)
    public UsuarioDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.name = usuario.getName();
        this.phone = usuario.getPhone();
        this.role = usuario.getRole();
        this.createdAt = usuario.getCreatedAt();
        this.deletedAt = usuario.getDeletedAt();
        this.lastLogin = usuario.getLastLogin();
        this.loginAttempts = usuario.getLoginAttempts();
        this.lockedUntil = usuario.getLockedUntil();
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Usuario.Role getRole() { return role; }
    public void setRole(Usuario.Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public Integer getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(Integer loginAttempts) { this.loginAttempts = loginAttempts; }
    
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
}
