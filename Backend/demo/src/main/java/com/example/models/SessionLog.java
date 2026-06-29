package com.example.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_logs")
public class SessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "session_token", length = 512)
    private String sessionToken;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(length = 50)
    private String browser;

    @Column(length = 100)
    private String location;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "logout_reason", length = 20)
    private LogoutReason logoutReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ==== Enum ====
    public enum LogoutReason {
        MANUAL,         // Usuario cerr\u00f3 sesi\u00f3n manualmente
        TIMEOUT,        // Sesi\u00f3n expir\u00f3 por inactividad
        TOKEN_EXPIRED,  // Token JWT expir\u00f3
        FORCED,         // Admin forz\u00f3 el cierre
        SECURITY        // Cierre por razones de seguridad
    }

    // ==== Constructores ====
    public SessionLog() {}

    public SessionLog(Integer userId, String sessionToken, String ipAddress, String userAgent) {
        this.userId = userId;
        this.sessionToken = sessionToken;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginTime = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.loginTime == null) {
            this.loginTime = LocalDateTime.now();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // ==== M\u00e9todos de utilidad ====
    public void endSession(LogoutReason reason) {
        this.isActive = false;
        this.logoutTime = LocalDateTime.now();
        this.logoutReason = reason;
    }

    public Long getSessionDuration() {
        if (logoutTime == null) {
            return java.time.Duration.between(loginTime, LocalDateTime.now()).toMinutes();
        }
        return java.time.Duration.between(loginTime, logoutTime).toMinutes();
    }

    public boolean isCurrentlyActive() {
        return isActive && logoutTime == null;
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

    public LogoutReason getLogoutReason() {
        return logoutReason;
    }

    public void setLogoutReason(LogoutReason logoutReason) {
        this.logoutReason = logoutReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
