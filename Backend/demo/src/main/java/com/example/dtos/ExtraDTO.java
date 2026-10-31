package com.example.dtos;

import com.example.models.Extra;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class ExtraDTO {

    private Integer id;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private String category;
    private Boolean isAvailable;
    private Integer displayOrder;
    private Integer stockQuantity;
    private Integer minStock;
    private BigDecimal unitCost;
    private BigDecimal profitMargin;
    private boolean lowStock;
    private boolean outOfStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor vacío
    public ExtraDTO() {}

    // Constructor desde entidad
    public ExtraDTO(Extra extra) {
        this.id = extra.getId();
        this.name = extra.getName();
        this.description = extra.getDescription();
        this.imageUrl = extra.getImageUrl();
        this.price = extra.getPrice();
        this.category = extra.getCategory() != null ? extra.getCategory().name() : null;
        this.isAvailable = extra.getIsAvailable();
        this.displayOrder = extra.getDisplayOrder() != null ? extra.getDisplayOrder() : 0;
        this.stockQuantity = extra.getStockQuantity() != null ? extra.getStockQuantity() : 0;
        this.minStock = extra.getMinStock() != null ? extra.getMinStock() : 5;
        this.unitCost = extra.getUnitCost() != null ? extra.getUnitCost() : BigDecimal.ZERO;
        if (this.price != null && this.price.compareTo(BigDecimal.ZERO) > 0) {
            this.profitMargin = this.price.subtract(this.unitCost)
                    .multiply(new BigDecimal("100"))
                    .divide(this.price, 2, RoundingMode.HALF_UP);
        } else {
            this.profitMargin = BigDecimal.ZERO;
        }
        this.outOfStock = this.stockQuantity <= 0;
        this.lowStock = !this.outOfStock && this.stockQuantity <= this.minStock;
        this.createdAt = extra.getCreatedAt();
        this.updatedAt = extra.getUpdatedAt();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getProfitMargin() { return profitMargin; }
    public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }

    public boolean isLowStock() { return lowStock; }
    public void setLowStock(boolean lowStock) { this.lowStock = lowStock; }

    public boolean isOutOfStock() { return outOfStock; }
    public void setOutOfStock(boolean outOfStock) { this.outOfStock = outOfStock; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
