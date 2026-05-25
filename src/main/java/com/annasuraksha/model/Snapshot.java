package com.annasuraksha.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "snapshots")
public class Snapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String root;
    private LocalDateTime createdAt;

    public Snapshot() {}
    public Snapshot(String root) { this.root = root; this.createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getRoot() { return root; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setRoot(String root) { this.root = root; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
