#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script with sudo." >&2
  exit 1
fi

if ! id mediflow >/dev/null 2>&1; then
  useradd --system --home-dir /opt/mediflow --shell /sbin/nologin mediflow
fi

install -d -o mediflow -g mediflow -m 0750 /opt/mediflow
install -d -o root -g root -m 0750 /opt/mediflow/bin
install -d -o root -g mediflow -m 0750 /etc/mediflow
touch /etc/mediflow/mediflow.env
chown root:mediflow /etc/mediflow/mediflow.env
chmod 0640 /etc/mediflow/mediflow.env

curl -fsSL \
  -o /etc/pki/tls/certs/rds-global-bundle.pem \
  https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
chmod 0644 /etc/pki/tls/certs/rds-global-bundle.pem

cat > /opt/mediflow/bin/deploy.sh <<'DEPLOY_SCRIPT'
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
DEPLOY_SCRIPT
chown root:root /opt/mediflow/bin/deploy.sh
chmod 0750 /opt/mediflow/bin/deploy.sh

cat > /etc/systemd/system/mediflow.service <<'SYSTEMD_UNIT'
[Unit]
Description=MediFlow Spring Boot backend
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=mediflow
Group=mediflow
WorkingDirectory=/opt/mediflow
EnvironmentFile=/etc/mediflow/mediflow.env
ExecStart=/usr/bin/java -XX:MaxRAMPercentage=70 -jar /opt/mediflow/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5
TimeoutStopSec=30
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
SYSTEMD_UNIT
chown root:root /etc/systemd/system/mediflow.service
chmod 0644 /etc/systemd/system/mediflow.service

cat > /etc/nginx/conf.d/mediflow.conf <<'NGINX_CONFIG'
server {
    listen 80;
    server_name api.mediflow-emr.cloud;

    client_max_body_size 10m;

    location / {
        proxy_pass http://127.0.0.1:9005;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 5s;
        proxy_read_timeout 60s;
    }
}
NGINX_CONFIG

systemctl daemon-reload
systemctl enable mediflow
nginx -t
systemctl reload nginx

echo "MediFlow EC2 bootstrap completed."
