package com.annasuraksha.service.fabric;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Query service to read anchor records from chain via FabricGatewayService/SDK.
 * Uses the FabricGatewayService to call a chaincode query function if available.
 */
@Service
public class FabricQueryService {

    private final FabricGatewayService gateway;

    public FabricQueryService(FabricGatewayService gateway) {
        this.gateway = gateway;
    }

    /**
     * Query the chain for an anchor record. Returns a map with parsed fields or null.
     */
    public Map<String, String> queryAnchor(String snapshotId) {
        try {
            Map<String, String> meta = gateway.queryAnchor(snapshotId);
            return meta;
        } catch (UnsupportedOperationException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
