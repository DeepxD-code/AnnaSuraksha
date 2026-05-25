package com.annasuraksha.service.fabric;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal Fabric Gateway skeleton. This implementation is non-functional unless Fabric Java SDK
 * libraries and a connection profile + wallet are provided. The class is guarded by FabricProperties.enabled
 * so the application will use the MockFabricGatewayService by default unless configured.
 */
@Service
public class RealFabricGatewayService implements FabricGatewayService {

    private final FabricProperties props;

    public RealFabricGatewayService(FabricProperties props) {
        this.props = props;
    }

    @Override
    public Map<String, String> anchorSnapshot(String snapshotId, String merkleRoot) throws Exception {
        if (!props.isEnabled()) return null;

        // NOTE: This is a skeleton. To enable real Fabric integration:
        // 1. Add Fabric Gateway Java SDK dependency
        // 2. Load connection profile from props.getConnectionProfile()
        // 3. Load wallet and identity from props.getWalletPath() and props.getIdentity()
        // 4. Connect to the Gateway, get network channel and contract
        // 5. Submit transaction: contract.submitTransaction("anchorSnapshot", snapshotId, merkleRoot)

        if (!StringUtils.hasText(props.getConnectionProfile()) || !StringUtils.hasText(props.getIdentity())) {
            throw new IllegalStateException("Fabric properties not configured: connectionProfile and identity required");
        }

        // For now, return a placeholder indicating the intended behavior.
        Map<String, String> meta = new HashMap<>();
        meta.put("txId", "fabric-skeleton-" + snapshotId + "-" + System.currentTimeMillis());
        meta.put("chain", "fabric-skeleton");
        return meta;
    }
}
