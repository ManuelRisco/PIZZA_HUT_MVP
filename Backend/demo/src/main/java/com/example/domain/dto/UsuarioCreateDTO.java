package com.example.domain.dto;

import com.example.domain.model.Usuario;

public class UsuarioCreateDTO {
    private String email;
    private String password;
    private String name;
    private String phone;
    private Usuario.Role role;

    // Constructor vacío
    public UsuarioCreateDTO() {}

    // Getters y setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Usuario.Role getRole() { return role; }
    public void setRole(Usuario.Role role) { this.role = role; }
}