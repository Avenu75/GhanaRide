package com.ghanaride.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * In-app notification entity.
 */
@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notif_user", columnList = "user_id"),
        @Index(name = "idx_notif_type", columnList = "type"),
        @Index(name = "idx_notif_read", columnList = "read"),
        @Index(name = "idx_notif_created", columnList = "created_at")
    }
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private NotificationType type;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "message", length = 1000, nullable = false)
    private String message;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "read")
    private Boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (read == null) read = false;
        if (priority == null) priority = "NORMAL";
    }

    public enum NotificationType {
        BOOKING_CONFIRMED,
        BOOKING_CANCELLED,
        TRIP_CANCELLED,
        TRIP_REMINDER,
        PAYMENT_RECEIVED,
        REFUND_PROCESSED,
        NEW_BOOKING,
        TRIP_APPROVED,
        TRIP_REJECTED,
        WALLET_TOPUP,
        LOYALTY_EARNED,
        REVIEW_RECEIVED,
        SYSTEM_MAINTENANCE,
        PROMOTION,
        PRICE_DROP,
        DRIVER_VERIFICATION,
        COMPANY_VERIFICATION,
        PASSWORD_CHANGED,
        LOGIN_ALERT,
        TWO_FACTOR_ENABLED
    }

    public Notification() {
    }

    public Notification(Long id, User user, NotificationType type, String title, String message, String actionUrl,
                       Boolean read, LocalDateTime readAt, String priority, String metadata, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.actionUrl = actionUrl;
        this.read = read;
        this.readAt = readAt;
        this.priority = priority;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Notification notification = new Notification();

        public Builder id(Long id) { notification.setId(id); return this; }
        public Builder user(User user) { notification.setUser(user); return this; }
        public Builder type(NotificationType type) { notification.setType(type); return this; }
        public Builder title(String title) { notification.setTitle(title); return this; }
        public Builder message(String message) { notification.setMessage(message); return this; }
        public Builder actionUrl(String actionUrl) { notification.setActionUrl(actionUrl); return this; }
        public Builder read(Boolean read) { notification.setRead(read); return this; }
        public Builder readAt(LocalDateTime readAt) { notification.setReadAt(readAt); return this; }
        public Builder priority(String priority) { notification.setPriority(priority); return this; }
        public Builder metadata(String metadata) { notification.setMetadata(metadata); return this; }
        public Builder createdAt(LocalDateTime createdAt) { notification.setCreatedAt(createdAt); return this; }
        public Notification build() { return notification; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}