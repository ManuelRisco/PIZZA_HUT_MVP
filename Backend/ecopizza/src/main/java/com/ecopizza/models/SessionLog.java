package com.ecopizza.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    public enum LogoutReason {
        MANUAL,
        TIMEOUT,
        TOKEN_EXPIRED,
        FORCED,
        SECURITY
    }

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

    public void endSession(LogoutReason reason) {
        this.isActive = false;
        this.logoutTime = LocalDateTime.now();
        this.logoutReason = reason;
    }

    public Long getSessionDuration() {
        if (logoutTime == null) {
            return Duration.between(loginTime, LocalDateTime.now()).toMinutes();
        }
        return Duration.between(loginTime, logoutTime).toMinutes();
    }

    public boolean isCurrentlyActive() {
        return isActive && logoutTime == null;
    }
}
