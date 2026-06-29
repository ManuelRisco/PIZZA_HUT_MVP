package com.example.dtos;

import com.example.models.Review;
import java.time.LocalDateTime;

public class ReviewDTO {
    private Integer id;
    private Integer userId;
    private Integer orderId;  // Cambiado de pizzaId a orderId
    private Integer rating;
    private String comment;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor vac\u00edo
    public ReviewDTO() {}

    // Constructor desde entidad
    public ReviewDTO(Review review) {
        this.id = review.getId();
        this.userId = review.getUserId();
        this.orderId = review.getOrderId();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.active = review.getActive();
        this.createdAt = review.getCreatedAt();
        this.updatedAt = review.getUpdatedAt();
    }

    // Getters y Setters
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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
