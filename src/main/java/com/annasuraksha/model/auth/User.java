package com.annasuraksha.model.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles;

    private String  stateCode;
    private String  fullName;
    private String  employeeId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        active = true;
    }
    public User() {}
    public User(Long id, String email, String passwordHash, List<String> roles, String stateCode, String fullName, String employeeId, boolean active, LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.stateCode = stateCode;
        this.fullName = fullName;
        this.employeeId = employeeId;
        this.active = active;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }
    public Long getId() { return this.id; }
    public String getEmail() { return this.email; }
    public String getPasswordHash() { return this.passwordHash; }
    public List<String> getRoles() { return this.roles; }
    public String getStateCode() { return this.stateCode; }
    public String getFullName() { return this.fullName; }
    public String getEmployeeId() { return this.employeeId; }
    public boolean isActive() { return this.active; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public LocalDateTime getLastLoginAt() { return this.lastLoginAt; }
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public static UserBuilder builder() { return new UserBuilder(); }
    public static class UserBuilder {
        private Long id;
        private String email;
        private String passwordHash;
        private List<String> roles;
        private String stateCode;
        private String fullName;
        private String employeeId;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        public UserBuilder id(Long id) { this.id = id; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public UserBuilder roles(List<String> roles) { this.roles = roles; return this; }
        public UserBuilder stateCode(String stateCode) { this.stateCode = stateCode; return this; }
        public UserBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public UserBuilder employeeId(String employeeId) { this.employeeId = employeeId; return this; }
        public UserBuilder active(boolean active) { this.active = active; return this; }
        public UserBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserBuilder lastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }
        public User build() { return new User(this.id, this.email, this.passwordHash, this.roles, this.stateCode, this.fullName, this.employeeId, this.active, this.createdAt, this.lastLoginAt); }
    }
}
