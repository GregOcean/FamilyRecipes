#!/usr/bin/env bash
set -euo pipefail
fr_root="$(cd "$(dirname "$0")/.." && pwd)"
fr_remote=root@152.42.242.92
# A private, temporary multiplexing socket allows one password prompt for this run.
fr_socket_dir=$(mktemp -d)
trap 'ssh -S "$fr_socket_dir/control" -O exit "$fr_remote" >/dev/null 2>&1 || true; rm -rf "$fr_socket_dir"' EXIT
ssh -M -S "$fr_socket_dir/control" -o ControlPersist=60 -fnNT "$fr_remote"
ssh -S "$fr_socket_dir/control" "$fr_remote" 'mkdir -p /root/familyrecipes-catalog-pilot-upload'
scp -o "ControlPath=$fr_socket_dir/control" "$fr_root/backend/src/main/resources/catalog/worker.py" "$fr_root/scripts/catalog-pilot/install-server.sh" "$fr_remote:/root/familyrecipes-catalog-pilot-upload/"
ssh -S "$fr_socket_dir/control" "$fr_remote" 'bash /root/familyrecipes-catalog-pilot-upload/install-server.sh'
