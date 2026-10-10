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
| 프런트 도메인 | `mediflow-emr.cloud` |
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

현재 로컬 작업 폴더는 원격 저장소가 연결되지 않은 상태입니다. 기존 `MediFlow-EMR/mediflow-dev`를 유지하는 것을 권장합니다.

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

최초 실행에서만 `JPA_DDL_AUTO=update`를 사용해 스키마를 만듭니다. 정상 기동 후에는 `/etc/mediflow/mediflow.env`를 `JPA_DDL_AUTO=validate`로 바꾸고 서비스를 재시작합니다. 다음 개선 단계에서는 Flyway 마이그레이션으로 교체합니다.

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
9. Route 53에서 루트 도메인을 CloudFront에 Alias로 연결합니다.

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
  → Spring Boot 조회 실패 시 캐시 fallback
```

이 방식은 환자정보를 Lambda로 전송하지 않고도 다음 내용을 포트폴리오에서 설명할 수 있습니다.

- 이벤트 기반 서버리스 처리
- 외부 공공 API 장애 격리
- S3 캐싱과 TTL
- CloudWatch 로그/알람
- 최소 권한 IAM

기본 배포 성공 후 별도 브랜치에서 추가합니다.

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
