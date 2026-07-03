package com.example.dtos;

import com.example.models.Usuario;
import jakarta.validation.constraints.*;

public class UsuarioCreateDTO {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String name;
    
    private String phone;
    private Usuario.Role role;

    // Constructor vacío
    public UsuarioCreateDTO() {
        // Constructor vacío para serialización
    }

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
