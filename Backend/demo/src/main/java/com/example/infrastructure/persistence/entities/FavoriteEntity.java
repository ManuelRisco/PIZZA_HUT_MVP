package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.example.domain.model.Favorite;
import com.example.domain.model.FavoriteId;

@Entity
@Table(name = "favorites")
public class FavoriteEntity {

    @EmbeddedId
    private FavoriteIdEntity id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ==== Ciclo de vida ====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==== Constructores ====
    public FavoriteEntity() {}

    // ==== Conversión entre dominio y entidad ====
    public static FavoriteEntity fromDomain(Favorite favorite) {
        FavoriteEntity entity = new FavoriteEntity();
        entity.id = new FavoriteIdEntity(
            favorite.getId().getUserId(),
            favorite.getId().getPizzaId()
        );
        entity.createdAt = favorite.getCreatedAt();
        return entity;
    }

    public Favorite toDomain() {
        return new Favorite(
            new FavoriteId(
                this.id.getUserId(),
                this.id.getPizzaId()
            ),
            this.createdAt
        );
    }

    // ==== Getters y Setters ====
    public FavoriteIdEntity getId() {
        return id;
    }

    public void setId(FavoriteIdEntity id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
