package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FavoriteIdEntity implements Serializable {

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Integer pizzaId;

    // ==== Constructores ====
    public FavoriteIdEntity() {}

    public FavoriteIdEntity(Integer userId, Integer pizzaId) {
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FavoriteIdEntity that = (FavoriteIdEntity) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(pizzaId, that.pizzaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, pizzaId);
    }
}
