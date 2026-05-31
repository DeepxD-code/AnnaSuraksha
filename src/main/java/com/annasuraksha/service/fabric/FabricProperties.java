package com.annasuraksha.service.fabric;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fabric")
public class FabricProperties {
    private boolean enabled = false;
    private String connectionProfile; // path to connection profile YAML/JSON
    private String walletPath; // path to wallet directory
    private String identity; // identity label
    private String channel = "mychannel";
    private String chaincode = "beneficiary";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getConnectionProfile() { return connectionProfile; }
    public void setConnectionProfile(String connectionProfile) { this.connectionProfile = connectionProfile; }
    public String getWalletPath() { return walletPath; }
    public void setWalletPath(String walletPath) { this.walletPath = walletPath; }
    public String getIdentity() { return identity; }
    public void setIdentity(String identity) { this.identity = identity; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getChaincode() { return chaincode; }
    public void setChaincode(String chaincode) { this.chaincode = chaincode; }
}
