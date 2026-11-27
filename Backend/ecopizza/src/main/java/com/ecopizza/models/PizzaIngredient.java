package com.ecopizza.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pizza_ingredients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PizzaIngredient {

    @EmbeddedId
    private PizzaIngredientId id = new PizzaIngredientId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pizzaId")
    @JoinColumn(name = "pizzaId")
    @JsonIgnore
    private Pizza pizza;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredientId")
    private Ingredient ingredient;

    @Column(nullable = false)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private boolean isDefault = true;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = new BigDecimal("50.00");

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PizzaIngredient(PizzaIngredientId id, Pizza pizza, Ingredient ingredient, boolean isDefault, LocalDateTime createdAt) {
        this.id = id;
        this.pizza = pizza;
        this.ingredient = ingredient;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.quantity == null || this.quantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantity = new BigDecimal("50.00");
        }
    }
}
