package com.ecopizza.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ingredients")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(nullable = false, length = 20)
    private String unit = "g";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPortionQuantity = new BigDecimal("50.00");

    @Column(nullable = false)
    private boolean allowAsExtra = true;

    @Column(nullable = false)
    @JsonIgnore
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @JsonIgnore
    private LocalDateTime updatedAt = LocalDateTime.now();

    @JsonIgnore
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "ingredient", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PizzaIngredient> pizzaIngredients;

    public Ingredient(Integer id, String name, BigDecimal extraCost, boolean isAvailable,
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

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null)
            this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
