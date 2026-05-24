package com.annasuraksha.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyChainRepository extends JpaRepository<SupplyChainEntry, Long> {
    List<SupplyChainEntry> findByShipmentIdOrderByEntryHeightAsc(String shipmentId);
    List<SupplyChainEntry> findByDiscrepancyFlaggedTrueOrderByCreatedAtDesc();
    List<SupplyChainEntry> findByFpsShopIdOrderByCreatedAtDesc(String fpsShopId);
    List<SupplyChainEntry> findByStateCodeOrderByCreatedAtDesc(String stateCode);
    List<SupplyChainEntry> findByStageOrderByCreatedAtDesc(SupplyChainEntry.Stage stage);
    long countByDiscrepancyFlaggedTrue();

    @Query("SELECT SUM(e.riceDiscrepancyKg) FROM SupplyChainEntry e WHERE e.riceDiscrepancyKg > 0")
    Long sumRiceDiscrepancy();

    @Query("SELECT SUM(e.wheatDiscrepancyKg) FROM SupplyChainEntry e WHERE e.wheatDiscrepancyKg > 0")
    Long sumWheatDiscrepancy();

    @Query("SELECT e FROM SupplyChainEntry e ORDER BY e.entryHeight DESC LIMIT 1")
    Optional<SupplyChainEntry> findLatestEntry();
}
