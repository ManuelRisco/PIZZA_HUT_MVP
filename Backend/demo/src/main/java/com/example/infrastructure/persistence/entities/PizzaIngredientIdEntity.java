package com.example.infrastructure.persistence.entities;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PizzaIngredientIdEntity implements Serializable {

    private Integer pizzaId;
    private Integer ingredientId;

    public PizzaIngredientIdEntity() {}

    public PizzaIngredientIdEntity(Integer pizzaId, Integer ingredientId) {
        this.pizzaId = pizzaId;
        this.ingredientId = ingredientId;
    }

    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PizzaIngredientIdEntity that)) return false;
        return Objects.equals(pizzaId, that.pizzaId) &&
               Objects.equals(ingredientId, that.ingredientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pizzaId, ingredientId);
    }
}