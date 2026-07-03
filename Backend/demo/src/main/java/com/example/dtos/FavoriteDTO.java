package com.example.dtos;

import com.example.models.Favorite;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class FavoriteDTO {
    
    @NotNull(message = "El ID del usuario es obligatorio")
    private Integer userId;
    
    @NotNull(message = "El ID de la pizza es obligatorio")
    private Integer pizzaId;
    
    private LocalDateTime createdAt;

    public FavoriteDTO() {}

    public FavoriteDTO(Favorite favorite) {
        if (favorite.getId() != null) {
            this.userId = favorite.getId().getUserId();
            this.pizzaId = favorite.getId().getPizzaId();
        }
        this.createdAt = favorite.getCreatedAt();
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
