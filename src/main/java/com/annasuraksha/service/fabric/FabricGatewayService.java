package com.annasuraksha.service.fabric;

import java.util.Map;

/**
 * Fabric gateway abstraction. Implementations should anchor snapshots on the ledger and return
 * a map with metadata like txId and chain name.
 */
public interface FabricGatewayService {
    /**
     * Anchor a snapshot root on-chain. Returns metadata map or null on failure.
     */
    Map<String, String> anchorSnapshot(String snapshotId, String merkleRoot) throws Exception;
}
