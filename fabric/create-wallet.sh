#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 2 ]; then
  echo "Usage: $0 <fabric-samples-path> <output-wallet-dir>" >&2
  exit 2
fi
FS="$1"
OUT="$2"

mkdir -p "$OUT"
echo "Copying Org1 Admin cert and key to wallet: $OUT"

cp "$FS/test-network/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/"* "$OUT/admin-cert.pem"
cp "$FS/test-network/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/"* "$OUT/admin-key.pem"

echo "Wallet prepared. You may need to rename files depending on SDK expectations."
