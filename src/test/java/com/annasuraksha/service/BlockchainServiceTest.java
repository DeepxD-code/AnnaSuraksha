package com.annasuraksha.service;

import com.annasuraksha.model.Beneficiary;
import com.annasuraksha.model.BeneficiaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 * STEP 2 OF THE LEARNING LADDER — YOUR FIRST MOCK
 * ============================================================================
 *
 * WHAT PROBLEM DOES MOCKING SOLVE, IN PLAIN TERMS?
 *
 * BlockchainService.validateChain() and getLatestHash() both call
 * beneRepo.findAllByOrderByBlockHeightAsc(). In real life, that method talks
 * to an H2/Postgres database. You do NOT want your test to:
 *   (a) need a real running database,
 *   (b) be slow because it's doing real I/O,
 *   (c) accidentally test the database's behavior instead of YOUR logic.
 *
 * So instead, you make a FAKE BeneficiaryRepository — one you fully control
 * — and you tell it exactly what to hand back when a specific method is
 * called. That's what @Mock + when(...).thenReturn(...) does below.
 *
 * Read it as a sentence: "WHEN someone calls beneRepo.findAllByOrderBy...(),
 * THEN RETURN this exact list I prepared." The real validateChain() method
 * has no idea the repository is fake — it just calls the method and gets
 * back whatever you scripted. That lets you test ONLY your logic
 * (the hash-chain comparison), with zero database involved.
 *
 * @ExtendWith(MockitoExtension.class) is the annotation that turns on
 * Mockito's @Mock processing for this test class — without it, @Mock fields
 * would just be null.
 */
@ExtendWith(MockitoExtension.class)
class BlockchainServiceTest {

    @Mock
    private BeneficiaryRepository beneRepo;

    private BlockchainService blockchainService;

    @BeforeEach
    void setUp() {
        // Construct the REAL service, but hand it the FAKE repository.
        // This is manual dependency injection — the same pattern Spring
        // does automatically at runtime, but here we do it by hand so we
        // control exactly what "the database" contains during the test.
        blockchainService = new BlockchainService(beneRepo);
    }

    // ------------------------------------------------------------------
    // Question 1: typical/normal case — an intact, unbroken chain.
    // ------------------------------------------------------------------
    @Test
    void validChainReturnsTrue() {
        Beneficiary first  = Beneficiary.builder().id(1L).blockHeight(1L)
            .prevBlockHash("0".repeat(64)).blockHash("HASH_A").build();
        Beneficiary second = Beneficiary.builder().id(2L).blockHeight(2L)
            .prevBlockHash("HASH_A").blockHash("HASH_B").build();

        // The "when...thenReturn" line: whenever validateChain() internally
        // calls this exact repository method, hand back this exact list.
        when(beneRepo.findAllByOrderByBlockHeightAsc()).thenReturn(List.of(first, second));

        assertTrue(blockchainService.validateChain(),
            "Each block's prevBlockHash correctly matches the prior block's blockHash — chain should be valid.");
    }

    // ------------------------------------------------------------------
    // Question 2: the edge case that actually matters for a FRAUD system.
    // If someone tampers with a stored blockHash after the fact, the next
    // block's prevBlockHash will no longer match. THIS is the property
    // the whole Merkle/blockchain design exists to detect. If this test
    // fails, the entire "tamper-evidence" claim in your README is false.
    // ------------------------------------------------------------------
    @Test
    void tamperedChainReturnsFalse() {
        Beneficiary first  = Beneficiary.builder().id(1L).blockHeight(1L)
            .prevBlockHash("0".repeat(64)).blockHash("HASH_A").build();
        Beneficiary second = Beneficiary.builder().id(2L).blockHeight(2L)
            .prevBlockHash("WRONG_HASH_SOMEONE_TAMPERED") // deliberately broken link
            .blockHash("HASH_B").build();

        when(beneRepo.findAllByOrderByBlockHeightAsc()).thenReturn(List.of(first, second));

        assertFalse(blockchainService.validateChain(),
            "A mismatched prevBlockHash must be detected as tampering — this is the entire point of the hash chain.");
    }

    // ------------------------------------------------------------------
    // Question 3: the boundary case — an empty chain (no beneficiaries yet).
    // This matters because it's exactly the state of a brand-new deployment,
    // and getLatestHash() has an explicit fallback for it in the source
    // ("0".repeat(64)) — that fallback deserves its own test, not an assumption.
    // ------------------------------------------------------------------
    @Test
    void emptyChainReturnsGenesisHash() {
        when(beneRepo.findAllByOrderByBlockHeightAsc()).thenReturn(List.of());

        assertEquals("0".repeat(64), blockchainService.getLatestHash(),
            "With no beneficiaries registered yet, getLatestHash() must return the documented genesis sentinel, not null or an exception.");
    }

    // ------------------------------------------------------------------
    // Bonus: a single-block chain is trivially valid — there's no second
    // block to have a mismatched prevBlockHash, so the loop in
    // validateChain() (which starts at i=1) never even runs.
    // ------------------------------------------------------------------
    @Test
    void singleBlockChainIsTriviallyValid() {
        Beneficiary only = Beneficiary.builder().id(1L).blockHeight(1L)
            .prevBlockHash("0".repeat(64)).blockHash("HASH_A").build();

        when(beneRepo.findAllByOrderByBlockHeightAsc()).thenReturn(List.of(only));

        assertTrue(blockchainService.validateChain(),
            "A chain of exactly one block has nothing to compare against and must be valid by definition.");
    }
}
