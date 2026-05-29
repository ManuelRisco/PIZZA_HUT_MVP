package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.domain.model.Order;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userId;

    private Integer addressId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Order.OrderStatus status = Order.OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 20)
    private Order.DeliveryType deliveryType = Order.DeliveryType.DELIVERY;

    private Integer paymentMethodId;  // FK a payment_methods
    
    private Integer promotionId;  // FK a promotions

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "promo_code", length = 50)
    private String promoCode;

    private LocalDateTime estimatedDelivery;

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
    public OrderEntity() {}

    // ==== Conversión entre dominio y entidad ====
    public static OrderEntity fromDomain(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.id = order.getId();
        entity.userId = order.getUserId();
        entity.addressId = order.getAddressId();
        entity.status = order.getStatus();
        entity.deliveryType = order.getDeliveryType();
        entity.paymentMethodId = order.getPaymentMethodId();
        entity.promotionId = order.getPromotionId();
        entity.subtotal = order.getSubtotal();
        entity.deliveryFee = order.getDeliveryFee();
        entity.discount = order.getDiscount();
        entity.total = order.getTotal();
        entity.notes = order.getNotes();
        entity.promoCode = order.getPromoCode();
        entity.estimatedDelivery = order.getEstimatedDelivery();
        entity.createdAt = order.getCreatedAt();
        entity.updatedAt = order.getUpdatedAt();
        return entity;
    }

    public Order toDomain() {
        Order order = new Order();
        order.setId(this.id);
        order.setUserId(this.userId);
        order.setAddressId(this.addressId);
        order.setStatus(this.status);
        order.setDeliveryType(this.deliveryType);
        order.setPaymentMethodId(this.paymentMethodId);
        order.setPromotionId(this.promotionId);
        order.setSubtotal(this.subtotal);
        order.setDeliveryFee(this.deliveryFee);
        order.setDiscount(this.discount);
        order.setTotal(this.total);
        order.setNotes(this.notes);
        order.setPromoCode(this.promoCode);
        order.setEstimatedDelivery(this.estimatedDelivery);
        order.setCreatedAt(this.createdAt);
        order.setUpdatedAt(this.updatedAt);
        return order;
    }

    // ==== Getters y Setters ====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }

    public Order.OrderStatus getStatus() { return status; }
    public void setStatus(Order.OrderStatus status) { this.status = status; }

    public Integer getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public Integer getPromotionId() { return promotionId; }
    public void setPromotionId(Integer promotionId) { this.promotionId = promotionId; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
