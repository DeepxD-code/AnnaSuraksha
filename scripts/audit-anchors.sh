#!/usr/bin/env bash
# Quick CLI audit: verify snapshot anchors against on-chain records.
# Usage: ./scripts/audit-anchors.sh <JWT_TOKEN> [BASE_URL]
set -euo pipefail
TOKEN="${1:-}"
BASE="${2:-http://localhost:8081}"
if [ -z "$TOKEN" ]; then echo "Usage: $0 <JWT_TOKEN> [BASE_URL]" >&2; exit 2; fi

echo "Fetching snapshot verification from $BASE/api/admin/ledger/verify-all ..."
RESP=$(curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/admin/ledger/verify-all")
echo "$RESP" | python -c "
import sys, json
data = json.load(sys.stdin).get('data', [])
if not data:
    print('No snapshots found.')
    sys.exit(0)
fail = 0
for s in data:
    sid = s.get('snapshotId')
    root = (s.get('root') or '')[:16]
    match = s.get('matchesOnChain')
    err = s.get('onChainError')
    status = 'OK' if match else 'MISMATCH' if match is not None and not match else 'NO_ANCHOR'
    if status != 'OK': fail += 1
    print(f'  snapshot {sid}: root={root}…  {status}' + (f'  error={err}' if err else ''))
sys.exit(1 if fail > 0 else 0)
"
RC=$?
if [ $RC -eq 0 ]; then echo "All snapshots match on-chain anchors."; else echo "WARNING: Some snapshots have anchor issues."; fi
exit $RC
