# MediFlow AWS 배포 가이드

이 문서는 AWS 웹 콘솔을 기준으로 MediFlow를 배포하는 순서입니다. 운영 대상은 다음과 같습니다.

- 프런트엔드: 비공개 S3 + CloudFront(OAC)
- 백엔드: EC2 + Spring Boot + systemd
- 데이터베이스: 비공개 RDS MariaDB
- CI/CD: GitHub Actions + AWS OIDC + Systems Manager
- 선택 확장: EventBridge + Lambda + 비공개 S3 약품정보 캐시

> 이 프로젝트는 실제 환자 데이터가 아닌 시연용 가상 데이터만 사용해야 합니다. 실제 의료정보를 올리면 안 됩니다.

## 0. 시작 전에 결정할 값

아래 값을 메모장에 적어 두고 문서 전체에서 동일하게 사용합니다.

| 항목 | 권장값 |
|---|---|
| AWS 리전 | `ap-northeast-2` |
| 프런트 도메인 | `www.mediflow-emr.cloud` |
| API 도메인 | `api.mediflow-emr.cloud` |
| 프런트 버킷 | 전 세계에서 고유한 이름, 예: `mediflow-frontend-계정ID` |
| 배포 아티팩트 버킷 | 예: `mediflow-deploy-계정ID` |
| RDS DB명 | `mediflowdb` |
| 백엔드 포트 | `9005` |

AWS 비용 알림부터 만듭니다.

1. AWS 콘솔에서 **Billing and Cost Management → Budgets**로 이동합니다.
2. 월 비용 예산을 만듭니다. 개인 포트폴리오라면 우선 USD 20 또는 본인이 감당 가능한 금액으로 설정합니다.
3. 50%, 80%, 100% 알림을 이메일로 설정합니다.

## 1. GitHub 저장소 준비

코드 저장소는 `MediFlow-EMR/mediflow-dev`, 문서 저장소는 `MediFlow-EMR/MediFlow-docs`입니다. 운영 배포는 코드 저장소 main의 GitHub Actions와 production 환경 변수로 실행합니다.

1. GitHub 저장소의 기본 브랜치를 `main`으로 확인합니다.
2. 현재 코드는 별도 브랜치(예: `codex/aws-deploy`)로 먼저 올립니다.
3. Pull Request에서 CI가 통과한 뒤 `main`에 병합합니다.
4. 영상 GIF 교체는 UI가 모두 끝난 뒤 별도 커밋으로 진행합니다.

절대로 저장소에 올리지 않을 값:

- DB 비밀번호
- Gemini/API 인증키
- OAuth Client Secret
- JWT Secret
- AWS Access Key
- EC2의 `/etc/mediflow/mediflow.env`

과거 채팅이나 화면에 노출된 API 키는 폐기하고 새 키를 발급합니다.

## 2. 네트워크와 보안 그룹

처음 배포는 기본 VPC로 진행해도 됩니다. RDS는 퍼블릭으로 열지 않습니다.

### 2.1 EC2 보안 그룹 `mediflow-ec2-sg`

인바운드:

- SSH 22: 가능하면 만들지 않고 Session Manager 사용
- HTTP 80: `0.0.0.0/0`
- HTTPS 443: `0.0.0.0/0`
- 9005: 외부에 열지 않음

### 2.2 RDS 보안 그룹 `mediflow-rds-sg`

인바운드:

- MariaDB 3306
- 소스: `mediflow-ec2-sg` 보안 그룹만 선택

`내 IP`나 `0.0.0.0/0`를 RDS에 추가하지 않습니다.

## 3. RDS MariaDB

1. **RDS → 데이터베이스 생성**을 선택합니다.
2. MariaDB, Single-AZ, 개발/테스트용 최소 인스턴스를 선택합니다.
3. DB 식별자 `mediflow-db`, 초기 DB명 `mediflowdb`를 입력합니다.
4. 퍼블릭 액세스는 **아니요**로 설정합니다.
5. `mediflow-rds-sg`를 연결합니다.
6. 자동 백업 7일, 삭제 방지 활성화를 권장합니다.
7. 생성 후 RDS 엔드포인트를 기록합니다.

운영은 `JPA_DDL_AUTO=validate`를 유지합니다. 기존 스키마의 변경은 Flyway가 앱 기동 전에 적용합니다. 현재 마이그레이션은 기존 MediFlow 스키마를 대상으로 하므로 빈 DB는 검증된 초기 스키마를 먼저 준비해야 합니다. 기존 운영 DB의 최초 기준선 등록과 백업은 [DB 전환 안내](../database/README.md)를 따릅니다.

## 4. EC2와 Systems Manager

### 4.1 EC2 IAM 역할

IAM 역할 `mediflow-ec2-role`을 만들고 다음 권한을 연결합니다.

- `AmazonSSMManagedInstanceCore`
- 배포 아티팩트 버킷의 `backend/releases/*`에 대한 `s3:GetObject`, `s3:GetObjectVersion`과 해당 접두사로 제한한 `s3:ListBucket`만 허용하는 인라인 정책

### 4.2 EC2 생성

1. Amazon Linux 2023, `t3.small` 또는 예산에 맞는 인스턴스를 선택하고 루트 EBS는 `gp3` 20 GiB로 설정합니다.
2. `mediflow-ec2-role`을 연결합니다.
3. `mediflow-ec2-sg`를 연결합니다.
4. Session Manager에서 연결되는지 확인합니다.

### 4.3 EC2 최초 설정

Session Manager 터미널에서 실행합니다.

```bash
sudo dnf update -y
sudo dnf install -y java-21-amazon-corretto-headless nginx mariadb105 curl
sudo useradd --system --home /opt/mediflow --shell /sbin/nologin mediflow
sudo mkdir -p /opt/mediflow/bin /etc/mediflow
sudo chown -R mediflow:mediflow /opt/mediflow
sudo chmod 750 /etc/mediflow
sudo curl -fsSL -o /etc/pki/tls/certs/rds-global-bundle.pem \
  https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
sudo chmod 644 /etc/pki/tls/certs/rds-global-bundle.pem
```

RDS가 `require_secure_transport=ON`인 상태를 유지하고, 운영 JDBC URL은 `sslMode=verify-full`과 위 인증서를 사용해 인증서 및 호스트 이름까지 검증합니다.

간단한 설치는 `deploy/ec2/bootstrap.sh`를 배포 버킷의 `backend/releases/bootstrap.sh`로 업로드한 뒤 EC2에서 실행합니다. 이 스크립트가 아래 파일과 권한을 함께 구성합니다.

```bash
aws s3 cp s3://YOUR_DEPLOY_BUCKET/backend/releases/bootstrap.sh /tmp/mediflow-bootstrap.sh
sudo bash /tmp/mediflow-bootstrap.sh
```

직접 구성하려면 저장소의 다음 파일 내용을 EC2에 복사합니다.

- `deploy/ec2/mediflow.service` → `/etc/systemd/system/mediflow.service`
- `deploy/ec2/deploy.sh` → `/opt/mediflow/bin/deploy.sh`
- `deploy/ec2/database-backup.py` → `/opt/mediflow/bin/database-backup.py` (배포 워크플로가 매 릴리스 설치)
- `backend/.env.prod.example`을 참고해 `/etc/mediflow/mediflow.env` 작성

권한을 설정합니다.

```bash
sudo chown root:root /etc/systemd/system/mediflow.service
sudo chmod 644 /etc/systemd/system/mediflow.service
sudo chown root:mediflow /etc/mediflow/mediflow.env
sudo chmod 640 /etc/mediflow/mediflow.env
sudo chown root:root /opt/mediflow/bin/deploy.sh
sudo chmod 750 /opt/mediflow/bin/deploy.sh
sudo systemctl daemon-reload
sudo systemctl enable mediflow
```

`SPRING_PROFILES_ACTIVE=prod,demo`는 가상 환자 시연 데이터를 생성합니다. 실제 운영 환경에서는 `prod`만 사용합니다.

## 5. 백엔드 HTTPS

개인 포트폴리오의 첫 배포는 EC2의 Nginx + ACM을 직접 결합할 수 없으므로 다음 중 하나를 선택합니다.

### 선택 A: ALB 사용

- 장점: ACM 인증서 연결과 헬스체크가 편함
- 단점: 사용량이 적어도 ALB 시간당 비용 발생

ALB를 사용하면 타깃 그룹 포트는 9005, 헬스체크는 `/actuator/health`로 설정합니다. EC2의 9005 인바운드는 ALB 보안 그룹만 허용합니다.

### 선택 B: 저비용 Nginx + Let's Encrypt

- 장점: ALB 비용 없음
- 단점: 인증서 갱신과 단일 EC2 운영 책임이 생김

첫 포트폴리오 배포는 선택 B, 실무형 아키텍처 시연이 목적이면 선택 A를 권장합니다.

## 6. 프런트 S3 + CloudFront

1. S3 버킷을 만들고 **퍼블릭 액세스 차단을 전부 유지**합니다.
2. 정적 웹사이트 호스팅은 활성화하지 않습니다.
3. CloudFront 배포를 만들고 S3 REST 원본을 선택합니다.
4. Origin Access Control(OAC)을 생성하고 `Sign requests`를 사용합니다.
5. 기본 루트 객체를 `index.html`로 설정합니다.
6. 허용 메서드는 `GET, HEAD, OPTIONS`만 선택합니다.
7. SPA 라우팅을 위해 403/404 응답을 `/index.html`, HTTP 200으로 매핑합니다.
8. CloudFront용 ACM 인증서는 반드시 `us-east-1`에서 발급합니다.
9. 가비아 DNS에서 호스트 `www`의 CNAME 값을 CloudFront 배포 도메인으로 설정합니다. 로그인 성공·실패 리다이렉트도 `https://www.mediflow-emr.cloud`를 사용합니다. 루트 도메인까지 연결하려면 별도로 Route 53 Alias 등 루트 도메인을 지원하는 방식을 구성합니다.

OAC가 제안하는 S3 버킷 정책을 적용합니다. S3 객체를 퍼블릭으로 만들지 않습니다.

## 7. GitHub Actions OIDC

AWS 액세스 키를 GitHub에 저장하지 않습니다.

### 7.1 IAM OIDC Provider

IAM → Identity providers에서 다음 공급자를 추가합니다.

- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

### 7.2 GitHub Actions 역할

`mediflow-github-actions-role`을 만들고 신뢰 정책의 저장소를 실제 값으로 바꿉니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::AWS_ACCOUNT_ID:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
      },
      "StringLike": {
        "token.actions.githubusercontent.com:sub": "repo:MediFlow-EMR/mediflow-dev:*"
      }
    }
  }]
}
```

역할 권한은 다음 범위로 제한합니다.

- 프런트 S3 버킷: ListBucket, PutObject, DeleteObject
- 배포 버킷 `backend/releases/*`: PutObject
- 지정 CloudFront 배포: CreateInvalidation
- 지정 EC2 인스턴스: SSM SendCommand/GetCommandInvocation

### 7.3 GitHub Environment 변수

GitHub 저장소 → Settings → Environments → `production`을 만들고 Variables에 등록합니다.

| 이름 | 값 예시 |
|---|---|
| `AWS_REGION` | `ap-northeast-2` |
| `AWS_GITHUB_ACTIONS_ROLE_ARN` | 위 IAM Role ARN |
| `S3_FRONTEND_BUCKET` | 프런트 버킷명 |
| `S3_DEPLOY_BUCKET` | 배포 버킷명 |
| `CLOUDFRONT_DISTRIBUTION_ID` | CloudFront ID |
| `EC2_INSTANCE_ID` | `i-...` |
| `VITE_API_BASE_URL` | `https://api.mediflow-emr.cloud` |

워크플로 파일:

- `.github/workflows/ci.yml`
- `.github/workflows/deploy-frontend.yml`
- `.github/workflows/deploy-backend.yml`

처음에는 `production` Environment에 승인 규칙을 켜서 자동 배포 전에 수동 승인을 받는 것을 권장합니다.

## 8. 첫 배포 순서

1. RDS 생성
2. 배포용 S3 버킷 생성
3. EC2와 IAM 역할 생성
4. EC2 환경변수 및 systemd 설치
5. 프런트 S3와 CloudFront 생성
6. GitHub OIDC 역할 생성
7. GitHub Environment 변수 등록
8. GitHub Actions에서 `Deploy backend` 수동 실행
9. `https://api.../actuator/health` 확인
10. GitHub Actions에서 `Deploy frontend` 수동 실행
11. 로그인, 쿠키, CORS, OAuth 콜백 확인
12. `JPA_DDL_AUTO=validate`로 변경 후 백엔드 재시작

## 9. Lambda 2단계

UI와 기본 배포가 안정된 뒤 Lambda를 추가합니다.

권장 기능은 **식약처 약품정보 캐시 워머**입니다.

```text
EventBridge(매일 새벽)
  → Lambda(Java 21 또는 Python 3.12)
  → 식약처 공개 API
  → private S3 drug-cache/*.json
  → Spring Boot에서 허가품목 확인 후 상세정보 캐시 우선 조회
```

이 방식은 환자정보를 Lambda로 전송하지 않고도 다음 내용을 포트폴리오에서 설명할 수 있습니다.

- 이벤트 기반 서버리스 처리
- 외부 공공 API 장애 격리
- S3 캐싱과 TTL
- CloudWatch 로그/알람
- 최소 권한 IAM

현재 구현은 품목번호 1개를 테스트 이벤트로 조회·저장하는 단계입니다.
Lambda 소스는 `deploy/lambda/drug-cache/lambda_function.py`입니다.
환자정보나 API 인증키를 이벤트 JSON에 넣지 않습니다.

### Lambda 기본 설정

- Python 3.12, 메모리 256 MB, 제한 시간 120초, VPC 미연결
- 환경변수 `CACHE_BUCKET`: 비공개 배포 버킷 이름
- 환경변수 `CACHE_PREFIX`: `drug-cache/`
- 환경변수 `DRUG_API_SERVICE_KEY`: 공공데이터포털 Decoding 인증키 (Git에 저장 금지)
- 실행 역할: 기본 로그 권한 + 해당 버킷 `drug-cache/*`에만 `s3:PutObject`
- 테스트 이벤트: `{"item_seq":"202602327"}` (저장 검증용, 처방 매핑용 아님)
- `CACHED`와 `stored: true` 확인 후 S3 객체가 생성되었는지 확인

### EC2 백엔드 연결

EC2 역할 `mediflow-ec2-role`에 아래 인라인 정책을 추가합니다.
Lambda 실행 역할의 쓰기 정책은 그대로 유지합니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::mediflow-deploy-hjk3277-apne2-157325288484-ap-northeast-2-an/drug-cache/*"
  }]
}
```

EC2 `/etc/mediflow/mediflow.env`에서 다음 값을 설정하고 서비스를 재시작합니다.

```dotenv
DRUG_CACHE_ENABLED=true
DRUG_CACHE_BUCKET=mediflow-deploy-hjk3277-apne2-157325288484-ap-northeast-2-an
DRUG_CACHE_REGION=ap-northeast-2
DRUG_CACHE_MAX_AGE_HOURS=48
```

캐시는 기본적으로 비활성화되어 있습니다. EC2 인스턴스 역할로만 인증하므로
AWS 액세스 키를 환경변수에 추가하지 않습니다. 백엔드는 허가품목 검색으로
유일한 품목번호를 먼저 확인하고 상세조회에서만 캐시를 사용합니다.
품목번호·제품명·정상 허가 상태·스키마·출처·조회시간을 검증하며,
48시간 초과/미래 날짜/손상된 객체/권한 오류/캐시 미존재는 공공 API 상세조회로 돌아갑니다.
따라서 현재는 상세 API 호출을 줄이는 캐시이며, 검색 API까지 장애가 난 경우의
완전한 오프라인 대체 기능은 아닙니다. 캐시 적중 로그는
`Drug permit cache hit - itemSeq: ...`입니다.

로그인한 사용자는 `/api/medications/drugs/{itemSeq}/detail`로 환자 처방과 독립적인
품목번호 상세 조회를 할 수 있습니다. 캐시 사용 시 응답 `data.status`는
`FOUND_PERMIT_CACHE`입니다. 미로그인 요청은 허용하지 않습니다.
예: `https://api.mediflow-emr.cloud/api/medications/drugs/202602327/detail`.
이는 카탈로그 조회일 뿐 해당 약을 환자에게 처방하거나 추천하는 기능이 아닙니다.

EventBridge 예약과 Lambda 자동 배포는 아직 연결하지 않았습니다.
사용할 실제 제품의 품목번호 목록을 확정하고 캐시 적중을 확인한 뒤 추가합니다.

### 데모 처방 카탈로그 검증 (2026-10-11)

`backend/src/main/resources/demo-drug-catalog.json`에 데모 코드 → 실제 허가 제품명 →
품목번호 → 제품 함량 → 경로를 명시합니다. `demo`/`dev` 프로파일에서만 적용하며,
사용자가 같은 코드로 이름을 바꾼 처방에는 자동 적용하지 않습니다.
정상 허가 상태와 상세 허가문서(효능·용법·주의사항)는 식약처 허가 API를 직접 호출하여
15개 품목 모두 확인했습니다. 원료 함량과 성상도 확인했습니다.
공식 데이터셋: https://www.data.go.kr/data/15095677/openapi.do

| 데모 성분 | 검증한 제품 | 품목번호 |
|---|---|---|
| 아스피린 | 아스피린프로텍트정100밀리그람 | 200108429 |
| 아토르바스타틴 | 리피토정40밀리그램 | 200410086 |
| 세프트리악손 | 대웅곰세핀주2.0그램 | 199600565 |
| 아세트아미노펜 | 타이레놀정500밀리그람 | 202106092 |
| 메트포르민 | 글루코파지정500밀리그램 | 200401015 |
| 인슐린글라진 | 란투스주솔로스타 | 200410194 |
| 암로디핀 | 노바스크정5밀리그람 | 200610660 |
| 로사르탄 | 코자정 | 200811814 |
| 세레콕시브 | 쎄레브렉스캡슐100밀리그램 | 200603815 |
| 메트로니다졸 | 트리젤주 | 198400094 |
| 판토프라졸 | 판토록정40밀리그램 | 201006491 |
| 살부타몰 | 벤토린네뷸2.5mg | 199907319 |
| 부데소니드 | 풀미칸분무용현탁액 | 200100450 |
| 푸로세미드 | 라식스정 | 196400037 |
| 탄산칼슘 | 파마탄산칼슘정 | 199801435 |

제품 함량과 기존 처방 투여량은 별도입니다. 예를 들어 란투스의 제품 농도는
100unit/mL, 시연 처방은 10unit이며, 라식스정의 제품 함량은 40mg/정,
시연 처방은 20mg입니다. 위 15종은 제품정보 연결을 위해 기존 용량·경로·빈도를 변경하지 않습니다.
해당 데모를 임상 처방의 적절성 검증이나 실제 투약에 사용해서는 안 됩니다.

2026.10.11 사용자 요청으로 미확정 2종은 **가상 시연 처방 자체**를 교체했습니다.
임상적으로 서로 동등한 약이라는 의미가 아니며 실제 환자에게 적용할 대체 규칙이 아닙니다.

- `CARD-NTG` 니트로글리세린 0.4mg/필요시 → `CARD-CLOP` 플라빅스정75밀리그램
  (클로피도그렐), 품목번호 `200700907`, 시연 용량 75mg/경구/1일 1회.
- `GI-SUC`/`GI-SUCR` 수크랄페이트 → `GI-ALM` 알마겔정(알마게이트),
  품목번호 `198700430`, 제품 함량 500mg/정, 시연 용량 1g/경구/1일 3회.
  식전 지시는 식후 30분~1시간에 씹어서 복용으로 변경.

두 제품의 정상 허가 상태·함량·효능·용법·주의사항을 공식 API로 확인했습니다.
총 17종이 품목번호로 조회됩니다. 시작 시 정확히 이전 시드 이름/코드/용량/경로/빈도와
일치하는 가상 데이터만 멱등 보정합니다. 직접 변경한 데이터, 실제 투약과 연결된 수행 기록은
보존합니다. `prod` 단독 프로파일에는 적용하지 않습니다.

데모 환자는 사용자마다 복제하지 않는 공통 데이터입니다. 신규 사용자에게 부서와 근무조별
담당 환자를 배정하므로 `내 담당`은 계정·근무조마다 다르고 겹칠 수 있습니다.
`전체 환자`는 로그인 사용자의 부서에 입원한 환자를 표시합니다.

약품명 추측 검색 대신 매핑된 품목번호로 상세 API/캐시를 조회합니다.
데모 제품은 허가정보, 캐시 적중 제품은 허가정보(S3 캐시), 이름 검색 실패는
e약은요·허가정보 조회 경로를 표시합니다. API 오류는 `TEMPORARY_ERROR`로 구분합니다.

## 10. 배포 완료 확인

- 프런트 새로고침과 `/app` 직접 접근이 모두 성공
- API health가 `UP`
- 로그인/로그아웃 및 쿠키 갱신 성공
- 환자 목록, V/S, 오더, 약품정보 조회 성공
- AI 초안 생성 성공
- RDS는 외부에서 직접 접근 불가
- S3 객체는 직접 URL 접근 불가, CloudFront로만 접근
- GitHub 저장소와 로그에 비밀값이 없음
- CloudWatch와 AWS Budget 알림 활성화

## DB 마이그레이션 배포 확인

- 배포 전 RDS 백업·복구 상태와 연결 대상 DB, 스키마 권한을 확인합니다.
- 기존 DB 최초 도입에만 FLYWAY_BASELINE_ON_MIGRATE=true로 20261010 기준선을 등록합니다. 이후 false로 유지합니다.
- 새 JAR의 prod 시작 시 Flyway가 버전별 SQL을 실행하고 Hibernate validate 후 건강 상태를 확인합니다.
- flyway_schema_history의 성공 기록 및 기존 인수인계 보존을 확인합니다. 체크섬 불일치·실패는 서버 기동을 막습니다.
- 앱 롤백은 DB 변경을 되돌리지 않습니다. [마이그레이션 절차](../database/README.md)를 따릅니다.

## 열린 화면의 배포 호환성

프런트엔드 배포는 해시가 붙은 assets를 먼저 업로드하고 기존 번들을 보존한 뒤 index.html을 마지막에 교체합니다. --delete로 이전 파일을 제거하지 않으므로 배포 전에 연 화면도 지연 로딩된 페이지로 이동할 수 있습니다. 과거 번들을 다시 제공해야 할 때는 Restore frontend assets 워크플로에 검증된 릴리스 커밋을 입력합니다. 이 작업은 해시 JS/CSS/SVG만 복원하며 현재 index.html과 소개 GIF는 교체하지 않습니다. 오래된 번들의 정리는 활성 세션 및 보존 기간을 고려한 별도 작업입니다.

## 인증 메일 장애 확인

`Inspect production` 워크플로는 SMTP 설정 존재 여부, 메일 예외 종류·SMTP 상태 코드의 발생 수와 TCP 연결 결과만 출력합니다. 인증 정보·코드·수신 주소는 출력하지 않습니다. 중복 조회는 SMTP 연결 없이 동작하고 인증 메일은 명시적 POST 요청에서만 발송합니다.

SMTP 535는 인증 거절입니다. 운영 계정의 SMTP 허용 설정과 인증 정보를 확인하고 EC2 `/etc/mediflow/mediflow.env`의 `MAIL_USERNAME`, `MAIL_PASSWORD`를 보안 세션에서 갱신한 뒤 `sudo systemctl restart mediflow`를 실행합니다. 비밀번호를 커밋·이슈·워크플로 입력값·채팅에 남기지 않습니다. 코드 재배포는 이 파일의 비밀값을 자동 변경하지 않습니다.
