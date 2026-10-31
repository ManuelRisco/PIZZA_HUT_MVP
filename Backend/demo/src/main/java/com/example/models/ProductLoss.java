package com.example.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_losses")
public class ProductLoss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType itemType;

    private Integer ingredientId;
    private Integer extraId;
    private Integer pizzaId;
    private Integer sizeId;
    private Integer orderId;

    @Column(nullable = false, length = 150)
    private String itemName;

    @Column(nullable = false, length = 20)
    private String unit = "und";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalLossCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LossCategory category;

    @Column(length = 255)
    private String reason;

    private Integer reportedByUserId;

    @Column(nullable = false)
    private LocalDateTime lossDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum ItemType {
        INGREDIENT,
        EXTRA,
        PIZZA,
        SIZE
    }

    public enum LossCategory {
        EXPIRED,
        DAMAGED,
        KITCHEN_ERROR,
        CANCELLED_ORDER,
        OTHER
    }

    public ProductLoss() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lossDate == null) {
            lossDate = LocalDateTime.now();
        }
        if (unit == null || unit.isBlank()) {
            unit = "und";
        }
        if (unitCost == null) {
            unitCost = BigDecimal.ZERO;
        }
        if (totalLossCost == null) {
            totalLossCost = BigDecimal.ZERO;
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

    public LossCategory getCategory() { return category; }
    public void setCategory(LossCategory category) { this.category = category; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(Integer reportedByUserId) { this.reportedByUserId = reportedByUserId; }

    public LocalDateTime getLossDate() { return lossDate; }
    public void setLossDate(LocalDateTime lossDate) { this.lossDate = lossDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

