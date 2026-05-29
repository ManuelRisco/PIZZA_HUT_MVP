package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.example.domain.model.Ingredient;

@Entity
@Table(name = "ingredients")
public class IngredientEntity {

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
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PizzaIngredientEntity> pizzaIngredients;

    // ==== Ciclo de vida ====
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==== Constructores ====
    public IngredientEntity() {}

    // ==== Conversión entre entidad y dominio ====
    public static IngredientEntity fromDomain(Ingredient ingredient) {
        IngredientEntity entity = new IngredientEntity();
        entity.id = ingredient.getId();
        entity.name = ingredient.getName();
        entity.extraCost = ingredient.getExtraCost();
        entity.isAvailable = ingredient.isAvailable();
        entity.createdAt = ingredient.getCreatedAt();
        entity.updatedAt = ingredient.getUpdatedAt();
        entity.deletedAt = ingredient.getDeletedAt();
        return entity;
    }

    public Ingredient toDomain() {
        return new Ingredient(
                this.id,
                this.name,
                this.extraCost,
                this.isAvailable,
                this.createdAt,
                this.updatedAt,
                this.deletedAt,
                null // Evita bucles infinitos por relación con PizzaIngredient
        );
    }

    // ==== Getters y Setters ====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getExtraCost() { return extraCost; }
    public void setExtraCost(BigDecimal extraCost) { this.extraCost = extraCost; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { this.isAvailable = available; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public List<PizzaIngredientEntity> getPizzaIngredients() { return pizzaIngredients; }
    public void setPizzaIngredients(List<PizzaIngredientEntity> pizzaIngredients) { this.pizzaIngredients = pizzaIngredients; }
}