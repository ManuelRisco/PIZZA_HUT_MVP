package com.example.dtos;

import com.example.models.ProductLoss;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductLossDTO {
    private Integer id;
    private String itemType;
    private Integer ingredientId;
    private Integer extraId;
    private Integer pizzaId;
    private Integer sizeId;
    private Integer orderId;
    private String itemName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalLossCost;
    private String category;
    private String reason;
    private Integer reportedByUserId;
    private String reportedByName;
    private LocalDateTime lossDate;
    private LocalDateTime createdAt;

    public ProductLossDTO() {}

    public ProductLossDTO(ProductLoss loss) {
        this.id = loss.getId();
        this.itemType = loss.getItemType() != null ? loss.getItemType().name() : null;
        this.ingredientId = loss.getIngredientId();
        this.extraId = loss.getExtraId();
        this.pizzaId = loss.getPizzaId();
        this.sizeId = loss.getSizeId();
        this.orderId = loss.getOrderId();
        this.itemName = loss.getItemName();
        this.unit = loss.getUnit();
        this.quantity = loss.getQuantity();
        this.unitCost = loss.getUnitCost();
        this.totalLossCost = loss.getTotalLossCost();
        this.category = loss.getCategory() != null ? loss.getCategory().name() : null;
        this.reason = loss.getReason();
        this.reportedByUserId = loss.getReportedByUserId();
        this.lossDate = loss.getLossDate();
        this.createdAt = loss.getCreatedAt();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

    public Integer getExtraId() { return extraId; }
    public void setExtraId(Integer extraId) { this.extraId = extraId; }

    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

    public Integer getSizeId() { return sizeId; }
    public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getTotalLossCost() { return totalLossCost; }
    public void setTotalLossCost(BigDecimal totalLossCost) { this.totalLossCost = totalLossCost; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(Integer reportedByUserId) { this.reportedByUserId = reportedByUserId; }

    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }

    public LocalDateTime getLossDate() { return lossDate; }
    public void setLossDate(LocalDateTime lossDate) { this.lossDate = lossDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

