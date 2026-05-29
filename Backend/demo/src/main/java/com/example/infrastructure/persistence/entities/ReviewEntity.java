package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.example.domain.model.Review;

@Entity
@Table(name = "reviews", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "orderId"})
})
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Integer orderId;  // Cambiado de pizzaId a orderId

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Boolean active = true;  // Por defecto está activa

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ==== Ciclo de vida ====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==== Constructores ====
    public ReviewEntity() {}

    // ==== Conversión entre dominio y entidad ====
    public static ReviewEntity fromDomain(Review review) {
        ReviewEntity entity = new ReviewEntity();
        entity.id = review.getId();
        entity.userId = review.getUserId();
        entity.orderId = review.getOrderId();
        entity.rating = review.getRating();
        entity.comment = review.getComment();
        entity.active = review.getActive() != null ? review.getActive() : true;
        // No establecer createdAt/updatedAt aquí, dejar que @PrePersist y @PreUpdate lo manejen
        if (review.getCreatedAt() != null) {
            entity.createdAt = review.getCreatedAt();
        }
        if (review.getUpdatedAt() != null) {
            entity.updatedAt = review.getUpdatedAt();
        }
        return entity;
    }

    public Review toDomain() {
        return new Review(
            this.id,
            this.userId,
            this.orderId,
            this.rating,
            this.comment,
            this.active,
            this.createdAt,
            this.updatedAt
        );
    }

    // ==== Getters y Setters ====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
