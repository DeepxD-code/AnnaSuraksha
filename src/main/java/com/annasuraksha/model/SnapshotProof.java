package com.annasuraksha.model;

import jakarta.persistence.*;

@Entity
@Table(name = "snapshot_proofs")
public class SnapshotProof {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long snapshotId;
    private Long beneficiaryId;
    private Integer leafIndex;
    @Lob
    private String proofJson; // JSON array of sibling hashes

    public SnapshotProof() {}
    public SnapshotProof(Long snapshotId, Long beneficiaryId, Integer leafIndex, String proofJson) {
        this.snapshotId = snapshotId; this.beneficiaryId = beneficiaryId; this.leafIndex = leafIndex; this.proofJson = proofJson;
    }

    public Long getId() { return id; }
    public Long getSnapshotId() { return snapshotId; }
    public Long getBeneficiaryId() { return beneficiaryId; }
    public Integer getLeafIndex() { return leafIndex; }
    public String getProofJson() { return proofJson; }
    public void setId(Long id) { this.id = id; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }
    public void setLeafIndex(Integer leafIndex) { this.leafIndex = leafIndex; }
    public void setProofJson(String proofJson) { this.proofJson = proofJson; }
}
