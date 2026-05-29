package com.example.infrastructure.persistence.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.example.domain.model.Address;

@Entity
@Table(name = "addresses")
public class AddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false, length = 255)
    private String line1;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 255)
    private String reference;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

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
    public AddressEntity() {}

    // ==== Conversión entre dominio y entidad ====
    public static AddressEntity fromDomain(Address address) {
        AddressEntity entity = new AddressEntity();
        entity.id = address.getId();
        entity.userId = address.getUserId();
        entity.line1 = address.getLine1();
        entity.city = address.getCity();
        entity.district = address.getDistrict();
        entity.reference = address.getReference();
        entity.isDefault = address.getIsDefault();
        entity.createdAt = address.getCreatedAt();
        entity.updatedAt = address.getUpdatedAt();
        entity.deletedAt = address.getDeletedAt();
        return entity;
    }

    public Address toDomain() {
        return new Address(
            this.id,
            this.userId,
            this.line1,
            this.city,
            this.district,
            this.reference,
            this.isDefault,
            this.createdAt,
            this.updatedAt,
            this.deletedAt
        );
    }

    // ==== Getters y Setters ====
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
