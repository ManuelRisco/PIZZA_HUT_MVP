package com.example.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites")
public class Favorite {

    @EmbeddedId
    private FavoriteId id;
    private LocalDateTime createdAt;

    // ==== Constructores ====
    public Favorite() {
    }

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

