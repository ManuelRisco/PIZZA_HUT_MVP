package com.example.dtos;

import com.example.models.StockMovement;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockMovementDTO {
    private Integer id;
    private String itemType;
    private Integer ingredientId;
    private Integer extraId;
    private Integer sizeId;
    private String itemName;
    private String unit;
    private String movementType;
    private BigDecimal quantity;
    private BigDecimal previousStock;
    private BigDecimal newStock;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String referenceType;
    private Integer referenceId;
    private String reason;
    private Integer userId;
    private String userName;
    private LocalDateTime createdAt;

    public StockMovementDTO() {}

    public StockMovementDTO(StockMovement sm) {
        this.id = sm.getId();
        this.itemType = sm.getItemType() != null ? sm.getItemType().name() : null;
        this.ingredientId = sm.getIngredientId();
        this.extraId = sm.getExtraId();
        this.sizeId = sm.getSizeId();
        this.movementType = sm.getMovementType() != null ? sm.getMovementType().name() : null;
        this.quantity = sm.getQuantity();
        this.previousStock = sm.getPreviousStock();
        this.newStock = sm.getNewStock();
        this.unitCost = sm.getUnitCost();
        this.totalCost = sm.getTotalCost();
        this.referenceType = sm.getReferenceType();
        this.referenceId = sm.getReferenceId();
        this.reason = sm.getReason();
        this.userId = sm.getUserId();
        this.createdAt = sm.getCreatedAt();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

    public Integer getExtraId() { return extraId; }
    public void setExtraId(Integer extraId) { this.extraId = extraId; }

    public Integer getSizeId() { return sizeId; }
    public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

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

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

