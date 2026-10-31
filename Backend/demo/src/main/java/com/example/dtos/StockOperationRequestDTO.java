package com.example.dtos;

import java.math.BigDecimal;

public class StockOperationRequestDTO {
    private String itemType; // "INGREDIENT" o "EXTRA"
    private Integer itemId;
    private BigDecimal quantity; // Cantidad a sumar en restock, o nuevo stock físico en adjust
    private BigDecimal unitCost; // Opcional: actualizar costo unitario al comprar
    private BigDecimal minStock; // Opcional: actualizar stock mínimo
    private String reason;

    public StockOperationRequestDTO() {}

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getMinStock() { return minStock; }
    public void setMinStock(BigDecimal minStock) { this.minStock = minStock; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

