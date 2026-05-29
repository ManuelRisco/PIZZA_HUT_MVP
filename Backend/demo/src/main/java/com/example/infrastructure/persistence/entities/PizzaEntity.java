package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.domain.model.Pizza;
import com.example.domain.model.Category;

@Entity
@Table(name = "pizzas")
public class PizzaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relación con CategoryEntity
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoryId")
    private CategoryEntity category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean isAvailable = true;

    @Column(nullable = false)
    private boolean isPopular = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "pizza", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
    public PizzaEntity() {}

    // ==== Conversión entre dominio ↔ entidad ====
    public static PizzaEntity fromDomain(Pizza pizza) {
        PizzaEntity entity = new PizzaEntity();
        entity.id = pizza.getId();
        if (pizza.getCategory() != null) {
            entity.category = CategoryEntity.fromDomain(pizza.getCategory());
        }
        entity.name = pizza.getName();
        entity.description = pizza.getDescription();
        entity.imageUrl = pizza.getImageUrl();
        entity.price = pizza.getPrice();
        entity.isAvailable = pizza.getIsAvailable();
        entity.isPopular = pizza.getIsPopular();
        entity.createdAt = pizza.getCreatedAt();
        entity.updatedAt = pizza.getUpdatedAt();
        entity.deletedAt = pizza.getDeletedAt();
        return entity;
    }

    public Pizza toDomain() {
        Category domainCategory = (category != null) ? category.toDomain() : null;
        return new Pizza(
            this.id,
            domainCategory,
            this.name,
            this.description,
            this.imageUrl,
            this.price,
            this.isAvailable,
            this.isPopular,
            this.createdAt,
            this.updatedAt,
            this.deletedAt,
            null // evita cargar ingredientes recursivamente
        );
    }

    // ==== Getters y Setters ====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public CategoryEntity getCategory() { return category; }
    public void setCategory(CategoryEntity category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }

    public boolean getIsPopular() { return isPopular; }
    public void setIsPopular(boolean isPopular) { this.isPopular = isPopular; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public List<PizzaIngredientEntity> getPizzaIngredients() { return pizzaIngredients; }
    public void setPizzaIngredients(List<PizzaIngredientEntity> pizzaIngredients) { this.pizzaIngredients = pizzaIngredients; }
}