package com.example.domain.dto;

import com.example.domain.model.AuditLog;
import java.time.LocalDateTime;

public class AuditLogDTO {
    
    private Long id;
    private Integer userId;
    private String userName;  // Nombre del usuario (para mostrar)
    private String actionType;
    private String entityType;
    private Integer entityId;
    private String ipAddress;
    private String userAgent;
    private String requestMethod;
    private String requestUrl;
    private String oldValues;
    private String newValues;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    // Constructor vacío
    public AuditLogDTO() {}

    // Constructor desde entidad
    public AuditLogDTO(AuditLog auditLog) {
        this.id = auditLog.getId();
        this.userId = auditLog.getUserId();
        this.actionType = auditLog.getActionType() != null ? auditLog.getActionType().name() : null;
        this.entityType = auditLog.getEntityType();
        this.entityId = auditLog.getEntityId();
        this.ipAddress = auditLog.getIpAddress();
        this.userAgent = auditLog.getUserAgent();
        this.requestMethod = auditLog.getRequestMethod();
        this.requestUrl = auditLog.getRequestUrl();
        this.oldValues = auditLog.getOldValues();
        this.newValues = auditLog.getNewValues();
        this.description = auditLog.getDescription();
        this.status = auditLog.getStatus() != null ? auditLog.getStatus().name() : null;
        this.createdAt = auditLog.getCreatedAt();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getOldValues() {
        return oldValues;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
