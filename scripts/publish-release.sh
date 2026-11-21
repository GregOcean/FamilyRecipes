#!/usr/bin/env bash
# Upload an already-built bundle to the existing server and deploy it.
set -euo pipefail
fr_root="$(cd "$(dirname "$0")/.." && pwd)"
fr_version="${1:?用法: bash scripts/publish-release.sh 1.0.12}"
[[ "$fr_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo '版本格式错误'; exit 1; }
fr_bundle="$fr_root/familyrecipes-release/$fr_version"
[[ -f "$fr_bundle/SHA256SUMS" && -f "$fr_bundle/VERSION" ]] || { echo "发布包不存在：$fr_bundle"; exit 1; }
[[ "$(cat "$fr_bundle/VERSION")" == "$fr_version" ]] || { echo '发布包版本不一致'; exit 1; }
(cd "$fr_bundle" && shasum -a 256 -c SHA256SUMS)
fr_remote="root@152.42.242.92"
fr_target="/root/familyrecipes-release/$fr_version"
ssh "$fr_remote" "mkdir -p '$fr_target'"
scp "$fr_bundle"/* "$fr_remote:$fr_target/"
ssh "$fr_remote" "cd '$fr_target' && bash deploy-current-server.sh '$fr_version'"
