package com.example.domain.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PizzaIngredientId implements Serializable {

    private Integer pizzaId;
    private Integer ingredientId;

    // Constructor vacío
    public PizzaIngredientId() {}

    // Constructor completo
    public PizzaIngredientId(Integer pizzaId, Integer ingredientId) {
        this.pizzaId = pizzaId;
        this.ingredientId = ingredientId;
    }

    // Getters y setters
    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PizzaIngredientId that)) return false;
        return Objects.equals(pizzaId, that.pizzaId) &&
               Objects.equals(ingredientId, that.ingredientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pizzaId, ingredientId);
    }
}
