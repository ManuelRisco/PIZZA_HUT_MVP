package com.example.dtos;

import com.example.models.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SizeDTO {
    private Integer id;
    private String name;
    private BigDecimal extraCost;
    private BigDecimal recipeMultiplier;
    private Integer stockQuantity;
    private Integer minStock;
    private String description;
    private Integer displayOrder;
    private LocalDateTime deletedAt;

    // Constructor vacío
    public SizeDTO() {}

    // Constructor desde entidad
    public SizeDTO(Size size) {
        this.id = size.getId();
        this.name = size.getName();
        this.extraCost = size.getExtraCost();
        this.recipeMultiplier = size.getRecipeMultiplier() != null ? size.getRecipeMultiplier() : BigDecimal.ONE;
        this.stockQuantity = size.getStockQuantity();
        this.minStock = size.getMinStock();
        this.description = size.getDescription();
        this.displayOrder = size.getDisplayOrder();
        this.deletedAt = size.getDeletedAt();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getExtraCost() { return extraCost; }
    public void setExtraCost(BigDecimal extraCost) { this.extraCost = extraCost; }

    public BigDecimal getRecipeMultiplier() { return recipeMultiplier; }
    public void setRecipeMultiplier(BigDecimal recipeMultiplier) { this.recipeMultiplier = recipeMultiplier; }

    public Integer getStockQuantity() { return stockQuantity != null ? stockQuantity : 0; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getMinStock() { return minStock != null ? minStock : 0; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
