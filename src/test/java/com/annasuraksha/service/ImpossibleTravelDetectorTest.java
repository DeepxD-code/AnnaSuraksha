package com.annasuraksha.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ============================================================================
 * WHY THIS FILE STARTS WHERE IT STARTS (read this before anything else)
 * ============================================================================
 *
 * ImpossibleTravelDetector is a PURE function class: given the same inputs,
 * analyse() always returns the same output. It doesn't touch a database,
 * doesn't need Spring to boot up, and doesn't need Mockito. That's exactly
 * why the council said to begin here — you can learn what a test even *is*
 * without also having to learn mocking, dependency injection, or Spring's
 * test annotations at the same time.
 *
 * The three questions to ask about ANY function, before you write a test:
 *   1. What does it return for a typical, "normal" input?
 *   2. What does it return at the EDGES (zero, negative, exactly-on-the-
 *      boundary values, unknown/null input)?
 *   3. What does it return for input specifically designed to break an
 *      assumption you're making about the logic?
 *
 * This file answers all three, one method at a time, in increasing order
 * of difficulty. Read the comments above each test — they explain not just
 * WHAT is being asserted, but WHY that assertion is the interesting one.
 */
class ImpossibleTravelDetectorTest {

    private final ImpossibleTravelDetector detector = new ImpossibleTravelDetector();

    // ------------------------------------------------------------------
    // TEST 1 — the exact test the council told you to write first.
    // Question being asked: "what's the typical, boring, nothing-is-wrong case?"
    // ------------------------------------------------------------------
    @Test
    void sameStateIsNeverImpossible() {
        var result = detector.analyse("UP", "UP", 5.0);

        assertFalse(result.isImpossible(), "Claiming in your home state can never be 'impossible travel' — there is no travel.");
        assertEquals("Same state.", result.explanation());
    }

    // ------------------------------------------------------------------
    // TEST 2 — same idea, but proving case-sensitivity doesn't break it.
    // This exists because the source code calls .equalsIgnoreCase(stateB)
    // — a test should confirm that guarantee, not just assume it.
    // ------------------------------------------------------------------
    @Test
    void sameStateIsCaseInsensitive() {
        var result = detector.analyse("up", "UP", 0.5);

        assertFalse(result.isImpossible(), "State codes are compared case-insensitively in the source — 'up' and 'UP' must be treated as identical.");
    }

    // ------------------------------------------------------------------
    // TEST 3 — the "unknown input" edge case (question #2 above: edges).
    // What happens when someone passes a state code that doesn't exist
    // in STATE_COORDS? This matters in a fraud system: a beneficiary
    // record with a typo'd or corrupted state code should NOT crash the
    // detector or silently report "impossible" — it should say "I don't know."
    // ------------------------------------------------------------------
    @Test
    void unknownStateCodeIsHandledGracefully() {
        var result = detector.analyse("XX", "UP", 2.0);

        assertFalse(result.isImpossible(), "An unrecognized state code must never be silently flagged as fraud.");
        assertEquals("Unknown state code.", result.explanation());
        assertEquals(-1, result.distanceKm(), "Distance is sentinel -1 when a state is unrecognized, per the source code.");
    }
}
