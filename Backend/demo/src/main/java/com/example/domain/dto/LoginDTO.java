package com.example.domain.dto;

public class LoginDTO {
    private String email;
    private String password;

    // Constructor vacío
    public LoginDTO() {}

    // Constructor con parámetros
    public LoginDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters y setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}