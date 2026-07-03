package com.example.dtos;

import com.example.models.OrderItem;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class OrderItemDTO {
    private Integer id;
    
    @NotNull(message = "El ID del pedido es obligatorio")
    private Integer orderId;
    
    private Integer pizzaId;
    private Integer extraId;
    
    @NotBlank(message = "El tipo de ítem es obligatorio")
    private String itemType; // "PIZZA" o "EXTRA"
    
    private Integer sizeId;
    
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer quantity;
    
    private BigDecimal unitPrice;
    private BigDecimal sizeExtra;
    private BigDecimal lineTotal;

    // Constructor vacío
    public OrderItemDTO() {}

    // Constructor desde entidad
    public OrderItemDTO(OrderItem orderItem) {
        this.id = orderItem.getId();
        this.orderId = orderItem.getOrderId();
        this.pizzaId = orderItem.getPizzaId();
        this.extraId = orderItem.getExtraId();
        this.itemType = orderItem.getItemType();
        this.sizeId = orderItem.getSizeId();
        this.quantity = orderItem.getQuantity();
        this.unitPrice = orderItem.getUnitPrice();
        this.sizeExtra = orderItem.getSizeExtra();
        this.lineTotal = orderItem.getLineTotal();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

    public Integer getExtraId() { return extraId; }
    public void setExtraId(Integer extraId) { this.extraId = extraId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

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
}
