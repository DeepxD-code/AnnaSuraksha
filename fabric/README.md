Permissioned Blockchain (Hyperledger Fabric) integration
=====================================================

Goal
----
This directory contains design notes and a starter Java chaincode module for integrating
AnnaSuraksha with a permissioned blockchain (Hyperledger Fabric). The intent is to
provide a production-grade, privacy-preserving ledger alternative to the current
in-database SHA256 chain used in the demo.

Why Fabric
----------
- Permissioned (consortium) model matches government deployments where multiple state
  or central agencies may run peers.
- Private Data Collections allow storing PII off-ledger while keeping hashes on the ledger.
- Pluggable consensus and ACLs give governance and performance control.

High-level design
-----------------
- Chaincode (this module) implements canonical operations:
  - CreateBeneficiary (store private data in a private collection, publish public digest)
  - QueryBeneficiary (read from private collection for authorized peers)
  - AnchorSnapshot (store Merkle root or snapshot metadata on public ledger)
  - QueryAnchor (fetch anchor record)
- Private data collections: `collectionBeneficiary` (permitting only authorized orgs to read PII).
- Endorsement policy: configurable per channel; for pilot use single-org endorsement.
- Identity: use Fabric CAs and MSPs for admins/peers; apps authenticate with client certificates.

Starter files
-------------
- fabric/chaincode : Java chaincode (Maven) skeleton using Fabric Java Contract API.

Deployment notes (pilot)
-----------------------
1. Install Hyperledger Fabric samples and prerequisites (docker, docker-compose, fabric-samples).
2. Use `fabric-samples/test-network` to create a channel and deploy the chaincode package.
3. Create a private data collection configuration for `collectionBeneficiary` and include in chaincode definition.
4. Use the AnnaSuraksha server to call chaincode via Fabric SDK (Java or Node) for operations.

Security & privacy
------------------
- Never write raw PII values to the channel's public state.
- Private data collections are stored off-chain on endorsing peers but hashed on the ledger.
- Use a backup and audit policy for private data; enable pvtdata dissemination controls.

Next steps
----------
1. Flesh out chaincode with the exact data model used by AnnaSuraksha (fields, hashes).
2. Add Fabric SDK integration in the main app (a service that submits transactions and queries).
3. Provide Docker-based test-network scripts (or use `fabric-samples`) and CI steps to package chaincode.

References
----------
- Hyperledger Fabric docs: https://hyperledger-fabric.readthedocs.io/
- Fabric Java Contract API examples: https://github.com/hyperledger/fabric-samples
