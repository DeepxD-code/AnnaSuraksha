#!/usr/bin/env bash
set -euo pipefail

if [ -z "${FABRIC_SAMPLES_PATH:-}" ]; then
  echo "FABRIC_SAMPLES_PATH must be set to your fabric-samples checkout" >&2
  exit 2
fi

CHAINCODE_PATH="${1:-$(pwd)/fabric/chaincode}"

echo "Building chaincode at: $CHAINCODE_PATH"
(cd "$CHAINCODE_PATH" && mvn -DskipTests clean package)

echo "Deploying chaincode to test-network using fabric-samples at $FABRIC_SAMPLES_PATH"
cd "$FABRIC_SAMPLES_PATH/test-network"
./network.sh deployCC -ccn beneficiary -ccp "$CHAINCODE_PATH" -ccl java

echo "Deployment complete."
