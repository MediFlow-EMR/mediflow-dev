#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -lt 1 || $# -gt 2 || "$1" != s3://* ]]; then
  echo "usage: deploy.sh s3://bucket/key.jar [baseline-existing:true|false]" >&2
  exit 2
fi

artifact_uri="$1"
app_dir="/opt/mediflow"
next_jar="${app_dir}/app.next.jar"
current_jar="${app_dir}/app.jar"
backup_jar="${app_dir}/app.previous.jar"

baseline_existing="${2:-false}"
[[ "$baseline_existing" == true || "$baseline_existing" == false ]]
python3 "${app_dir}/bin/database-backup.py"
install -d -m 0755 /etc/systemd/system/mediflow.service.d
cat > /etc/systemd/system/mediflow.service.d/00-safety.conf <<EOF
[Service]
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=TZ=Asia/Seoul
Environment=DEMO_DAILY_REFRESH_ENABLED=true
Environment=JPA_DDL_AUTO=validate
Environment=FLYWAY_BASELINE_ON_MIGRATE=false
EOF
install -d -m 0755 /run/systemd/system/mediflow.service.d
cat > /run/systemd/system/mediflow.service.d/migration.conf <<EOF
[Service]
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=TZ=Asia/Seoul
Environment=DEMO_DAILY_REFRESH_ENABLED=true
Environment=JPA_DDL_AUTO=validate
Environment=FLYWAY_BASELINE_ON_MIGRATE=${baseline_existing}
EOF
cleanup() {
  rm -f /run/systemd/system/mediflow.service.d/migration.conf
  systemctl daemon-reload
}
trap cleanup EXIT
systemctl daemon-reload

aws s3 cp "$artifact_uri" "$next_jar"
chown mediflow:mediflow "$next_jar"
chmod 0640 "$next_jar"

if [[ -f "$current_jar" ]]; then
  cp -f "$current_jar" "$backup_jar"
fi
mv -f "$next_jar" "$current_jar"

systemctl restart mediflow

for _ in {1..30}; do
  if curl --fail --silent http://127.0.0.1:9005/actuator/health | grep -q '"status":"UP"' && python3 "${app_dir}/bin/database-backup.py" verify; then
    echo "MediFlow deployment healthy"
    exit 0
  fi
  sleep 2
done

echo "Health check failed; rolling back" >&2
if [[ -f "$backup_jar" ]]; then
  mv -f "$backup_jar" "$current_jar"
  chown mediflow:mediflow "$current_jar"
  cleanup
  systemctl restart mediflow
fi
exit 1
