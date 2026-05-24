package com.annasuraksha.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supply_chain_entries")
public class SupplyChainEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum Stage {
        WAREHOUSE_LOADED, DISPATCHED, IN_TRANSIT, FPS_RECEIVED, DISTRIBUTED, CONFIRMED
    }

    @Enumerated(EnumType.STRING)
    private Stage  stage;
    private String shipmentId;
    private String warehouseId;
    private String fpsShopId;
    private String stateCode;
    private String district;
    private String warehouseOfficerId;
    private String transporterId;
    private String fpsOperatorId;

    private Integer dispatchedRiceKg;
    private Integer dispatchedWheatKg;
    private Integer dispatchedSugarKg;
    private Integer receivedRiceKg;
    private Integer receivedWheatKg;
    private Integer receivedSugarKg;
    private Integer riceDiscrepancyKg;
    private Integer wheatDiscrepancyKg;
    private Integer sugarDiscrepancyKg;

    private Boolean discrepancyFlagged;
    private String  discrepancyReason;

    // GPS
    private Double  vehicleLat;
    private Double  vehicleLon;

    // Blockchain
    private String  entryHash;
    private String  prevEntryHash;
    private Long    entryHeight;

    private LocalDateTime dispatchedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt         == null) createdAt         = LocalDateTime.now();
        if (discrepancyFlagged== null) discrepancyFlagged = false;
    }
    public SupplyChainEntry() {}
    public SupplyChainEntry(Long id, Stage stage, String shipmentId, String warehouseId, String fpsShopId, String stateCode, String district, String warehouseOfficerId, String transporterId, String fpsOperatorId, Integer dispatchedRiceKg, Integer dispatchedWheatKg, Integer dispatchedSugarKg, Integer receivedRiceKg, Integer receivedWheatKg, Integer receivedSugarKg, Integer riceDiscrepancyKg, Integer wheatDiscrepancyKg, Integer sugarDiscrepancyKg, Boolean discrepancyFlagged, String discrepancyReason, Double vehicleLat, Double vehicleLon, String entryHash, String prevEntryHash, Long entryHeight, LocalDateTime dispatchedAt, LocalDateTime receivedAt, LocalDateTime createdAt) {
        this.id = id;
        this.stage = stage;
        this.shipmentId = shipmentId;
        this.warehouseId = warehouseId;
        this.fpsShopId = fpsShopId;
        this.stateCode = stateCode;
        this.district = district;
        this.warehouseOfficerId = warehouseOfficerId;
        this.transporterId = transporterId;
        this.fpsOperatorId = fpsOperatorId;
        this.dispatchedRiceKg = dispatchedRiceKg;
        this.dispatchedWheatKg = dispatchedWheatKg;
        this.dispatchedSugarKg = dispatchedSugarKg;
        this.receivedRiceKg = receivedRiceKg;
        this.receivedWheatKg = receivedWheatKg;
        this.receivedSugarKg = receivedSugarKg;
        this.riceDiscrepancyKg = riceDiscrepancyKg;
        this.wheatDiscrepancyKg = wheatDiscrepancyKg;
        this.sugarDiscrepancyKg = sugarDiscrepancyKg;
        this.discrepancyFlagged = discrepancyFlagged;
        this.discrepancyReason = discrepancyReason;
        this.vehicleLat = vehicleLat;
        this.vehicleLon = vehicleLon;
        this.entryHash = entryHash;
        this.prevEntryHash = prevEntryHash;
        this.entryHeight = entryHeight;
        this.dispatchedAt = dispatchedAt;
        this.receivedAt = receivedAt;
        this.createdAt = createdAt;
    }
    public Long getId() { return this.id; }
    public Stage getStage() { return this.stage; }
    public String getShipmentId() { return this.shipmentId; }
    public String getWarehouseId() { return this.warehouseId; }
    public String getFpsShopId() { return this.fpsShopId; }
    public String getStateCode() { return this.stateCode; }
    public String getDistrict() { return this.district; }
    public String getWarehouseOfficerId() { return this.warehouseOfficerId; }
    public String getTransporterId() { return this.transporterId; }
    public String getFpsOperatorId() { return this.fpsOperatorId; }
    public Integer getDispatchedRiceKg() { return this.dispatchedRiceKg; }
    public Integer getDispatchedWheatKg() { return this.dispatchedWheatKg; }
    public Integer getDispatchedSugarKg() { return this.dispatchedSugarKg; }
    public Integer getReceivedRiceKg() { return this.receivedRiceKg; }
    public Integer getReceivedWheatKg() { return this.receivedWheatKg; }
    public Integer getReceivedSugarKg() { return this.receivedSugarKg; }
    public Integer getRiceDiscrepancyKg() { return this.riceDiscrepancyKg; }
    public Integer getWheatDiscrepancyKg() { return this.wheatDiscrepancyKg; }
    public Integer getSugarDiscrepancyKg() { return this.sugarDiscrepancyKg; }
    public Boolean isDiscrepancyFlagged() { return this.discrepancyFlagged; }
    public String getDiscrepancyReason() { return this.discrepancyReason; }
    public Boolean getDiscrepancyFlagged() { return this.discrepancyFlagged; }
    public Double getVehicleLat() { return this.vehicleLat; }
    public Double getVehicleLon() { return this.vehicleLon; }
    public String getEntryHash() { return this.entryHash; }
    public String getPrevEntryHash() { return this.prevEntryHash; }
    public Long getEntryHeight() { return this.entryHeight; }
    public LocalDateTime getDispatchedAt() { return this.dispatchedAt; }
    public LocalDateTime getReceivedAt() { return this.receivedAt; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setId(Long id) { this.id = id; }
    public void setStage(Stage stage) { this.stage = stage; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
    public void setWarehouseId(String warehouseId) { this.warehouseId = warehouseId; }
    public void setFpsShopId(String fpsShopId) { this.fpsShopId = fpsShopId; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public void setDistrict(String district) { this.district = district; }
    public void setWarehouseOfficerId(String warehouseOfficerId) { this.warehouseOfficerId = warehouseOfficerId; }
    public void setTransporterId(String transporterId) { this.transporterId = transporterId; }
    public void setFpsOperatorId(String fpsOperatorId) { this.fpsOperatorId = fpsOperatorId; }
    public void setDispatchedRiceKg(Integer dispatchedRiceKg) { this.dispatchedRiceKg = dispatchedRiceKg; }
    public void setDispatchedWheatKg(Integer dispatchedWheatKg) { this.dispatchedWheatKg = dispatchedWheatKg; }
    public void setDispatchedSugarKg(Integer dispatchedSugarKg) { this.dispatchedSugarKg = dispatchedSugarKg; }
    public void setReceivedRiceKg(Integer receivedRiceKg) { this.receivedRiceKg = receivedRiceKg; }
    public void setReceivedWheatKg(Integer receivedWheatKg) { this.receivedWheatKg = receivedWheatKg; }
    public void setReceivedSugarKg(Integer receivedSugarKg) { this.receivedSugarKg = receivedSugarKg; }
    public void setRiceDiscrepancyKg(Integer riceDiscrepancyKg) { this.riceDiscrepancyKg = riceDiscrepancyKg; }
    public void setWheatDiscrepancyKg(Integer wheatDiscrepancyKg) { this.wheatDiscrepancyKg = wheatDiscrepancyKg; }
    public void setSugarDiscrepancyKg(Integer sugarDiscrepancyKg) { this.sugarDiscrepancyKg = sugarDiscrepancyKg; }
    public void setDiscrepancyFlagged(Boolean discrepancyFlagged) { this.discrepancyFlagged = discrepancyFlagged; }
    public void setDiscrepancyReason(String discrepancyReason) { this.discrepancyReason = discrepancyReason; }
    public void setVehicleLat(Double vehicleLat) { this.vehicleLat = vehicleLat; }
    public void setVehicleLon(Double vehicleLon) { this.vehicleLon = vehicleLon; }
    public void setEntryHash(String entryHash) { this.entryHash = entryHash; }
    public void setPrevEntryHash(String prevEntryHash) { this.prevEntryHash = prevEntryHash; }
    public void setEntryHeight(Long entryHeight) { this.entryHeight = entryHeight; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public static SupplyChainEntryBuilder builder() { return new SupplyChainEntryBuilder(); }
    public static class SupplyChainEntryBuilder {
        private Long id;
        private Stage stage;
        private String shipmentId;
        private String warehouseId;
        private String fpsShopId;
        private String stateCode;
        private String district;
        private String warehouseOfficerId;
        private String transporterId;
        private String fpsOperatorId;
        private Integer dispatchedRiceKg;
        private Integer dispatchedWheatKg;
        private Integer dispatchedSugarKg;
        private Integer receivedRiceKg;
        private Integer receivedWheatKg;
        private Integer receivedSugarKg;
        private Integer riceDiscrepancyKg;
        private Integer wheatDiscrepancyKg;
        private Integer sugarDiscrepancyKg;
        private Boolean discrepancyFlagged;
        private String discrepancyReason;
        private Double vehicleLat;
        private Double vehicleLon;
        private String entryHash;
        private String prevEntryHash;
        private Long entryHeight;
        private LocalDateTime dispatchedAt;
        private LocalDateTime receivedAt;
        private LocalDateTime createdAt;
        public SupplyChainEntryBuilder id(Long id) { this.id = id; return this; }
        public SupplyChainEntryBuilder stage(Stage stage) { this.stage = stage; return this; }
        public SupplyChainEntryBuilder shipmentId(String shipmentId) { this.shipmentId = shipmentId; return this; }
        public SupplyChainEntryBuilder warehouseId(String warehouseId) { this.warehouseId = warehouseId; return this; }
        public SupplyChainEntryBuilder fpsShopId(String fpsShopId) { this.fpsShopId = fpsShopId; return this; }
        public SupplyChainEntryBuilder stateCode(String stateCode) { this.stateCode = stateCode; return this; }
        public SupplyChainEntryBuilder district(String district) { this.district = district; return this; }
        public SupplyChainEntryBuilder warehouseOfficerId(String warehouseOfficerId) { this.warehouseOfficerId = warehouseOfficerId; return this; }
        public SupplyChainEntryBuilder transporterId(String transporterId) { this.transporterId = transporterId; return this; }
        public SupplyChainEntryBuilder fpsOperatorId(String fpsOperatorId) { this.fpsOperatorId = fpsOperatorId; return this; }
        public SupplyChainEntryBuilder dispatchedRiceKg(Integer dispatchedRiceKg) { this.dispatchedRiceKg = dispatchedRiceKg; return this; }
        public SupplyChainEntryBuilder dispatchedWheatKg(Integer dispatchedWheatKg) { this.dispatchedWheatKg = dispatchedWheatKg; return this; }
        public SupplyChainEntryBuilder dispatchedSugarKg(Integer dispatchedSugarKg) { this.dispatchedSugarKg = dispatchedSugarKg; return this; }
        public SupplyChainEntryBuilder receivedRiceKg(Integer receivedRiceKg) { this.receivedRiceKg = receivedRiceKg; return this; }
        public SupplyChainEntryBuilder receivedWheatKg(Integer receivedWheatKg) { this.receivedWheatKg = receivedWheatKg; return this; }
        public SupplyChainEntryBuilder receivedSugarKg(Integer receivedSugarKg) { this.receivedSugarKg = receivedSugarKg; return this; }
        public SupplyChainEntryBuilder riceDiscrepancyKg(Integer riceDiscrepancyKg) { this.riceDiscrepancyKg = riceDiscrepancyKg; return this; }
        public SupplyChainEntryBuilder wheatDiscrepancyKg(Integer wheatDiscrepancyKg) { this.wheatDiscrepancyKg = wheatDiscrepancyKg; return this; }
        public SupplyChainEntryBuilder sugarDiscrepancyKg(Integer sugarDiscrepancyKg) { this.sugarDiscrepancyKg = sugarDiscrepancyKg; return this; }
        public SupplyChainEntryBuilder discrepancyFlagged(Boolean discrepancyFlagged) { this.discrepancyFlagged = discrepancyFlagged; return this; }
        public SupplyChainEntryBuilder discrepancyReason(String discrepancyReason) { this.discrepancyReason = discrepancyReason; return this; }
        public SupplyChainEntryBuilder vehicleLat(Double vehicleLat) { this.vehicleLat = vehicleLat; return this; }
        public SupplyChainEntryBuilder vehicleLon(Double vehicleLon) { this.vehicleLon = vehicleLon; return this; }
        public SupplyChainEntryBuilder entryHash(String entryHash) { this.entryHash = entryHash; return this; }
        public SupplyChainEntryBuilder prevEntryHash(String prevEntryHash) { this.prevEntryHash = prevEntryHash; return this; }
        public SupplyChainEntryBuilder entryHeight(Long entryHeight) { this.entryHeight = entryHeight; return this; }
        public SupplyChainEntryBuilder dispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; return this; }
        public SupplyChainEntryBuilder receivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; return this; }
        public SupplyChainEntryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public SupplyChainEntry build() { return new SupplyChainEntry(this.id, this.stage, this.shipmentId, this.warehouseId, this.fpsShopId, this.stateCode, this.district, this.warehouseOfficerId, this.transporterId, this.fpsOperatorId, this.dispatchedRiceKg, this.dispatchedWheatKg, this.dispatchedSugarKg, this.receivedRiceKg, this.receivedWheatKg, this.receivedSugarKg, this.riceDiscrepancyKg, this.wheatDiscrepancyKg, this.sugarDiscrepancyKg, this.discrepancyFlagged, this.discrepancyReason, this.vehicleLat, this.vehicleLon, this.entryHash, this.prevEntryHash, this.entryHeight, this.dispatchedAt, this.receivedAt, this.createdAt); }
    }
}
