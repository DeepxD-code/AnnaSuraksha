package com.annasuraksha.model.alert;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_events")
public class AlertEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String alertId;

    private String alertType;
    private String severity;
    private String title;

    @Column(length = 2000)
    private String description;

    private String entityId;
    private String entityType;
    private String stateCode;
    private String actionUrl;

    private Boolean       acknowledged;
    private String        acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt    == null) createdAt    = LocalDateTime.now();
        if (acknowledged == null) acknowledged = false;
    }
    public AlertEvent() {}
    public AlertEvent(Long id, String alertId, String alertType, String severity, String title, String description, String entityId, String entityType, String stateCode, String actionUrl, Boolean acknowledged, String acknowledgedBy, LocalDateTime acknowledgedAt, LocalDateTime createdAt) {
        this.id = id;
        this.alertId = alertId;
        this.alertType = alertType;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.entityId = entityId;
        this.entityType = entityType;
        this.stateCode = stateCode;
        this.actionUrl = actionUrl;
        this.acknowledged = acknowledged;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = acknowledgedAt;
        this.createdAt = createdAt;
    }
    public Long getId() { return this.id; }
    public String getAlertId() { return this.alertId; }
    public String getAlertType() { return this.alertType; }
    public String getSeverity() { return this.severity; }
    public String getTitle() { return this.title; }
    public String getDescription() { return this.description; }
    public String getEntityId() { return this.entityId; }
    public String getEntityType() { return this.entityType; }
    public String getStateCode() { return this.stateCode; }
    public String getActionUrl() { return this.actionUrl; }
    public Boolean isAcknowledged() { return this.acknowledged; }
    public String getAcknowledgedBy() { return this.acknowledgedBy; }
    public LocalDateTime getAcknowledgedAt() { return this.acknowledgedAt; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public static AlertEventBuilder builder() { return new AlertEventBuilder(); }
    public static class AlertEventBuilder {
        private Long id;
        private String alertId;
        private String alertType;
        private String severity;
        private String title;
        private String description;
        private String entityId;
        private String entityType;
        private String stateCode;
        private String actionUrl;
        private Boolean acknowledged;
        private String acknowledgedBy;
        private LocalDateTime acknowledgedAt;
        private LocalDateTime createdAt;
        public AlertEventBuilder id(Long id) { this.id = id; return this; }
        public AlertEventBuilder alertId(String alertId) { this.alertId = alertId; return this; }
        public AlertEventBuilder alertType(String alertType) { this.alertType = alertType; return this; }
        public AlertEventBuilder severity(String severity) { this.severity = severity; return this; }
        public AlertEventBuilder title(String title) { this.title = title; return this; }
        public AlertEventBuilder description(String description) { this.description = description; return this; }
        public AlertEventBuilder entityId(String entityId) { this.entityId = entityId; return this; }
        public AlertEventBuilder entityType(String entityType) { this.entityType = entityType; return this; }
        public AlertEventBuilder stateCode(String stateCode) { this.stateCode = stateCode; return this; }
        public AlertEventBuilder actionUrl(String actionUrl) { this.actionUrl = actionUrl; return this; }
        public AlertEventBuilder acknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; return this; }
        public AlertEventBuilder acknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; return this; }
        public AlertEventBuilder acknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; return this; }
        public AlertEventBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AlertEvent build() { return new AlertEvent(this.id, this.alertId, this.alertType, this.severity, this.title, this.description, this.entityId, this.entityType, this.stateCode, this.actionUrl, this.acknowledged, this.acknowledgedBy, this.acknowledgedAt, this.createdAt); }
    }
}
