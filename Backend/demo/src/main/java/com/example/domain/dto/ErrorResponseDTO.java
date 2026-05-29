package com.example.domain.dto;

import java.time.LocalDateTime;

public class ErrorResponseDTO {
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String path;

    // Constructor vacío
    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor con parámetros
    public ErrorResponseDTO(String message, int status, String path) {
        this.message = message;
        this.status = status;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    // Getters y setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}