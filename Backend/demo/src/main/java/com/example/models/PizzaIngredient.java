package com.example.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pizza_ingredients")
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
    private boolean isDefault = true;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = new BigDecimal("50.00");

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructor vacío
    public PizzaIngredient() {}

    // Constructor original preservado por compatibilidad
    public PizzaIngredient(PizzaIngredientId id, Pizza pizza, Ingredient ingredient, boolean isDefault, LocalDateTime createdAt) {
        this.id = id;
        this.pizza = pizza;
        this.ingredient = ingredient;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    // Getters y setters
    public PizzaIngredientId getId() {
        return id;
    }

    public void setId(PizzaIngredientId id) {
        this.id = id;
    }

    public Pizza getPizza() {
        return pizza;
    }

    public void setPizza(Pizza pizza) {
        this.pizza = pizza;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.quantity == null || this.quantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantity = new BigDecimal("50.00");
        }
    }
}
