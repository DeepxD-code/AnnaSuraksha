package com.annasuraksha.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aadhaarHash;
    private String voterIdHash;
    private String maskedAadhaar;
    private String biometricHash;

    private String fullName;
    private String stateCode;
    private String stateName;
    private String district;
    private String category;    // BPL / APL / AAY / PHH

    @Column(nullable = false)
    private Integer familySize;

    private String  phone;
    private Integer riceKg;
    private Integer wheatKg;
    private Integer sugarKg;
    private Integer keroseneL;
    private String  fpsShopId;

    private String  status;     // ACTIVE / GHOST / SUSPENDED
    private Boolean migrant;
    private String  claimState;
    private Integer claimCount;

    // Geo
    private Double latitude;
    private Double longitude;

    // Blockchain
    private String blockHash;
    private String prevBlockHash;
    private Long   blockHeight;

    // Ghost detection
    private String ghostReason;
    private String ghostLayer;

    // Risk score cache
    private Double        riskScore;
    private String        riskLevel;
    private LocalDateTime riskComputedAt;

    // Demo flag
    private Boolean simulationMode;

    // Timestamps
    private LocalDateTime registeredAt;
    private LocalDateTime lastClaimAt;
    private LocalDateTime flaggedAt;

    @PrePersist
    protected void onCreate() {
        if (registeredAt  == null) registeredAt  = LocalDateTime.now();
        if (claimCount    == null) claimCount    = 0;
        if (migrant       == null) migrant        = false;
        if (status        == null) status         = "ACTIVE";
        if (simulationMode== null) simulationMode = false;
    }
    public Beneficiary() {}
    public Beneficiary(Long id, String aadhaarHash, String voterIdHash, String maskedAadhaar, String biometricHash, String fullName, String stateCode, String stateName, String district, Integer familySize, String phone, Integer riceKg, Integer wheatKg, Integer sugarKg, Integer keroseneL, String fpsShopId, Boolean migrant, String claimState, Integer claimCount, Double latitude, Double longitude, String blockHash, String prevBlockHash, Long blockHeight, String ghostReason, String ghostLayer, Double riskScore, String riskLevel, LocalDateTime riskComputedAt, Boolean simulationMode, LocalDateTime registeredAt, LocalDateTime lastClaimAt, LocalDateTime flaggedAt) {
        this.id = id;
        this.aadhaarHash = aadhaarHash;
        this.voterIdHash = voterIdHash;
        this.maskedAadhaar = maskedAadhaar;
        this.biometricHash = biometricHash;
        this.fullName = fullName;
        this.stateCode = stateCode;
        this.stateName = stateName;
        this.district = district;
        this.familySize = familySize;
        this.phone = phone;
        this.riceKg = riceKg;
        this.wheatKg = wheatKg;
        this.sugarKg = sugarKg;
        this.keroseneL = keroseneL;
        this.fpsShopId = fpsShopId;
        this.migrant = migrant;
        this.claimState = claimState;
        this.claimCount = claimCount;
        this.latitude = latitude;
        this.longitude = longitude;
        this.blockHash = blockHash;
        this.prevBlockHash = prevBlockHash;
        this.blockHeight = blockHeight;
        this.ghostReason = ghostReason;
        this.ghostLayer = ghostLayer;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.riskComputedAt = riskComputedAt;
        this.simulationMode = simulationMode;
        this.registeredAt = registeredAt;
        this.lastClaimAt = lastClaimAt;
        this.flaggedAt = flaggedAt;
    }
    public Long getId() { return this.id; }
    public String getAadhaarHash() { return this.aadhaarHash; }
    public String getVoterIdHash() { return this.voterIdHash; }
    public String getMaskedAadhaar() { return this.maskedAadhaar; }
    public String getBiometricHash() { return this.biometricHash; }
    public String getFullName() { return this.fullName; }
    public String getStateCode() { return this.stateCode; }
    public String getStateName() { return this.stateName; }
    public String getDistrict() { return this.district; }
    public Integer getFamilySize() { return this.familySize; }
    public String getPhone() { return this.phone; }
    public Integer getRiceKg() { return this.riceKg; }
    public Integer getWheatKg() { return this.wheatKg; }
    public Integer getSugarKg() { return this.sugarKg; }
    public Integer getKeroseneL() { return this.keroseneL; }
    public String getFpsShopId() { return this.fpsShopId; }
    public Boolean isMigrant() { return this.migrant; }
    public String getClaimState() { return this.claimState; }
    public Integer getClaimCount() { return this.claimCount; }
    public Double getLatitude() { return this.latitude; }
    public Double getLongitude() { return this.longitude; }
    public String getBlockHash() { return this.blockHash; }
    public String getPrevBlockHash() { return this.prevBlockHash; }
    public Long getBlockHeight() { return this.blockHeight; }
    public String getGhostReason() { return this.ghostReason; }
    public String getGhostLayer() { return this.ghostLayer; }
    public Double getRiskScore() { return this.riskScore; }
    public String getRiskLevel() { return this.riskLevel; }
    public LocalDateTime getRiskComputedAt() { return this.riskComputedAt; }
    public Boolean isSimulationMode() { return this.simulationMode; }
    public LocalDateTime getRegisteredAt() { return this.registeredAt; }
    public LocalDateTime getLastClaimAt() { return this.lastClaimAt; }
    public LocalDateTime getFlaggedAt() { return this.flaggedAt; }
    public String getCategory() { return this.category; }
    public String getStatus() { return this.status; }
    public Boolean getMigrant() { return this.migrant; }
    public void setId(Long id) { this.id = id; }
    public void setAadhaarHash(String aadhaarHash) { this.aadhaarHash = aadhaarHash; }
    public void setVoterIdHash(String voterIdHash) { this.voterIdHash = voterIdHash; }
    public void setMaskedAadhaar(String maskedAadhaar) { this.maskedAadhaar = maskedAadhaar; }
    public void setBiometricHash(String biometricHash) { this.biometricHash = biometricHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public void setStateName(String stateName) { this.stateName = stateName; }
    public void setDistrict(String district) { this.district = district; }
    public void setFamilySize(Integer familySize) { this.familySize = familySize; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRiceKg(Integer riceKg) { this.riceKg = riceKg; }
    public void setWheatKg(Integer wheatKg) { this.wheatKg = wheatKg; }
    public void setSugarKg(Integer sugarKg) { this.sugarKg = sugarKg; }
    public void setKeroseneL(Integer keroseneL) { this.keroseneL = keroseneL; }
    public void setFpsShopId(String fpsShopId) { this.fpsShopId = fpsShopId; }
    public void setMigrant(Boolean migrant) { this.migrant = migrant; }
    public void setClaimState(String claimState) { this.claimState = claimState; }
    public void setClaimCount(Integer claimCount) { this.claimCount = claimCount; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setBlockHash(String blockHash) { this.blockHash = blockHash; }
    public void setPrevBlockHash(String prevBlockHash) { this.prevBlockHash = prevBlockHash; }
    public void setBlockHeight(Long blockHeight) { this.blockHeight = blockHeight; }
    public void setGhostReason(String ghostReason) { this.ghostReason = ghostReason; }
    public void setGhostLayer(String ghostLayer) { this.ghostLayer = ghostLayer; }
    public void setRiskScore(Double riskScore) { this.riskScore = riskScore; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setRiskComputedAt(LocalDateTime riskComputedAt) { this.riskComputedAt = riskComputedAt; }

    public void setSimulationMode(Boolean simulationMode) { this.simulationMode = simulationMode; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public void setLastClaimAt(LocalDateTime lastClaimAt) { this.lastClaimAt = lastClaimAt; }
    public void setFlaggedAt(LocalDateTime flaggedAt) { this.flaggedAt = flaggedAt; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(String status) { this.status = status; }
    public static BeneficiaryBuilder builder() { return new BeneficiaryBuilder(); }
    public static class BeneficiaryBuilder {
        private Long id;
        private String aadhaarHash;
        private String voterIdHash;
        private String maskedAadhaar;
        private String biometricHash;
        private String fullName;
        private String stateCode;
        private String stateName;
        private String district;
        private Integer familySize;
        private String phone;
        private Integer riceKg;
        private Integer wheatKg;
        private Integer sugarKg;
        private Integer keroseneL;
        private String fpsShopId;
        private Boolean migrant;
        private String claimState;
        private Integer claimCount;
        private Double latitude;
        private Double longitude;
        private String blockHash;
        private String prevBlockHash;
        private Long blockHeight;
        private String ghostReason;
        private String ghostLayer;
        private Double riskScore;
        private String riskLevel;
        private LocalDateTime riskComputedAt;
        private Boolean simulationMode;
        private LocalDateTime registeredAt;
        private LocalDateTime lastClaimAt;
        private LocalDateTime flaggedAt;
        private String category;
        private String status;

        public BeneficiaryBuilder id(Long id) { this.id = id; return this; }
        public BeneficiaryBuilder aadhaarHash(String aadhaarHash) { this.aadhaarHash = aadhaarHash; return this; }
        public BeneficiaryBuilder voterIdHash(String voterIdHash) { this.voterIdHash = voterIdHash; return this; }
        public BeneficiaryBuilder maskedAadhaar(String maskedAadhaar) { this.maskedAadhaar = maskedAadhaar; return this; }
        public BeneficiaryBuilder biometricHash(String biometricHash) { this.biometricHash = biometricHash; return this; }
        public BeneficiaryBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public BeneficiaryBuilder stateCode(String stateCode) { this.stateCode = stateCode; return this; }
        public BeneficiaryBuilder stateName(String stateName) { this.stateName = stateName; return this; }
        public BeneficiaryBuilder district(String district) { this.district = district; return this; }
        public BeneficiaryBuilder familySize(Integer familySize) { this.familySize = familySize; return this; }
        public BeneficiaryBuilder phone(String phone) { this.phone = phone; return this; }
        public BeneficiaryBuilder riceKg(Integer riceKg) { this.riceKg = riceKg; return this; }
        public BeneficiaryBuilder wheatKg(Integer wheatKg) { this.wheatKg = wheatKg; return this; }
        public BeneficiaryBuilder sugarKg(Integer sugarKg) { this.sugarKg = sugarKg; return this; }
        public BeneficiaryBuilder keroseneL(Integer keroseneL) { this.keroseneL = keroseneL; return this; }
        public BeneficiaryBuilder fpsShopId(String fpsShopId) { this.fpsShopId = fpsShopId; return this; }
        public BeneficiaryBuilder migrant(Boolean migrant) { this.migrant = migrant; return this; }
        public BeneficiaryBuilder claimState(String claimState) { this.claimState = claimState; return this; }
        public BeneficiaryBuilder claimCount(Integer claimCount) { this.claimCount = claimCount; return this; }
        public BeneficiaryBuilder latitude(Double latitude) { this.latitude = latitude; return this; }
        public BeneficiaryBuilder longitude(Double longitude) { this.longitude = longitude; return this; }
        public BeneficiaryBuilder blockHash(String blockHash) { this.blockHash = blockHash; return this; }
        public BeneficiaryBuilder prevBlockHash(String prevBlockHash) { this.prevBlockHash = prevBlockHash; return this; }
        public BeneficiaryBuilder blockHeight(Long blockHeight) { this.blockHeight = blockHeight; return this; }
        public BeneficiaryBuilder ghostReason(String ghostReason) { this.ghostReason = ghostReason; return this; }
        public BeneficiaryBuilder ghostLayer(String ghostLayer) { this.ghostLayer = ghostLayer; return this; }
        public BeneficiaryBuilder riskScore(Double riskScore) { this.riskScore = riskScore; return this; }
        public BeneficiaryBuilder riskLevel(String riskLevel) { this.riskLevel = riskLevel; return this; }
        public BeneficiaryBuilder riskComputedAt(LocalDateTime riskComputedAt) { this.riskComputedAt = riskComputedAt; return this; }
        public BeneficiaryBuilder simulationMode(Boolean simulationMode) { this.simulationMode = simulationMode; return this; }
        public BeneficiaryBuilder registeredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; return this; }
        public BeneficiaryBuilder lastClaimAt(LocalDateTime lastClaimAt) { this.lastClaimAt = lastClaimAt; return this; }
        public BeneficiaryBuilder flaggedAt(LocalDateTime flaggedAt) { this.flaggedAt = flaggedAt; return this; }
        public BeneficiaryBuilder category(String category) { this.category = category; return this; }
        public BeneficiaryBuilder status(String status) { this.status = status; return this; }

        public Beneficiary build() { 
            Beneficiary b = new Beneficiary(id, aadhaarHash, voterIdHash, maskedAadhaar, biometricHash, fullName, stateCode, stateName, district, familySize, phone, riceKg, wheatKg, sugarKg, keroseneL, fpsShopId, migrant, claimState, claimCount, latitude, longitude, blockHash, prevBlockHash, blockHeight, ghostReason, ghostLayer, riskScore, riskLevel, riskComputedAt, simulationMode, registeredAt, lastClaimAt, flaggedAt); 
            b.setCategory(category);
            b.setStatus(status);
            return b;
        }
    }
}
