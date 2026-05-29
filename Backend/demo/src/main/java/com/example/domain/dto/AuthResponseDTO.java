package com.example.domain.dto;

public class AuthResponseDTO {
    private String token;
    private UsuarioDTO usuario;
    private String message;

    // Constructor vacío
    public AuthResponseDTO() {}

    // Constructor con parámetros
    public AuthResponseDTO(String token, UsuarioDTO usuario, String message) {
        this.token = token;
        this.usuario = usuario;
        this.message = message;
    }

    // Getters y setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UsuarioDTO getUsuario() { return usuario; }
    public void setUsuario(UsuarioDTO usuario) { this.usuario = usuario; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}