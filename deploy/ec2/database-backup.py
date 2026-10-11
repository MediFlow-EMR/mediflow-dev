#!/usr/bin/env python3
"""Back up the configured database without exposing credentials in process arguments."""
import gzip
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone


def main():
    env = {}
    for line in Path('/etc/mediflow/mediflow.env').read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        parts = shlex.split(value, comments=False)
        env[key.strip()] = parts[0] if len(parts) == 1 else value
    database = env['DB_NAME']
    if not re.fullmatch(r'[A-Za-z0-9_]+', database):
        raise ValueError('Invalid configured database name')
    dump = shutil.which('mariadb-dump') or shutil.which('mysqldump')
    if not dump:
        raise RuntimeError('Database backup client is missing')
    def quote(value):
        return '"' + str(value).replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n') + '"'
    config = '[client]\n' + ''.join(f'{key}={quote(value)}\n' for key, value in {
        'host': env['DB_HOST'], 'port': env.get('DB_PORT', '3306'),
        'user': env['DB_USERNAME'], 'password': env['DB_PASSWORD'],
    }.items())
    if env.get('DB_SSL_MODE', 'verify-full') != 'disable':
        config += 'ssl\nssl-verify-server-cert\nssl-ca=' + quote(env.get('DB_SSL_CERT', '/etc/pki/tls/certs/rds-global-bundle.pem')) + '\n'
    backups = Path('/opt/mediflow/backups')
    backups.mkdir(mode=0o700, exist_ok=True)
    os.chmod(backups, 0o700)
    with tempfile.NamedTemporaryFile(mode='w', prefix='mediflow-db-', delete=True) as credentials:
        credentials.write(config)
        credentials.flush()
        def query(sql):
            return subprocess.check_output(['mariadb', f'--defaults-extra-file={credentials.name}', '--batch', '--skip-column-names', database, '-e', sql], text=True).strip()
        if len(query("SHOW TABLES WHERE Tables_in_" + database + " IN ('patient','nursing_note','handover')").splitlines()) != 3:
            raise RuntimeError('Expected MediFlow tables are missing; refusing migration baseline')
        if len(sys.argv) > 1 and sys.argv[1] == 'verify':
            latest = query('SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1')
            if not latest or latest.split()[-1] != '1':
                raise RuntimeError('Flyway migration has not succeeded')
            print('Flyway migration:', latest)
            print('Clinical row counts:', query('SELECT (SELECT COUNT(*) FROM patient), (SELECT COUNT(*) FROM nursing_note), (SELECT COUNT(*) FROM handover)'))
            return
        path = backups / (database + '-' + datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ') + '.sql.gz')
        with path.open('xb') as target:
            os.chmod(path, 0o600)
            process = subprocess.Popen([dump, f'--defaults-extra-file={credentials.name}', '--single-transaction', '--routines', '--triggers', '--events', '--hex-blob', '--no-tablespaces', database], stdout=subprocess.PIPE)
            with gzip.GzipFile(fileobj=target, mode='wb') as compressed:
                shutil.copyfileobj(process.stdout, compressed)
            if process.wait() != 0:
                raise RuntimeError('Database backup failed; deployment stopped')
        with gzip.open(path, 'rb') as source:
            while source.read(1024 * 1024):
                pass
        print('Database backup verified:', path.name)
        print('Clinical row counts:', query('SELECT (SELECT COUNT(*) FROM patient), (SELECT COUNT(*) FROM nursing_note), (SELECT COUNT(*) FROM handover)'))


if __name__ == '__main__':
    main()
