package com.annasuraksha.service.fabric;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("fabricGatewayService")
public class MockFabricGatewayService implements FabricGatewayService {

    private final Map<String, String> anchorStorage = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> anchorSnapshot(String snapshotId, String merkleRoot) {
        String txId = "mock-tx-" + snapshotId + "-" + System.currentTimeMillis();
        anchorStorage.put(snapshotId, merkleRoot);
        Map<String, String> meta = new HashMap<>();
        meta.put("txId", txId);
        meta.put("merkleRoot", merkleRoot);
        meta.put("chain", "fabric-mock");
        return meta;
    }

    @Override
    public Map<String, String> queryAnchor(String snapshotId) {
        String root = anchorStorage.getOrDefault(snapshotId, "");
        Map<String, String> meta = new HashMap<>();
        meta.put("txId", "mock-tx-" + snapshotId + "-" + System.currentTimeMillis());
        meta.put("merkleRoot", root);
        meta.put("chain", "fabric-mock");
        return meta;
    }
}