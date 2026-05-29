package com.example.domain.model;

import java.time.LocalDateTime;

public class Favorite {

    private FavoriteId id;
    private LocalDateTime createdAt;

    // ==== Constructores ====
    public Favorite() {}

    public Favorite(FavoriteId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    // ==== Getters y Setters ====
    public FavoriteId getId() {
        return id;
    }

    public void setId(FavoriteId id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
