#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 || "$1" != s3://* ]]; then
  echo "usage: deploy.sh s3://bucket/key.jar" >&2
  exit 2
fi

artifact_uri="$1"
app_dir="/opt/mediflow"
next_jar="${app_dir}/app.next.jar"
current_jar="${app_dir}/app.jar"
backup_jar="${app_dir}/app.previous.jar"

aws s3 cp "$artifact_uri" "$next_jar"
chown mediflow:mediflow "$next_jar"
chmod 0640 "$next_jar"

if [[ -f "$current_jar" ]]; then
  cp -f "$current_jar" "$backup_jar"
fi
mv -f "$next_jar" "$current_jar"

systemctl restart mediflow

for _ in {1..30}; do
  if curl --fail --silent http://127.0.0.1:9005/actuator/health | grep -q '"status":"UP"'; then
    echo "MediFlow deployment healthy"
    exit 0
  fi
  sleep 2
done

echo "Health check failed; rolling back" >&2
if [[ -f "$backup_jar" ]]; then
  mv -f "$backup_jar" "$current_jar"
  chown mediflow:mediflow "$current_jar"
  systemctl restart mediflow
fi
exit 1
