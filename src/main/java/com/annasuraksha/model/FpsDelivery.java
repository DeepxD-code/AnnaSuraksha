package com.annasuraksha.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fps_deliveries")
public class FpsDelivery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long    beneficiaryId;
    private String  fpsShopId;
    private String  fpsOperatorName;
    private String  stateCode;
    private Integer dealerRiceKg;
    private Integer confirmedRiceKg;
    private Boolean flagged;
    private String  flagReason;
    private LocalDateTime deliveryDate;
    public FpsDelivery() {}
    public FpsDelivery(Long id, Long beneficiaryId, String fpsShopId, String fpsOperatorName, String stateCode, Integer dealerRiceKg, Integer confirmedRiceKg, Boolean flagged, String flagReason, LocalDateTime deliveryDate) {
        this.id = id;
        this.beneficiaryId = beneficiaryId;
        this.fpsShopId = fpsShopId;
        this.fpsOperatorName = fpsOperatorName;
        this.stateCode = stateCode;
        this.dealerRiceKg = dealerRiceKg;
        this.confirmedRiceKg = confirmedRiceKg;
        this.flagged = flagged;
        this.flagReason = flagReason;
        this.deliveryDate = deliveryDate;
    }
    public Long getId() { return this.id; }
    public Long getBeneficiaryId() { return this.beneficiaryId; }
    public String getFpsShopId() { return this.fpsShopId; }
    public String getFpsOperatorName() { return this.fpsOperatorName; }
    public String getStateCode() { return this.stateCode; }
    public Integer getDealerRiceKg() { return this.dealerRiceKg; }
    public Integer getConfirmedRiceKg() { return this.confirmedRiceKg; }
    public Boolean isFlagged() { return this.flagged; }
    public String getFlagReason() { return this.flagReason; }
    public Boolean getFlagged() { return this.flagged; }
    public LocalDateTime getDeliveryDate() { return this.deliveryDate; }
    public void setId(Long id) { this.id = id; }
    public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }
    public void setFpsShopId(String fpsShopId) { this.fpsShopId = fpsShopId; }
    public void setFpsOperatorName(String fpsOperatorName) { this.fpsOperatorName = fpsOperatorName; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public void setDealerRiceKg(Integer dealerRiceKg) { this.dealerRiceKg = dealerRiceKg; }
    public void setConfirmedRiceKg(Integer confirmedRiceKg) { this.confirmedRiceKg = confirmedRiceKg; }
    public void setFlagged(Boolean flagged) { this.flagged = flagged; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }
    public void setDeliveryDate(LocalDateTime deliveryDate) { this.deliveryDate = deliveryDate; }
    public static FpsDeliveryBuilder builder() { return new FpsDeliveryBuilder(); }
    public static class FpsDeliveryBuilder {
        private Long id;
        private Long beneficiaryId;
        private String fpsShopId;
        private String fpsOperatorName;
        private String stateCode;
        private Integer dealerRiceKg;
        private Integer confirmedRiceKg;
        private Boolean flagged;
        private String flagReason;
        private LocalDateTime deliveryDate;
        public FpsDeliveryBuilder id(Long id) { this.id = id; return this; }
        public FpsDeliveryBuilder beneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; return this; }
        public FpsDeliveryBuilder fpsShopId(String fpsShopId) { this.fpsShopId = fpsShopId; return this; }
        public FpsDeliveryBuilder fpsOperatorName(String fpsOperatorName) { this.fpsOperatorName = fpsOperatorName; return this; }
        public FpsDeliveryBuilder stateCode(String stateCode) { this.stateCode = stateCode; return this; }
        public FpsDeliveryBuilder dealerRiceKg(Integer dealerRiceKg) { this.dealerRiceKg = dealerRiceKg; return this; }
        public FpsDeliveryBuilder confirmedRiceKg(Integer confirmedRiceKg) { this.confirmedRiceKg = confirmedRiceKg; return this; }
        public FpsDeliveryBuilder flagged(Boolean flagged) { this.flagged = flagged; return this; }
        public FpsDeliveryBuilder flagReason(String flagReason) { this.flagReason = flagReason; return this; }
        public FpsDeliveryBuilder deliveryDate(LocalDateTime deliveryDate) { this.deliveryDate = deliveryDate; return this; }
        public FpsDelivery build() { return new FpsDelivery(this.id, this.beneficiaryId, this.fpsShopId, this.fpsOperatorName, this.stateCode, this.dealerRiceKg, this.confirmedRiceKg, this.flagged, this.flagReason, this.deliveryDate); }
    }
}
