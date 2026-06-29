package com.example.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Integer entityId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_url", length = 500)
    private String requestUrl;

    @Column(name = "old_values", columnDefinition = "JSON")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "JSON")
    private String newValues;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.SUCCESS;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ==== Enums ====
    public enum ActionType {
        LOGIN,
        LOGOUT,
        CREATE,
        UPDATE,
        DELETE,
        VIEW,
        EXPORT,
        FAILED_LOGIN
    }

    public enum Status {
        SUCCESS,
        FAILED,
        WARNING
    }

    // ==== Constructores ====
    public AuditLog() {}

    public AuditLog(Integer userId, ActionType actionType, String description) {
        this.userId = userId;
        this.actionType = actionType;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // ==== Builder est\u00e1tico para facilitar la creaci\u00f3n ====
    public static AuditLog.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AuditLog auditLog = new AuditLog();

        public Builder userId(Integer userId) {
            auditLog.userId = userId;
            return this;
        }

        public Builder actionType(ActionType actionType) {
            auditLog.actionType = actionType;
            return this;
        }

        public Builder entityType(String entityType) {
            auditLog.entityType = entityType;
            return this;
        }

        public Builder entityId(Integer entityId) {
            auditLog.entityId = entityId;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            auditLog.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            auditLog.userAgent = userAgent;
            return this;
        }

        public Builder requestMethod(String requestMethod) {
            auditLog.requestMethod = requestMethod;
            return this;
        }

        public Builder requestUrl(String requestUrl) {
            auditLog.requestUrl = requestUrl;
            return this;
        }

        public Builder oldValues(String oldValues) {
            auditLog.oldValues = oldValues;
            return this;
        }

        public Builder newValues(String newValues) {
            auditLog.newValues = newValues;
            return this;
        }

        public Builder description(String description) {
            auditLog.description = description;
            return this;
        }

        public Builder status(Status status) {
            auditLog.status = status;
            return this;
        }

        public AuditLog build() {
            auditLog.createdAt = LocalDateTime.now();
            return auditLog;
        }
    }

    // ==== Getters y Setters ====
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

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
