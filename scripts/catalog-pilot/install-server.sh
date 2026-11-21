#!/usr/bin/env bash
set -euo pipefail
[[ $(id -u) == 0 ]] || { echo 'Run as root'; exit 1; }
cd "$(dirname "$0")"
command -v python3 >/dev/null
command -v mysql >/dev/null
[[ -s /etc/familyrecipes/ai.env ]] || { echo 'Missing /etc/familyrecipes/ai.env'; exit 1; }
python3 -c "import ast,pathlib; ast.parse(pathlib.Path('worker.py').read_text())"
install -d -m 700 /opt/familyrecipes-catalog-pilot /var/lib/familyrecipes-catalog-pilot
# Stop the older token-heavy worker before replacing it; preserve the previous evidence.
if [[ "$(systemctl show familyrecipes-catalog-pilot.service -p LoadState --value)" != 'not-found' ]]; then
    systemctl stop familyrecipes-catalog-pilot.service
fi
fr_backup="/var/lib/familyrecipes-catalog-pilot/backup-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$fr_backup"
for fr_file in state.sqlite results.json recipes.json recipes-rules-v2.json recipes-balanced-v2.json; do
    if [[ -f "/var/lib/familyrecipes-catalog-pilot/$fr_file" ]]; then
        cp -p "/var/lib/familyrecipes-catalog-pilot/$fr_file" "$fr_backup/"
    fi
done
install -m 600 worker.py /opt/familyrecipes-catalog-pilot/worker.py
cat > /etc/systemd/system/familyrecipes-catalog-pilot.service <<'EOF'
[Unit]
Description=Family Recipes common Chinese dishes pilot (rules only, no LLM)
After=network-online.target mysql.service
Wants=network-online.target
[Service]
Type=oneshot
User=root
UMask=0077
EnvironmentFile=/etc/familyrecipes/ai.env
EnvironmentFile=-/etc/familyrecipes/catalog-pilot.env
ExecStart=/usr/bin/python3 -u /opt/familyrecipes-catalog-pilot/worker.py
TimeoutStartSec=2h
Restart=no
Nice=10
MemoryMax=512M
CPUQuota=30%
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/lib/familyrecipes-catalog-pilot
[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
# Start once; do not enable boot-time automatic spending.
systemctl start --no-block familyrecipes-catalog-pilot
printf '%s\n' 'Task submitted. Progress: journalctl -u familyrecipes-catalog-pilot -f' 'Results: /var/lib/familyrecipes-catalog-pilot/results.json' 'Pause: systemctl stop familyrecipes-catalog-pilot' 'Resume: systemctl start familyrecipes-catalog-pilot'
