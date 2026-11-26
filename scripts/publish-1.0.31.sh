#!/usr/bin/env bash
# 1.0.31 一键发布：本地无发布包时先构建，再上传并在服务器执行部署。
# 用法：bash scripts/publish-1.0.31.sh
set -euo pipefail

fr_root="$(cd "$(dirname "$0")/.." && pwd)"
fr_version="1.0.31"
fr_bundle="$fr_root/familyrecipes-release/$fr_version"

if [[ ! -f "$fr_bundle/SHA256SUMS" || ! -f "$fr_bundle/VERSION" ]]; then
    echo "未找到 $fr_version 发布包，先执行本地构建…"
    bash "$fr_root/scripts/prepare-release.sh" "$fr_version"
fi

[[ "$(cat "$fr_bundle/VERSION")" == "$fr_version" ]] || { echo "发布包版本不一致"; exit 1; }
(cd "$fr_bundle" && shasum -a 256 -c SHA256SUMS)

echo "上传并部署 $fr_version 到生产服务器…"
bash "$fr_root/scripts/publish-release.sh" "$fr_version"
