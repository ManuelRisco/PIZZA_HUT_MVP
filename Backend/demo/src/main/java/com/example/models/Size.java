package com.example.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sizes")
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private BigDecimal extraCost;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal recipeMultiplier = BigDecimal.ONE;
    @Column(nullable = false)
    private Integer stockQuantity = 0;
    @Column(nullable = false)
    private Integer minStock = 0;
    private String description;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // ==== Constructores ====
    public Size() {
    }

    public Size(Integer id, String name, BigDecimal extraCost, String description,
            Integer displayOrder, LocalDateTime createdAt, LocalDateTime updatedAt,
            LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.extraCost = extraCost;
        this.description = description;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    // ==== Getters y Setters ====
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getExtraCost() {
        return extraCost;
    }

    public void setExtraCost(BigDecimal extraCost) {
        this.extraCost = extraCost;
    }

    public BigDecimal getRecipeMultiplier() {
        return recipeMultiplier;
    }

    public void setRecipeMultiplier(BigDecimal recipeMultiplier) {
        this.recipeMultiplier = recipeMultiplier;
    }

    public Integer getStockQuantity() {
        return stockQuantity != null ? stockQuantity : 0;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getMinStock() {
        return minStock != null ? minStock : 0;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (recipeMultiplier == null || recipeMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            recipeMultiplier = BigDecimal.ONE;
        }
        if (displayOrder == null) {
            displayOrder = 1;
        }
        if (extraCost == null) {
            extraCost = BigDecimal.ZERO;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (recipeMultiplier == null || recipeMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            recipeMultiplier = BigDecimal.ONE;
        }
        updatedAt = LocalDateTime.now();
    }
}
