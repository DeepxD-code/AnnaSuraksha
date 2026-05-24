package com.annasuraksha.model;

public record FraudFeatureVector(
    double duplicateAadhaarSignal,
    double duplicateVoterIdSignal,
    double rationUsageAnomalyScore,
    double categoryMismatchSignal,
    double dealerDiversionRate,
    double multiShopClaimSignal,
    double impossibleTravelSignal,
    double crossStateFraudSignal,
    double claimFrequencyAnomaly,
    double nightTimeClaimSignal,
    double bulkClaimBurstScore,
    double newFpsShopSignal,
    double districtBaseRiskScore,
    Long   beneficiaryId,
    String beneficiaryName,
    String stateCode,
    String category
) {
    public double rawSignalSum() {
        return duplicateAadhaarSignal + duplicateVoterIdSignal
             + rationUsageAnomalyScore + categoryMismatchSignal
             + dealerDiversionRate + multiShopClaimSignal
             + impossibleTravelSignal + crossStateFraudSignal
             + claimFrequencyAnomaly + nightTimeClaimSignal
             + bulkClaimBurstScore + newFpsShopSignal + districtBaseRiskScore;
    }
}
