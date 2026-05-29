package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pizzas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Pizza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoryId")
    private Category category;

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
    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime updatedAt = LocalDateTime.now();

    @JsonIgnore  // Ocultar metadatos internos
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "pizza", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PizzaIngredient> pizzaIngredients;

    // Constructor vacío
    public Pizza() {}

    // Constructor completo
    public Pizza(Integer id, Category category, String name, String description, String imageUrl, java.math.BigDecimal price,
                 boolean isAvailable, boolean isPopular, LocalDateTime createdAt, LocalDateTime updatedAt,
                 LocalDateTime deletedAt, List<PizzaIngredient> pizzaIngredients) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.isAvailable = isAvailable;
        this.isPopular = isPopular;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.pizzaIngredients = pizzaIngredients;
    }

    // Getters y setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }

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

    public List<PizzaIngredient> getPizzaIngredients() { return pizzaIngredients; }
    public void setPizzaIngredients(List<PizzaIngredient> pizzaIngredients) { this.pizzaIngredients = pizzaIngredients; }
}