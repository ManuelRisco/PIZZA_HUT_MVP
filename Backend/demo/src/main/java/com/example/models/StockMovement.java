package com.example.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType itemType;

    private Integer ingredientId;
    private Integer extraId;
    private Integer sizeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType movementType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal previousStock;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newStock;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(length = 30)
    private String referenceType;

    private Integer referenceId;

    @Column(length = 255)
    private String reason;

    private Integer userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum ItemType {
        INGREDIENT,
        EXTRA,
        SIZE
    }

    public enum MovementType {
        PURCHASE,
        SALE,
        LOSS,
        ADJUSTMENT,
        RETURN
    }

    public StockMovement() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (unitCost == null) {
            unitCost = BigDecimal.ZERO;
        }
        if (totalCost == null) {
            totalCost = BigDecimal.ZERO;
        }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

    public Integer getExtraId() { return extraId; }
    public void setExtraId(Integer extraId) { this.extraId = extraId; }

    public Integer getSizeId() { return sizeId; }
    public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }

    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPreviousStock() { return previousStock; }
    public void setPreviousStock(BigDecimal previousStock) { this.previousStock = previousStock; }

    public BigDecimal getNewStock() { return newStock; }
    public void setNewStock(BigDecimal newStock) { this.newStock = newStock; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

