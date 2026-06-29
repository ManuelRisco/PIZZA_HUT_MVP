package com.example.dtos;

import com.example.models.SessionLog;
import java.time.LocalDateTime;

public class SessionLogDTO {
    
    private Long id;
    private Integer userId;
    private String userName;  // Nombre del usuario (para mostrar)
    private String sessionToken;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String userAgent;
    private String deviceType;
    private String browser;
    private String location;
    private Boolean isActive;
    private String logoutReason;
    private LocalDateTime createdAt;
    
    // Campos calculados
    private Long sessionDuration;  // En minutos

    // Constructor vac\u00edo
    public SessionLogDTO() {}

    // Constructor desde entidad
    public SessionLogDTO(SessionLog sessionLog) {
        this.id = sessionLog.getId();
        this.userId = sessionLog.getUserId();
        this.sessionToken = sessionLog.getSessionToken();
        this.loginTime = sessionLog.getLoginTime();
        this.lastActivityTime = sessionLog.getLoginTime(); // Fallback to login time
        this.logoutTime = sessionLog.getLogoutTime();
        this.ipAddress = sessionLog.getIpAddress();
        this.userAgent = sessionLog.getUserAgent();
        this.deviceType = sessionLog.getDeviceType();
        this.browser = sessionLog.getBrowser();
        this.location = sessionLog.getLocation();
        this.isActive = sessionLog.getIsActive();
        this.logoutReason = sessionLog.getLogoutReason() != null ? sessionLog.getLogoutReason().name() : null;
        this.createdAt = sessionLog.getCreatedAt();
        
        // Calcular duraci\u00f3n
        this.sessionDuration = sessionLog.getSessionDuration();
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

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(LocalDateTime lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
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

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getLogoutReason() {
        return logoutReason;
    }

    public void setLogoutReason(String logoutReason) {
        this.logoutReason = logoutReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSessionDuration() {
        return sessionDuration;
    }

    public void setSessionDuration(Long sessionDuration) {
        this.sessionDuration = sessionDuration;
    }
}
