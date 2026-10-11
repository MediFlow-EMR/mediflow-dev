# 버전별 자동 마이그레이션

## 적용 순서

`prod` 프로필에서 Flyway가 JAR에 포함된 `backend/src/main/resources/db/migration/V*.sql`을 앱 시작 시 실행합니다. 성공 후 Hibernate가 `ddl-auto=validate`로 엔티티와 테이블을 검사하고 서버가 기동합니다. 실행 기록과 체크섬은 `flyway_schema_history`에 남습니다. 재시작 시 성공한 버전을 다시 실행하지 않으며, 이미 배포한 SQL 파일은 수정하지 않고 다음 버전 파일을 추가합니다.

현재 마이그레이션은 **기존 MediFlow 스키마**를 대상으로 합니다. 빈 DB 전체를 구축하는 스크립트가 아닙니다. `V20261011__handover_drafts_and_revisions.sql`은 인수인계 컬럼과 이력 테이블을 추가하며, 기존 AI 본문을 그대로 최종본에 보존합니다. `20261011-handover-drafts.sql`은 동일 내용의 수동 적용용 사본입니다. 자동 적용 환경에서는 수동 실행이 필요하지 않습니다.

## 기존 운영 DB 최초 전환

1. 연결 대상 DB 이름과 백업·복구 가능 상태를 확인합니다. 배포 스크립트는 EC2의 `/opt/mediflow/backups`에 권한 0600의 압축 논리 백업을 만들고 gzip 무결성을 검사한 뒤에만 교체합니다. 서버 외부 복구가 필요한 경우 RDS 스냅샷을 별도로 유지합니다.
2. 기존 스키마가 이미 있는 DB에 Flyway 이력이 없는 경우, 첫 배포에만 GitHub production 환경 변수 `FLYWAY_BASELINE_ON_MIGRATE=true`를 설정합니다. 배포 스크립트가 실행 중인 서비스에 일시적으로 전달합니다.
3. 배포하면 기존 스키마를 `20261010` 기준선으로 등록하고 그 이후 SQL을 자동 실행합니다. 기준선 등록은 기존 테이블이나 데이터를 삭제하지 않습니다.
4. 건강 상태와 `flyway_schema_history`의 성공 기록, 기존 기록 보존을 확인합니다. 이후 GitHub production 변수는 `false`로 돌립니다. systemd의 영구 안전 설정은 prod/validate/기준선 false를 유지하고, 첫 실행의 기준선 설정 파일은 배포 종료 시 제거합니다.

최초 전환 기본값은 `false`입니다. 대상 확인 없이 임의의 DB를 자동 기준선으로 등록하지 않기 위한 설정입니다. DB 계정에는 해당 스키마의 ALTER/CREATE/INDEX 및 필요한 DML 권한이 있어야 합니다. 운영 접근 권한·백업이 준비되기 전에는 원격 적용하지 않습니다.

## 실패와 롤백

마이그레이션 실패 또는 체크섬 불일치 시 앱 시작이 실패합니다. EC2 배포 스크립트의 건강 상태와 Flyway 성공 이력 검사도 실패합니다. JAR 롤백은 DB 변경을 되돌리지 않습니다. MariaDB DDL은 완전한 트랜잭션 롤백을 보장하지 않으므로 백업·추가형 변경·실패 원인 확인이 필요합니다. 실패 이력을 지우거나 `repair`를 무조건 실행하지 않습니다. Flyway clean은 비활성화했습니다.

## 로컬 환경

기존 MariaDB 보존: `bash gradlew bootRun --args='--spring.profiles.active=local'`. `local`은 독립 설정 파일로 `validate`만 수행하며, Flyway 및 데모 초기화를 실행하지 않습니다. `dev` 기본값도 `validate`입니다. 빈 테스트 DB 생성은 별도 격리된 DB에서 명시적으로 `JPA_DDL_AUTO=create`를 설정한 경우에만 수행합니다. 운영 DB에 `create`를 설정하지 않습니다.

## 기록 권한

초안은 작성자만 조회·수정합니다. 승인 기록 및 이력은 같은 부서에서 조회하고, 작성자만 다시 수정합니다. 승인 기록은 초안으로 되돌리거나 AI 원본·작성 구분을 바꿀 수 없습니다. 저장마다 내용 스냅샷을 동일 트랜잭션에 추가합니다. 기존 기록은 첫 수정 전에 이전 내용도 보존합니다. PUT 요청에는 조회한 `version`이 필요하며 오래된 버전은 HTTP 409입니다. 이력의 사용자·기록 ID는 물리적 FK가 없는 스냅샷 식별자입니다.

공식 참고: [Flyway 기존 스키마 기준선](https://documentation.red-gate.com/fd/baselines-273973441.html), [MariaDB 지원](https://documentation.red-gate.com/flyway/reference/database-driver-reference/mariadb).
