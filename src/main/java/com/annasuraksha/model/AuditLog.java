package com.annasuraksha.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    private String entityId;
    private String entityType;
    private String performedBy;
    private String clientIp;
    private String httpMethod;
    private String path;
    private Integer httpStatus;
    private Long    durationMs;
    private String  details;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public AuditLog() {}
    public AuditLog(Long id, String eventType, String entityId, String entityType, String performedBy, String clientIp, String httpMethod, String path, Integer httpStatus, Long durationMs, String details, LocalDateTime createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.entityId = entityId;
        this.entityType = entityType;
        this.performedBy = performedBy;
        this.clientIp = clientIp;
        this.httpMethod = httpMethod;
        this.path = path;
        this.httpStatus = httpStatus;
        this.durationMs = durationMs;
        this.details = details;
        this.createdAt = createdAt;
    }
    public Long getId() { return this.id; }
    public String getEventType() { return this.eventType; }
    public String getEntityId() { return this.entityId; }
    public String getEntityType() { return this.entityType; }
    public String getPerformedBy() { return this.performedBy; }
    public String getClientIp() { return this.clientIp; }
    public String getHttpMethod() { return this.httpMethod; }
    public String getPath() { return this.path; }
    public Integer getHttpStatus() { return this.httpStatus; }
    public Long getDurationMs() { return this.durationMs; }
    public String getDetails() { return this.details; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public void setPath(String path) { this.path = path; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setDetails(String details) { this.details = details; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public static AuditLogBuilder builder() { return new AuditLogBuilder(); }
    public static class AuditLogBuilder {
        private Long id;
        private String eventType;
        private String entityId;
        private String entityType;
        private String performedBy;
        private String clientIp;
        private String httpMethod;
        private String path;
        private Integer httpStatus;
        private Long durationMs;
        private String details;
        private LocalDateTime createdAt;
        public AuditLogBuilder id(Long id) { this.id = id; return this; }
        public AuditLogBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public AuditLogBuilder entityId(String entityId) { this.entityId = entityId; return this; }
        public AuditLogBuilder entityType(String entityType) { this.entityType = entityType; return this; }
        public AuditLogBuilder performedBy(String performedBy) { this.performedBy = performedBy; return this; }
        public AuditLogBuilder clientIp(String clientIp) { this.clientIp = clientIp; return this; }
        public AuditLogBuilder httpMethod(String httpMethod) { this.httpMethod = httpMethod; return this; }
        public AuditLogBuilder path(String path) { this.path = path; return this; }
        public AuditLogBuilder httpStatus(Integer httpStatus) { this.httpStatus = httpStatus; return this; }
        public AuditLogBuilder durationMs(Long durationMs) { this.durationMs = durationMs; return this; }
        public AuditLogBuilder details(String details) { this.details = details; return this; }
        public AuditLogBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AuditLog build() { return new AuditLog(this.id, this.eventType, this.entityId, this.entityType, this.performedBy, this.clientIp, this.httpMethod, this.path, this.httpStatus, this.durationMs, this.details, this.createdAt); }
    }
}
