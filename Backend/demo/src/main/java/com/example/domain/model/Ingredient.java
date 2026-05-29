package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ingredients")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal extraCost = BigDecimal.ZERO;

    @Column(name = "isAvailable", nullable = false)
    private boolean isAvailable = true;

    @Column(nullable = false)
    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime updatedAt = LocalDateTime.now();

    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PizzaIngredient> pizzaIngredients;

    // Constructor vacío
    public Ingredient() {}

    // Constructor completo
    public Ingredient(Integer id, String name, java.math.BigDecimal extraCost, boolean isAvailable,
                      LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt,
                      List<PizzaIngredient> pizzaIngredients) {
        this.id = id;
        this.name = name;
        this.extraCost = extraCost;
        this.isAvailable = isAvailable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.pizzaIngredients = pizzaIngredients;
    }

    // Getters y setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public java.math.BigDecimal getExtraCost() { return extraCost; }
    public void setExtraCost(java.math.BigDecimal extraCost) { this.extraCost = extraCost; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { this.isAvailable = available; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public List<PizzaIngredient> getPizzaIngredients() { return pizzaIngredients; }
    public void setPizzaIngredients(List<PizzaIngredient> pizzaIngredients) { this.pizzaIngredients = pizzaIngredients; }
}