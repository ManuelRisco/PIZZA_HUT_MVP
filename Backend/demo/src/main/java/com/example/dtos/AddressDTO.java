package com.example.dtos;

import com.example.models.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AddressDTO {
    private Integer id;
    
    @NotNull(message = "El ID del usuario es obligatorio")
    private Integer userId;
    
    @NotBlank(message = "La línea 1 de la dirección es obligatoria")
    private String line1;
    
    @NotBlank(message = "La ciudad es obligatoria")
    private String city;
    
    @NotBlank(message = "El distrito es obligatorio")
    private String district;
    
    private String reference;
    
    @JsonProperty("isDefault")
    private Boolean isDefault;

    // Constructor vacío
    public AddressDTO() {}

    // Constructor desde entidad
    public AddressDTO(Address address) {
        this.id = address.getId();
        this.userId = address.getUserId();
        this.line1 = address.getLine1();
        this.city = address.getCity();
        this.district = address.getDistrict();
        this.reference = address.getReference();
        this.isDefault = address.getIsDefault();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
