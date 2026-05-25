package com.annasuraksha;

import com.annasuraksha.model.Beneficiary;
import com.annasuraksha.model.BeneficiaryRepository;
import com.annasuraksha.model.Snapshot;
import com.annasuraksha.model.SnapshotProof;
import com.annasuraksha.model.SnapshotProofRepository;
import com.annasuraksha.model.SnapshotRepository;
import com.annasuraksha.service.MerkleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MerkleFullFlowTest {

    @Autowired MerkleService merkleService;
    @Autowired BeneficiaryRepository beneRepo;
    @Autowired SnapshotRepository snapshotRepo;
    @Autowired SnapshotProofRepository proofRepo;

    @BeforeEach
    void setup() {
        snapshotRepo.deleteAll();
        proofRepo.deleteAll();
        beneRepo.deleteAll();

        beneRepo.saveAll(List.of(
            makeBene("1111-1111-1111", "IN-UP-S001", "ACTIVE"),
            makeBene("2222-2222-2222", "IN-UP-S001", "ACTIVE"),
            makeBene("3333-3333-3333", "IN-MH-S001", "ACTIVE")
        ));
    }

    private Beneficiary makeBene(String aadhaar, String stateCode, String status) {
        Beneficiary b = new Beneficiary();
        b.setAadhaarHash("HASH_" + aadhaar);
        b.setStateCode(stateCode);
        b.setStatus(status);
        b.setFamilySize(4);
        return b;
    }

    @Test
    void snapshotProofAndVerifyRoundtrip() {
        Snapshot snap = merkleService.createSnapshot();
        assertThat(snap).isNotNull();
        assertThat(snap.getRoot()).isNotBlank();

        List<Beneficiary> all = beneRepo.findAll();
        for (Beneficiary b : all) {
            SnapshotProof proof = merkleService.getProof(snap.getId(), b.getId());
            assertThat(proof).as("Proof for beneficiary %s", b.getId()).isNotNull();
            assertThat(proof.getProofJson()).isNotBlank();

            boolean verified = merkleService.verifyProof(snap.getId(), b.getId());
            assertThat(verified).as("Proof should verify for beneficiary %s", b.getId()).isTrue();
        }
    }

    @Test
    void snapshotHasAnchorMetadataStored() {
        Snapshot snap = merkleService.createSnapshot();
        assertThat(snap.getAnchorTxHash()).isNotNull();
        assertThat(snap.getChainName()).isEqualTo("fabric-mock");
        assertThat(snap.getAnchoredAt()).isNotNull();
    }
}