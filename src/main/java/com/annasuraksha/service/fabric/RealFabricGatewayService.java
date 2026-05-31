package com.annasuraksha.service.fabric;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

// Fabric Gateway imports are optional; RealFabricGatewayService will attempt to use them when available
// and fails fast with a clear error message when not present.
// We import reflectively to avoid hard compile-time dependency when the SDK artifact isn't available.

/**
 * Minimal Fabric Gateway skeleton. This implementation is non-functional unless Fabric Java SDK
 * libraries and a connection profile + wallet are provided. The class is guarded by FabricProperties.enabled
 * so the application will use the MockFabricGatewayService by default unless configured.
 */
@Service
@ConditionalOnProperty(name = "fabric.enabled", havingValue = "true")
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

        if (!StringUtils.hasText(props.getConnectionProfile()) || !StringUtils.hasText(props.getIdentity()) || !StringUtils.hasText(props.getWalletPath())) {
            throw new IllegalStateException("Fabric properties not configured: connectionProfile, walletPath and identity required");
        }

        // Use reflection to interact with Fabric Gateway classes if they're present at runtime.
        Path walletPath = Paths.get(props.getWalletPath());
        Path networkConfigPath = Paths.get(props.getConnectionProfile());

        try {
            Class<?> WalletsClass = Class.forName("org.hyperledger.fabric.gateway.Wallets");
            Class<?> WalletClass = Class.forName("org.hyperledger.fabric.gateway.Wallet");
            Class<?> GatewayClass = Class.forName("org.hyperledger.fabric.gateway.Gateway");

            // Wallets.newFileSystemWallet(walletPath)
            java.lang.reflect.Method newFileSystemWallet = WalletsClass.getMethod("newFileSystemWallet", Path.class);
            Object wallet = newFileSystemWallet.invoke(null, walletPath);

            // wallet.get(identity)
            java.lang.reflect.Method walletGet = WalletClass.getMethod("get", String.class);
            Object identityEntry = walletGet.invoke(wallet, props.getIdentity());
            if (identityEntry == null) {
                throw new IllegalStateException("Identity not found in wallet: " + props.getIdentity());
            }

            // Gateway.Builder builder = Gateway.createBuilder();
            java.lang.reflect.Method createBuilder = GatewayClass.getMethod("createBuilder");
            Object builder = createBuilder.invoke(null);

            // builder.identity(wallet, identity).networkConfig(networkConfigPath)
            java.lang.reflect.Method identityMethod = builder.getClass().getMethod("identity", WalletClass, String.class);
            Object withIdentity = identityMethod.invoke(builder, wallet, props.getIdentity());
            java.lang.reflect.Method networkConfigMethod = withIdentity.getClass().getMethod("networkConfig", Path.class);
            Object configured = networkConfigMethod.invoke(withIdentity, networkConfigPath);

            // try (Gateway gateway = builder.connect()) { ... }
            java.lang.reflect.Method connectMethod = configured.getClass().getMethod("connect");
            Object gateway = connectMethod.invoke(configured);

            try {
                java.lang.reflect.Method getNetwork = gateway.getClass().getMethod("getNetwork", String.class);
                Object network = getNetwork.invoke(gateway, props.getChannel());

                java.lang.reflect.Method getContract = network.getClass().getMethod("getContract", String.class);
                Object contract = getContract.invoke(network, props.getChaincode());

                java.lang.reflect.Method submitTransaction = contract.getClass().getMethod("submitTransaction", String.class, String.class, String.class);
                Object result = submitTransaction.invoke(contract, "anchorSnapshot", snapshotId, merkleRoot);
                String txId = result != null ? result.toString() : "";
                Map<String, String> meta = new HashMap<>();
                meta.put("txId", txId);
                meta.put("chain", props.getChannel());
                return meta;
            } finally {
                // gateway.close() if available
                try {
                    java.lang.reflect.Method close = gateway.getClass().getMethod("close");
                    close.invoke(gateway);
                } catch (NoSuchMethodException ns) { /* ignore */ }
            }
        } catch (ClassNotFoundException cnf) {
            throw new IllegalStateException("Fabric Gateway SDK not available on classpath. Add the dependency or use the mock implementation.", cnf);
        }
        
    }

    @Override
    public Map<String, String> queryAnchor(String snapshotId) throws Exception {
        if (!props.isEnabled()) return null;

        if (!StringUtils.hasText(props.getConnectionProfile()) || !StringUtils.hasText(props.getIdentity()) || !StringUtils.hasText(props.getWalletPath())) {
            throw new IllegalStateException("Fabric properties not configured: connectionProfile, walletPath and identity required");
        }

        Path walletPath = Paths.get(props.getWalletPath());
        Path networkConfigPath = Paths.get(props.getConnectionProfile());

        try {
            Class<?> WalletsClass = Class.forName("org.hyperledger.fabric.gateway.Wallets");
            Class<?> WalletClass = Class.forName("org.hyperledger.fabric.gateway.Wallet");
            Class<?> GatewayClass = Class.forName("org.hyperledger.fabric.gateway.Gateway");

            java.lang.reflect.Method newFileSystemWallet = WalletsClass.getMethod("newFileSystemWallet", Path.class);
            Object wallet = newFileSystemWallet.invoke(null, walletPath);

            java.lang.reflect.Method walletGet = WalletClass.getMethod("get", String.class);
            Object identityEntry = walletGet.invoke(wallet, props.getIdentity());
            if (identityEntry == null) {
                throw new IllegalStateException("Identity not found in wallet: " + props.getIdentity());
            }

            java.lang.reflect.Method createBuilder = GatewayClass.getMethod("createBuilder");
            Object builder = createBuilder.invoke(null);

            java.lang.reflect.Method identityMethod = builder.getClass().getMethod("identity", WalletClass, String.class);
            Object withIdentity = identityMethod.invoke(builder, wallet, props.getIdentity());
            java.lang.reflect.Method networkConfigMethod = withIdentity.getClass().getMethod("networkConfig", Path.class);
            Object configured = networkConfigMethod.invoke(withIdentity, networkConfigPath);

            java.lang.reflect.Method connectMethod = configured.getClass().getMethod("connect");
            Object gateway = connectMethod.invoke(configured);

            try {
                java.lang.reflect.Method getNetwork = gateway.getClass().getMethod("getNetwork", String.class);
                Object network = getNetwork.invoke(gateway, props.getChannel());

                java.lang.reflect.Method getContract = network.getClass().getMethod("getContract", String.class);
                Object contract = getContract.invoke(network, props.getChaincode());

                java.lang.reflect.Method evaluateTransaction = contract.getClass().getMethod("evaluateTransaction", String.class, String.class);
                Object result = evaluateTransaction.invoke(contract, "queryAnchor", snapshotId);
                String json = result != null ? result.toString() : "";
                java.util.Map<String, String> meta = new java.util.HashMap<>();
                if (!json.isBlank()) {
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    java.util.Map m = om.readValue(json, java.util.Map.class);
                    m.forEach((k, v) -> meta.put(String.valueOf(k), String.valueOf(v)));
                }
                return meta;
            } finally {
                try { java.lang.reflect.Method close = gateway.getClass().getMethod("close"); close.invoke(gateway); } catch (NoSuchMethodException ns) {}
            }
        } catch (ClassNotFoundException cnf) {
            throw new IllegalStateException("Fabric Gateway SDK not available on classpath. Add the dependency or use the mock implementation.", cnf);
        }
    }
}
