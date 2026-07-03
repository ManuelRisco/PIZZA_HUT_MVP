package com.example.dtos;

import com.example.models.Order;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDTO {
    private Integer id;
    
    private Integer userId;
    
    @NotNull(message = "El ID de la dirección es obligatorio")
    private Integer addressId;
    
    private String status;
    private String deliveryType;  // PICKUP o DELIVERY
    
    @NotNull(message = "El ID del método de pago es obligatorio")
    private Integer paymentMethodId;
    
    private Integer promotionId;  // ID de la promoción aplicada
    
    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El subtotal no puede ser negativo")
    private BigDecimal subtotal;
    
    @NotNull(message = "La tarifa de entrega es obligatoria")
    @DecimalMin(value = "0.0", inclusive = true, message = "La tarifa de entrega no puede ser negativa")
    private BigDecimal deliveryFee;
    
    private BigDecimal discount;
    
    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El total no puede ser negativo")
    private BigDecimal total;
    
    private String notes;
    private String promoCode;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime createdAt;

    // Constructor vacío
    public OrderDTO() {}

    // Constructor desde entidad
    public OrderDTO(Order order) {
        this.id = order.getId();
        this.userId = order.getUserId();
        this.addressId = order.getAddressId();
        this.status = order.getStatus() != null ? order.getStatus().name() : null;
        this.deliveryType = order.getDeliveryType() != null ? order.getDeliveryType().name() : null;
        this.paymentMethodId = order.getPaymentMethodId();
        this.promotionId = order.getPromotionId();
        this.subtotal = order.getSubtotal();
        this.deliveryFee = order.getDeliveryFee();
        this.discount = order.getDiscount();
        this.total = order.getTotal();
        this.notes = order.getNotes();
        this.promoCode = order.getPromoCode();
        this.estimatedDelivery = order.getEstimatedDelivery();
        this.createdAt = order.getCreatedAt();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

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
}
