package com.annasuraksha.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "distribution_entries")
public class DistributionEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long beneficiaryId;
    private String beneficiaryName;
    private String fpsShopId;
    
    private Integer riceKg;
    private Integer wheatKg;
    private Integer sugarKg;
    
    private Boolean ruleViolation;
    private String violationReason;

    // Blockchain
    private String entryHash;
    private String prevEntryHash;
    private Long   entryHeight;

    private LocalDateTime distributedAt;

    @PrePersist
    protected void onCreate() {
        if (distributedAt == null) distributedAt = LocalDateTime.now();
        if (ruleViolation == null) ruleViolation = false;
    }

    public DistributionEntry() {}
    public DistributionEntry(Long id, Long beneficiaryId, String beneficiaryName, String fpsShopId, Integer riceKg, Integer wheatKg, Integer sugarKg, Boolean ruleViolation, String violationReason, String entryHash, String prevEntryHash, Long entryHeight, LocalDateTime distributedAt) {
        this.id = id;
        this.beneficiaryId = beneficiaryId;
        this.beneficiaryName = beneficiaryName;
        this.fpsShopId = fpsShopId;
        this.riceKg = riceKg;
        this.wheatKg = wheatKg;
        this.sugarKg = sugarKg;
        this.ruleViolation = ruleViolation;
        this.violationReason = violationReason;
        this.entryHash = entryHash;
        this.prevEntryHash = prevEntryHash;
        this.entryHeight = entryHeight;
        this.distributedAt = distributedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getFpsShopId() { return fpsShopId; }
    public void setFpsShopId(String fpsShopId) { this.fpsShopId = fpsShopId; }
    public Integer getRiceKg() { return riceKg; }
    public void setRiceKg(Integer riceKg) { this.riceKg = riceKg; }
    public Integer getWheatKg() { return wheatKg; }
    public void setWheatKg(Integer wheatKg) { this.wheatKg = wheatKg; }
    public Integer getSugarKg() { return sugarKg; }
    public void setSugarKg(Integer sugarKg) { this.sugarKg = sugarKg; }
    public Boolean getRuleViolation() { return ruleViolation; }
    public void setRuleViolation(Boolean ruleViolation) { this.ruleViolation = ruleViolation; }
    public String getViolationReason() { return violationReason; }
    public void setViolationReason(String violationReason) { this.violationReason = violationReason; }
    public String getEntryHash() { return entryHash; }
    public void setEntryHash(String entryHash) { this.entryHash = entryHash; }
    public String getPrevEntryHash() { return prevEntryHash; }
    public void setPrevEntryHash(String prevEntryHash) { this.prevEntryHash = prevEntryHash; }
    public Long getEntryHeight() { return entryHeight; }
    public void setEntryHeight(Long entryHeight) { this.entryHeight = entryHeight; }
    public LocalDateTime getDistributedAt() { return distributedAt; }
    public void setDistributedAt(LocalDateTime distributedAt) { this.distributedAt = distributedAt; }

    public static DistributionEntryBuilder builder() { return new DistributionEntryBuilder(); }
    public static class DistributionEntryBuilder {
        private Long id;
        private Long beneficiaryId;
        private String beneficiaryName;
        private String fpsShopId;
        private Integer riceKg;
        private Integer wheatKg;
        private Integer sugarKg;
        private Boolean ruleViolation;
        private String violationReason;
        private String entryHash;
        private String prevEntryHash;
        private Long entryHeight;
        private LocalDateTime distributedAt;

        public DistributionEntryBuilder id(Long id) { this.id = id; return this; }
        public DistributionEntryBuilder beneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; return this; }
        public DistributionEntryBuilder beneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; return this; }
        public DistributionEntryBuilder fpsShopId(String fpsShopId) { this.fpsShopId = fpsShopId; return this; }
        public DistributionEntryBuilder riceKg(Integer riceKg) { this.riceKg = riceKg; return this; }
        public DistributionEntryBuilder wheatKg(Integer wheatKg) { this.wheatKg = wheatKg; return this; }
        public DistributionEntryBuilder sugarKg(Integer sugarKg) { this.sugarKg = sugarKg; return this; }
        public DistributionEntryBuilder ruleViolation(Boolean ruleViolation) { this.ruleViolation = ruleViolation; return this; }
        public DistributionEntryBuilder violationReason(String violationReason) { this.violationReason = violationReason; return this; }
        public DistributionEntryBuilder entryHash(String entryHash) { this.entryHash = entryHash; return this; }
        public DistributionEntryBuilder prevEntryHash(String prevEntryHash) { this.prevEntryHash = prevEntryHash; return this; }
        public DistributionEntryBuilder entryHeight(Long entryHeight) { this.entryHeight = entryHeight; return this; }
        public DistributionEntryBuilder distributedAt(LocalDateTime distributedAt) { this.distributedAt = distributedAt; return this; }

        public DistributionEntry build() {
            return new DistributionEntry(id, beneficiaryId, beneficiaryName, fpsShopId, riceKg, wheatKg, sugarKg, ruleViolation, violationReason, entryHash, prevEntryHash, entryHeight, distributedAt);
        }
    }
}
