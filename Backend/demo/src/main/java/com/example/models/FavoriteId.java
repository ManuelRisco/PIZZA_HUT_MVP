package com.example.models;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

import java.util.Objects;

@Embeddable
public class FavoriteId implements Serializable {

    private Integer userId;
    private Integer pizzaId;

    // ==== Constructores ====
    public FavoriteId() {
    }

    public FavoriteId(Integer userId, Integer pizzaId) {
        this.userId = userId;
        this.pizzaId = pizzaId;
    }

    // ==== Getters y Setters ====
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getPizzaId() {
        return pizzaId;
    }

    public void setPizzaId(Integer pizzaId) {
        this.pizzaId = pizzaId;
    }

    // ==== equals y hashCode ====
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FavoriteId that = (FavoriteId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(pizzaId, that.pizzaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, pizzaId);
    }
}

