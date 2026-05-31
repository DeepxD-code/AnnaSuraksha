package com.annasuraksha.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnapshotProofRepository extends JpaRepository<SnapshotProof, Long> {
    List<SnapshotProof> findBySnapshotId(Long snapshotId);
    SnapshotProof findBySnapshotIdAndBeneficiaryId(Long snapshotId, Long beneficiaryId);
}
