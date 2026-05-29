package com.example.domain.dto;

import com.example.domain.model.Favorite;

public class FavoriteDTO {
    private Integer userId;
    private Integer pizzaId;

    // Constructor vacío
    public FavoriteDTO() {}

    // Constructor desde entidad
    public FavoriteDTO(Favorite favorite) {
        this.userId = favorite.getId().getUserId();
        this.pizzaId = favorite.getId().getPizzaId();
    }

    // Getters y Setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
}
