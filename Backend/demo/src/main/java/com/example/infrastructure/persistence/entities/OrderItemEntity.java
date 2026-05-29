package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.domain.model.OrderItem;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    public enum ItemType {
        PIZZA, EXTRA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer orderId;

    private Integer pizzaId;

    private Integer extraId;

    @Column(name = "item_type", length = 10)
    @Enumerated(EnumType.STRING)
    private ItemType itemType = ItemType.PIZZA;

    private Integer sizeId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sizeExtra = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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
    public OrderItemEntity() {}

    // ==== Conversión entre dominio y entidad ====
    public static OrderItemEntity fromDomain(OrderItem orderItem) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.id = orderItem.getId();
        entity.orderId = orderItem.getOrderId();
        entity.pizzaId = orderItem.getPizzaId();
        entity.extraId = orderItem.getExtraId();
        entity.itemType = orderItem.getItemType() != null 
            ? ItemType.valueOf(orderItem.getItemType()) 
            : ItemType.PIZZA;
        entity.sizeId = orderItem.getSizeId();
        entity.quantity = orderItem.getQuantity();
        entity.unitPrice = orderItem.getUnitPrice();
        entity.sizeExtra = orderItem.getSizeExtra();
        entity.lineTotal = orderItem.getLineTotal();
        entity.createdAt = orderItem.getCreatedAt();
        entity.updatedAt = orderItem.getUpdatedAt();
        return entity;
    }

    public OrderItem toDomain() {
        return new OrderItem(
            this.id,
            this.orderId,
            this.pizzaId,
            this.extraId,
            this.itemType != null ? this.itemType.name() : "PIZZA",
            this.sizeId,
            this.quantity,
            this.unitPrice,
            this.sizeExtra,
            this.lineTotal,
            this.createdAt,
            this.updatedAt
        );
    }

    // ==== Getters y Setters ====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

    public Integer getExtraId() { return extraId; }
    public void setExtraId(Integer extraId) { this.extraId = extraId; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public Integer getSizeId() { return sizeId; }
    public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getSizeExtra() { return sizeExtra; }
    public void setSizeExtra(BigDecimal sizeExtra) { this.sizeExtra = sizeExtra; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
