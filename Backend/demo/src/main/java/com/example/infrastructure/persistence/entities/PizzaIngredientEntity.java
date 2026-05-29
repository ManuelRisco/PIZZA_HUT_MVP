package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pizza_ingredients")
public class PizzaIngredientEntity {

    @EmbeddedId
    private PizzaIngredientIdEntity id = new PizzaIngredientIdEntity();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("pizzaId")
    @JoinColumn(name = "pizzaId")
    private PizzaEntity pizza;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredientId")
    private IngredientEntity ingredient;

    @Column(nullable = false)
    private boolean isDefault = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PizzaIngredientEntity() {}

    public PizzaIngredientEntity(PizzaIngredientIdEntity id, PizzaEntity pizza, IngredientEntity ingredient, boolean isDefault, LocalDateTime createdAt) {
        this.id = id;
        this.pizza = pizza;
        this.ingredient = ingredient;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
    }

    // ==== Getters y Setters ====
    public PizzaIngredientIdEntity getId() { return id; }
    public void setId(PizzaIngredientIdEntity id) { this.id = id; }

    public PizzaEntity getPizza() { return pizza; }
    public void setPizza(PizzaEntity pizza) { this.pizza = pizza; }

    public IngredientEntity getIngredient() { return ingredient; }
    public void setIngredient(IngredientEntity ingredient) { this.ingredient = ingredient; }

    public boolean getIsDefault() { return isDefault; }
    public void setIsDefault(boolean isDefault) { this.isDefault = isDefault; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}