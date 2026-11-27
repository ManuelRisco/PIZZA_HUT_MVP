package com.example.dtos;

import com.example.models.Usuario;
import java.time.LocalDateTime;

public class UsuarioDTO {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String dni;
    private Boolean ageVerified;
    private String ruc;
    private String rucRazonSocial;
    private String rucDireccion;
    private Boolean rucVerified;
    private Usuario.Role role;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private LocalDateTime lastLogin;
    private Integer loginAttempts;
    private LocalDateTime lockedUntil;
    private Boolean googleLinked;
    private String authProvider;
    private Boolean passwordSet;

    public UsuarioDTO() {}

    public UsuarioDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.name = usuario.getName();
        this.phone = usuario.getPhone();
        this.dni = usuario.getDni();
        this.ageVerified = Boolean.TRUE.equals(usuario.getAgeVerified());
        this.ruc = usuario.getRuc();
        this.rucRazonSocial = usuario.getRucRazonSocial();
        this.rucDireccion = usuario.getRucDireccion();
        this.rucVerified = Boolean.TRUE.equals(usuario.getRucVerified());
        this.role = usuario.getRole();
        this.createdAt = usuario.getCreatedAt();
        this.deletedAt = usuario.getDeletedAt();
        this.lastLogin = usuario.getLastLogin();
        this.loginAttempts = usuario.getLoginAttempts();
        this.lockedUntil = usuario.getLockedUntil();
        this.googleLinked = Boolean.TRUE.equals(usuario.getGoogleLinked());
        this.authProvider = usuario.getAuthProvider() != null ? usuario.getAuthProvider() : "LOCAL";
        this.passwordSet = usuario.getPasswordSet() == null ? true : usuario.getPasswordSet();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public Boolean getAgeVerified() { return ageVerified; }
    public void setAgeVerified(Boolean ageVerified) { this.ageVerified = ageVerified; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getRucRazonSocial() { return rucRazonSocial; }
    public void setRucRazonSocial(String rucRazonSocial) { this.rucRazonSocial = rucRazonSocial; }

    public String getRucDireccion() { return rucDireccion; }
    public void setRucDireccion(String rucDireccion) { this.rucDireccion = rucDireccion; }

    public Boolean getRucVerified() { return rucVerified; }
    public void setRucVerified(Boolean rucVerified) { this.rucVerified = rucVerified; }

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

    public Boolean getGoogleLinked() { return googleLinked; }
    public void setGoogleLinked(Boolean googleLinked) { this.googleLinked = googleLinked; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public Boolean getPasswordSet() { return passwordSet; }
    public void setPasswordSet(Boolean passwordSet) { this.passwordSet = passwordSet; }
}
