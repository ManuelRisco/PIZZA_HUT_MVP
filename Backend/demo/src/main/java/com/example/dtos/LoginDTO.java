package com.example.dtos;

public class LoginDTO {
    private String email;
    private String password;

    // Constructor vac\u00edo
    public LoginDTO() {}

    // Constructor con par\u00e1metros
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
