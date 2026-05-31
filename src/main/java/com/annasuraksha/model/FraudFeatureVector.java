package com.annasuraksha.model;

public record FraudFeatureVector(
    double duplicateAadhaarSignal,
    double duplicateVoterIdSignal,
    double duplicatePhoneSignal,
    double rationUsageAnomalyScore,
    double categoryMismatchSignal,
    double familySizeAnomalySignal,
    double dealerDiversionRate,
    double multiShopClaimSignal,
    double impossibleTravelSignal,
    double crossStateFraudSignal,
    double claimFrequencyAnomaly,
    double nightTimeClaimSignal,
    double bulkClaimBurstScore,
    double registrationVelocitySignal,
    double newFpsShopSignal,
    double districtBaseRiskScore,
    double confidenceScore,
    int    evidenceCount,
    Long   beneficiaryId,
    String beneficiaryName,
    String stateCode,
    String category
) {
    public double rawSignalSum() {
        return duplicateAadhaarSignal + duplicateVoterIdSignal + duplicatePhoneSignal
             + rationUsageAnomalyScore + categoryMismatchSignal + familySizeAnomalySignal
             + dealerDiversionRate + multiShopClaimSignal
             + impossibleTravelSignal + crossStateFraudSignal
             + claimFrequencyAnomaly + nightTimeClaimSignal
             + bulkClaimBurstScore + registrationVelocitySignal
             + newFpsShopSignal + districtBaseRiskScore;
    }

    public int activeSignalCount() {
        int count = 0;
        if (duplicateAadhaarSignal > 0) count++;
        if (duplicateVoterIdSignal > 0) count++;
        if (duplicatePhoneSignal > 0) count++;
        if (rationUsageAnomalyScore > 0.3) count++;
        if (categoryMismatchSignal > 0) count++;
        if (familySizeAnomalySignal > 0) count++;
        if (dealerDiversionRate > 0.2) count++;
        if (multiShopClaimSignal > 0) count++;
        if (impossibleTravelSignal > 0) count++;
        if (crossStateFraudSignal > 0) count++;
        if (claimFrequencyAnomaly > 0.3) count++;
        if (nightTimeClaimSignal > 0) count++;
        if (bulkClaimBurstScore > 0) count++;
        if (registrationVelocitySignal > 0) count++;
        if (newFpsShopSignal > 0) count++;
        if (districtBaseRiskScore > 0) count++;
        return count;
    }
}
