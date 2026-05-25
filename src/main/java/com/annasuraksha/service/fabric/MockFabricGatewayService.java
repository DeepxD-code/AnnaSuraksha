package com.annasuraksha.service.fabric;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("fabricGatewayService")
public class MockFabricGatewayService implements FabricGatewayService {
    @Override
    public Map<String, String> anchorSnapshot(String snapshotId, String merkleRoot) {
        // Mock implementation: return a fake tx id
        Map<String, String> meta = new HashMap<>();
        meta.put("txId", "mock-tx-" + snapshotId + "-" + System.currentTimeMillis());
        meta.put("chain", "fabric-mock");
        return meta;
    }

    @Override
    public Map<String, String> queryAnchor(String snapshotId) {
        Map<String, String> meta = new HashMap<>();
        meta.put("txId", "mock-tx-" + snapshotId + "-" + System.currentTimeMillis());
        meta.put("merkleRoot", "");
        meta.put("chain", "fabric-mock");
        return meta;
    }
}
