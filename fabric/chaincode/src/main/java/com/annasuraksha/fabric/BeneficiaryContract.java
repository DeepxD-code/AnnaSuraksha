package com.annasuraksha.fabric;

import com.annasuraksha.fabric.model.BeneficiaryPrivate;
import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;

@Contract(name = "BeneficiaryContract")
@Default
public class BeneficiaryContract implements ContractInterface {

    private final Gson gson = new Gson();

    @Transaction()
    public String createBeneficiary(Context ctx, String id, String privateDataJson) {
        ChaincodeStub stub = ctx.getStub();
        // Store private data in a private collection named "collectionBeneficiary"
        stub.putPrivateData("collectionBeneficiary", id, privateDataJson);

        // Also store a public digest (hash) on the public world state for verification
        String digest = sha256(privateDataJson);
        stub.putStringState("digest_" + id, digest);
        return digest;
    }

    @Transaction()
    public String queryBeneficiaryPrivate(Context ctx, String id) {
        ChaincodeStub stub = ctx.getStub();
        byte[] data = stub.getPrivateData("collectionBeneficiary", id);
        return data == null ? "" : new String(data);
    }

    @Transaction()
    public String queryDigest(Context ctx, String id) {
        ChaincodeStub stub = ctx.getStub();
        String digest = stub.getStringState("digest_" + id);
        return digest == null ? "" : digest;
    }

    @Transaction()
    public String anchorSnapshot(Context ctx, String snapshotId, String merkleRoot) {
        ChaincodeStub stub = ctx.getStub();
        String key = "anchor_" + snapshotId;
        String payload = gson.toJson(new AnchorRecord(snapshotId, merkleRoot, ctx.getClientIdentity().getId(), System.currentTimeMillis()));
        stub.putStringState(key, payload);
        return key;
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return javax.xml.bind.DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class AnchorRecord {
        public String snapshotId;
        public String merkleRoot;
        public String submittedBy;
        public long timestamp;
        public AnchorRecord(String snapshotId, String merkleRoot, String submittedBy, long timestamp) {
            this.snapshotId = snapshotId; this.merkleRoot = merkleRoot; this.submittedBy = submittedBy; this.timestamp = timestamp;
        }
    }
}
