package com.example.domain.dto;

import com.example.domain.model.Size;
import java.math.BigDecimal;

public class SizeDTO {
    private Integer id;
    private String name;
    private BigDecimal extraCost;
    private String description;
    private Integer displayOrder;

    // Constructor vacío
    public SizeDTO() {}

    // Constructor desde entidad
    public SizeDTO(Size size) {
        this.id = size.getId();
        this.name = size.getName();
        this.extraCost = size.getExtraCost();
        this.description = size.getDescription();
        this.displayOrder = size.getDisplayOrder();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getExtraCost() { return extraCost; }
    public void setExtraCost(BigDecimal extraCost) { this.extraCost = extraCost; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
