#!/usr/bin/env bash
set -euo pipefail
trap 'fr_status=$?; echo "发布未完成，退出码 $fr_status；请查看上方服务器错误信息。" >&2; exit "$fr_status"' ERR
fr_root="$(cd "$(dirname "$0")/.." && pwd)"
fr_bundle="$fr_root/familyrecipes-release/1.0.5"
test -f "$fr_bundle/SHA256SUMS"
(cd "$fr_bundle" && shasum -a 256 -c SHA256SUMS)
# SSH passwords are entered only in your terminal, never in scripts or chat.
ssh root@152.42.242.92 'mkdir -p /root/familyrecipes-release/1.0.5'
scp "$fr_bundle/"* root@152.42.242.92:/root/familyrecipes-release/1.0.5/
ssh -t root@152.42.242.92 'bash /root/familyrecipes-release/1.0.5/deploy-1.0.5-server.sh'
