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
    private String anchorTxHash;
    private String chainName;
    private LocalDateTime anchoredAt;

    public Snapshot() {}
    public Snapshot(String root) { this.root = root; this.createdAt = LocalDateTime.now(); }

    // Anchor metadata setters/getters
    public String getAnchorTxHash() { return anchorTxHash; }
    public void setAnchorTxHash(String anchorTxHash) { this.anchorTxHash = anchorTxHash; }

    public String getChainName() { return chainName; }
    public void setChainName(String chainName) { this.chainName = chainName; }

    public LocalDateTime getAnchoredAt() { return anchoredAt; }
    public void setAnchoredAt(LocalDateTime anchoredAt) { this.anchoredAt = anchoredAt; }

    public Long getId() { return id; }
    public String getRoot() { return root; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setRoot(String root) { this.root = root; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
