# MediFlow — 병동 간호·인수인계 EMR 연계 모듈

## 현재 구현 상태 (2026-10-18)

- 랜딩·로그인·메인·상세 화면은 라이트 기본값의 저장형 다크모드와 모바일 배치를 제공합니다. 공통 포커스·움직임과 reduced-motion을 적용했습니다.
- 환자 검색·병실·안전정보, 간호기록 내용·기간·카테고리·작성자·중요 여부, 인수인계 내용/작성자·기간·근무조·수기/AI·상태를 필터링합니다. 다중 선택은 같은 항목 안에서 OR, 항목 사이에서 AND이며 조회된 목록에 적용합니다.
- 간호기록·인수인계·오더·투약·V/S·섭취배설·검사결과는 최신순/오래된순을 선택합니다. 환자 목록은 KTAS 우선순위를 유지합니다.
- 상단 날짜·근무조로 과거 담당 환자와 부서 요약, 새 인수인계의 대상 근무조를 전환합니다. 환자 기본정보·최근 V/S는 현재 값이며 임상 탭의 기간 필터는 별도입니다.
- 수기·AI 초안을 서버에 임시저장하고 다시 열어 수정·승인합니다. 승인 후 작성자 수정, 원본 AI·수정본·버전별 스냅샷 보존을 지원합니다. 초안은 작성자 전용, 승인본·이력은 같은 부서 조회입니다.
- 공개 데모 계정은 한국 시간 기준으로 오늘·내일의 환자 배정을 자동 보충하고, 시작한 근무조에 시연용 처방·투약·V/S·섭취배설·간호기록·검사 예시를 추가합니다. 기존 기록·작성 내용은 유지하며 같은 날짜·근무조는 중복 생성하지 않습니다.
- 운영은 Flyway → Hibernate validate → 헬스 체크 순서입니다. 기존 DB의 최초 기준선·백업·실패 대응은 [DB 전환 안내](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/deploy/database/README.md)를 따릅니다. 로컬 MariaDB 보존은 `local` 프로필, 격리된 자동 테스트는 H2입니다.
- 최신 API는 [swagger.yaml](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/swagger.yaml) 1.3.2, DB는 업무 엔티티 18개와 데모 갱신 관리 1개로 총 19개 JPA 엔티티입니다. 소개 GIF는 2026.10.11 운영 화면 기준입니다.

> 간호사의 실무 경험에서 출발해 입원환자 상태 확인, 간호 수행 기록, 근무조 인수인계를 한 흐름으로 연결한 포트폴리오용 PoC

AWS 배포 시에는 [AWS 배포 가이드](deploy/aws/README.md)와 [배포 체크리스트](deploy/aws/CHECKLIST.md)를 먼저 확인하세요.

MediFlow는 병원 전체 업무를 대체하는 완제품 EMR이 아닙니다. 기존 EMR/OCS의 환자·오더·검사 데이터를 연계해 간호사의 병동 업무와 인수인계를 지원하는 신규 모듈을 가정해 설계했습니다. 모든 환자 데이터는 가상 데이터이며, 본 프로젝트는 전자의무기록시스템 인증을 취득한 제품이 아닙니다.

## 해결하려는 업무 흐름

```text
담당 환자 확인
  → 환자 식별·알레르기·최근 활력징후 확인
  → 오더 수행·투약·간호기록·I/O 입력
  → 이름·차트번호 대신 요청 내 가명 식별자로 근무조 기록을 AI에 전달
  → 간호사가 검토·수정한 뒤 인수인계 저장
```

### 구현 범위

- 담당/부서 환자 목록과 임상 안전정보 표시
- 오더 조회 및 수행 상태 관리
- 바이탈, 섭취·배설량, 투약, 검사결과, 간호기록
- AI 인수인계 초안 생성과 사용자 검토
- JWT/OAuth2 인증, 낙관적 잠금, 환경별 설정

### 명시적 한계

- 접수·입퇴원·원무·EDI 청구·PACS·약제 등 병원 전체 EMR 기능은 범위 밖입니다.
- AI 결과는 진료 판단이 아닌 인수인계 초안이며 의료인의 검토를 대체하지 않습니다.
- 자유 입력 간호기록까지 자동으로 개인정보를 제거하는 기능은 없습니다. 실제 개인정보를 입력하지 마세요.
- 실제 의료기관 적용에는 권한·감사·전자서명·표준용어·연계·인증에 대한 추가 검증이 필요합니다.

## 중요 주의사항

### 보안
- `.env` 파일은 **절대 Git에 커밋하지 마세요**
- API 키, DB 비밀번호 등 민감 정보는 `.env.example`에 실제 값을 넣지 마세요
- GitHub에 업로드하기 전 `.gitignore`에 `.env`가 포함되어 있는지 확인하세요

### 데이터
- 이 프로젝트는 **포트폴리오 목적**으로, 모든 환자 데이터는 **가상**입니다
- 실제 병원 환자 정보를 절대 입력하지 마세요
- 개인정보보호법 위반 위험이 있습니다

### 배포
- 프로덕션 환경에서는 `application-prod.yml` 사용
- AWS 배포 시 `.env` 파일이 아닌 **EC2 환경 변수** 또는 **AWS Secrets Manager** 사용 권장

---

## 목차

- [개발 환경 설정](#개발-환경-설정)
- [프로젝트 구조](#프로젝트-구조)
- [로컬 실행 방법](#로컬-실행-방법)
- [테스트 계정](#테스트-계정)
- [API 문서](#api-문서)
- [데이터베이스](#데이터베이스)
- [트러블슈팅](#트러블슈팅)

---

## 개발 환경 설정

### 필수 요구사항

| 항목 | 버전 | 설치 확인 |
|------|------|-----------|
| Java | 21 | `java -version` |
| Node.js | 22 (CI 기준) | `node -v` |
| MariaDB | 10.x | `mysql --version` |
| Gradle | 8.x | `gradle -v` |

---

## 프로젝트 구조
```
mediflow-emr/
├── backend/                    # Spring Boot 백엔드
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/mediflow/emr/
│   │   │   │   ├── config/           # 설정 (Security, JWT, OAuth2)
│   │   │   │   ├── controller/       # REST API 컨트롤러
│   │   │   │   ├── service/          # 비즈니스 로직
│   │   │   │   ├── repository/       # JPA Repository
│   │   │   │   ├── entity/           # JPA 엔티티
│   │   │   │   ├── dto/              # 데이터 전송 객체
│   │   │   │   ├── exception/        # 예외 처리
│   │   │   │   ├── init/             # 초기 데이터 생성 (DataInitializer.java)
│   │   │   │   └── util/             # 유틸리티 (JWT, Cookie)
│   │   │   └── resources/
│   │   │       ├── application.yml           # 공통 설정
│   │   │       ├── application-dev.yml       # 개발 환경
│   │   │       └── application-prod.yml      # 운영 환경
│   │   └── test/              # 테스트 코드
│   ├── .env                   # 환경 변수 (Git 제외, .gitignore에 명시)
│   ├── .env.example           # 환경 변수 템플릿
│   ├── .gitignore             # Git 제외 파일 목록
│   ├── build.gradle           # Gradle 빌드 설정
│   └── gradlew                # Gradle Wrapper
│
├── frontend/                  # React + Vite 프론트엔드
│   ├── src/
│   │   ├── components/        # React 컴포넌트
│   │   │   ├── auth/          # 인증 관련
│   │   │   ├── common/        # 공통 컴포넌트
│   │   │   ├── dashboard/     # 대시보드
│   │   │   ├── patient/       # 환자 관리
│   │   │   └── handover/      # 인수인계
│   │   ├── pages/             # 페이지 컴포넌트
│   │   ├── services/          # API 클라이언트
│   │   ├── stores/            # Zustand 상태 관리
│   │   ├── routes.jsx         # 라우팅 설정
│   │   └── main.jsx           # 엔트리 포인트
│   ├── .env                   # 환경 변수 (Git 제외, .gitignore에 명시)
│   ├── .env.example           # 환경 변수 템플릿
│   ├── .gitignore             # Git 제외 파일 목록
│   ├── package.json           # npm 의존성
│   └── vite.config.js         # Vite 설정
│
├── docs/                      # 프로젝트 문서
│   ├── PROJECT_OVERVIEW.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE_DESIGN.md
│   └── ...
│
└── swagger.yaml               # OpenAPI 3.0 문서
```

---

## 로컬 실행 방법

### 1. 저장소 클론
```bash
git clone https://github.com/MediFlow-EMR/mediflow-dev.git
cd mediflow-dev
```

### 2. 데이터베이스 설정
```bash
# MariaDB 설치 (macOS)
brew install mariadb
brew services start mariadb

# 데이터베이스 생성
mysql -u root -p
```
```sql
CREATE DATABASE mediflowdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'mediflow'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON mediflowdb.* TO 'mediflow'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. 백엔드 실행
```bash
cd backend

# .env 파일 생성 (템플릿 복사)
cp .env.example .env

# .env 파일 수정 (에디터로 열어서 실제 값 입력)
vim .env

# Gradle 빌드 및 실행
bash ./gradlew bootRun --args='--spring.profiles.active=local'

```

**실행 확인:**
```bash
curl http://localhost:9005/actuator/health
# 응답: {"status":"UP"}
```

### 4. 프론트엔드 실행
```bash
cd frontend

# 의존성 설치
npm install

# .env 파일 생성
cp .env.example .env

# 개발 서버 실행 (http://localhost:3000)
npm run dev
```

**실행 확인:**
- 브라우저에서 `http://localhost:3000` 접속

---

## 테스트 계정

배포된 데모는 [MediFlow 로그인](https://www.mediflow-emr.cloud/auth/signin) → **이메일로 로그인**에서 아래 계정으로 체험할 수 있습니다. 로그인 화면의 **데모 계정 입력** 버튼은 응급실 계정 정보를 채웁니다. 회원가입이나 이메일 인증은 필요하지 않습니다.

`dev` 또는 `demo` 프로필에서 생성되는 공용 테스트 계정입니다. 실제 개인정보는 입력하지 마세요. 계정별 담당 환자는 다르지만 부서의 가상 환자·기록은 공유되므로, 다른 사용자의 변경이 보일 수 있습니다.

**간호사 계정 (응급실):**
```
이메일: nurseer1@mediflow.com
비밀번호: nurse123@
```

**간호사 계정 (중환자실):**
```
이메일: nurseicu1@mediflow.com
비밀번호: nurse123@
```

**간호사 계정 (내과병동):**
```
이메일: nursemw1@mediflow.com
비밀번호: nurse123@
```
---

### 환경 변수 생성 방법

#### JWT Secret 생성
```bash
# 256비트 랜덤 키 생성 후 Base64 인코딩
openssl rand -base64 32
```

#### Google OAuth2 설정

1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 생성 → API 및 서비스 → OAuth 2.0 클라이언트 ID 생성
3. 승인된 리디렉션 URI: `http://localhost:9005/login/oauth2/code/google`

#### Kakao OAuth2 설정

1. [Kakao Developers](https://developers.kakao.com/) 접속
2. 애플리케이션 추가 → 플랫폼 설정 → Web 플랫폼 등록
3. Redirect URI: `http://localhost:9005/login/oauth2/code/kakao`

#### Naver 이메일 

**이메일 인증 기능 사용 시에만 필요합니다.**

1. [Naver](https://naver.com) 로그인 → 내정보 → 보안설정
2. 2단계 인증 활성화
3. 애플리케이션 비밀번호 생성
4. 생성된 비밀번호를 `MAIL_PASSWORD`에 입력

#### 식약처 API

1. [공공데이터포털](https://www.data.go.kr/) 접속
2. "의약품 정보" 검색 → 활용신청
3. 승인 후 서비스 키 발급

#### Gemini AI

1. [Google AI Studio](https://makersuite.google.com/app/apikey) 접속
2. API 키 생성

---

## API 문서

### Swagger UI

**로컬 환경:**
```
내장 Swagger UI는 현재 설치되어 있지 않습니다. 아래 Swagger Editor 또는 공식 Swagger UI 컨테이너로 swagger.yaml을 확인하세요.
```

**Swagger Editor (온라인):**
1. [https://editor.swagger.io](https://editor.swagger.io) 접속
2. `swagger.yaml` 파일 내용 붙여넣기

**상세 API 문서:** [현재 API 안내](portfolio-docs/6.API_DOCUMENTATION.md)

---

## 데이터베이스

### ERD 
![데이터베이스](assets/db.png)

**상세 ERD:** [docs/DATABASE_DESIGN.md](https://github.com/MediFlow-EMR/MediFlow-docs/blob/main/5.DATABASE_DESIGN.md)

---

### 초기 데이터

개발 환경에서는 `DataInitializer.java`가 자동으로 샘플 데이터를 생성합니다:

**생성되는 데이터:**
- 부서: 응급실, 중환자실, 내과병동, 외과병동, 내과외래, 외과외래, 수술실, 투석실 (8개)
- 근무조: 주간/초번/야간 (오늘 날짜 기준)
- 사용자: Admin 1명 + 간호사 33명
- 환자: 가상 더미 데이터 (실제 환자 정보 아님)
    - 응급실: 15명
    - 중환자실: 12명
    - 내과병동: 35명
    - 외과병동: 30명
    - 내과외래: 20명
    - 외과외래: 18명
    - 수술실: 6명
    - 투석실: 10명
- 바이탈 사인, 간호기록, 투약 기록, 섭취배설량, 의료 오더, 검사 결과, 인수인계 등

**초기화 비활성화:**
```yaml
# application-dev.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # create → validate로 변경
```

**초기화 재실행:**
```bash
# 데이터베이스 초기화 후 재시작
mysql -u mediflow -p mediflowdb -e "DROP DATABASE mediflowdb; CREATE DATABASE mediflowdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
bash ./gradlew bootRun --args='--spring.profiles.active=local'
```

---

### 데이터베이스 마이그레이션

**현재 전략:** JPA DDL Auto (개발 환경)

**권장 전략:** Flyway 도입
```gradle
// build.gradle
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```
```
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__add_indexes.sql
└── V3__add_fulltext_search.sql
```

---

### 새 API 추가 방법

**1. Entity 생성**
```java
@Entity
@Table(name = "new_table")
public class NewEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
}
```

**2. Repository 생성**
```java
public interface NewRepository extends JpaRepository<NewEntity, Long> {
    List<NewEntity> findByPatientId(Long patientId);
}
```

**3. DTO 생성**
```java
public record NewRequest(
    @NotNull Long patientId,
    @NotBlank String content
) {}
```

**4. Service 생성**
```java
@Service
@RequiredArgsConstructor
public class NewService {
    private final NewRepository repository;
    
    public NewResponse create(NewRequest request) {
        // 비즈니스 로직
    }
}
```

**5. Controller 생성**
```java
@RestController
@RequestMapping("/api/new")
@RequiredArgsConstructor
public class NewController {
    private final NewService service;
    
    @PostMapping
    public ApiResponse<NewResponse> create(@Valid @RequestBody NewRequest request) {
        return ApiResponse.ok(service.create(request));
    }
}
```

**6. Swagger 문서 업데이트**
```yaml
# swagger.yaml에 엔드포인트 추가
```
---

## 트러블슈팅

### 백엔드

**문제: Port 9005 already in use**
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :9005

# 프로세스 종료
kill -9 <PID>
```

---

**문제: JWT 토큰 검증 실패**
```bash
# JWT_SECRET이 256비트 이상인지 확인
echo -n "your-secret" | wc -c  # 32자 이상이어야 함

# 새 Secret 생성
openssl rand -base64 32
```

---

**문제: OAuth2 리다이렉트 오류**
```yaml
# application-dev.yml에서 redirect-uri 확인
redirect-uri: 'http://localhost:9005/login/oauth2/code/google'
# Google Console의 승인된 리디렉션 URI와 정확히 일치해야 함
```

---

**문제: 데이터베이스 연결 실패**
```bash
# MariaDB 실행 확인
brew services list | grep mariadb

# MariaDB 재시작
brew services restart mariadb

# 연결 테스트
mysql -h localhost -P 3306 -u mediflow -p mediflowdb
```

---

**문제: 초기 데이터가 생성되지 않음**
```yaml
# application-dev.yml 확인
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 기존 DB 보존. 스키마 변경은 Flyway로 적용
```

---

### 프론트엔드

**문제: CORS 에러**
```javascript
// vite.config.js 확인
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:9005',
      changeOrigin: true,
    },
  },
}
```

---

**문제: 쿠키가 전송되지 않음**
```javascript
// apiClient.js 확인
axios.defaults.withCredentials = true;
```
```yaml
# Backend application-dev.yml 확인
spring:
  security:
    cors:
      allowed-origins: http://localhost:3000
      allow-credentials: true
```

---

**문제: 빌드 실패**
```bash
# 잠금 파일에 맞춰 의존성 재설치 후 빌드
npm ci
npm run build
```

---

**문제: Vite 개발 서버가 느림**
```javascript
// vite.config.js에 최적화 설정 추가
export default defineConfig({
  optimizeDeps: {
    include: ['react', 'react-dom', 'axios'],
  },
});
```

---

### 공통

**문제: 환경 변수 로드 안 됨**
```bash
# .env 파일 위치 확인
ls -la backend/.env
ls -la frontend/.env

# .env 파일 권한 확인
chmod 600 backend/.env
chmod 600 frontend/.env

# 환경 변수 확인 (Backend)
cd backend
bash ./gradlew bootRun --args='--spring.profiles.active=local' --debug

# 환경 변수 확인 (Frontend)
cd frontend
npm run dev -- --debug
```

---

**문제: Git에 .env 파일이 커밋됨**
```bash
# .gitignore에 .env가 있는지 확인
cat .gitignore | grep .env

# 이미 커밋된 .env 파일 제거
git rm --cached backend/.env
git rm --cached frontend/.env
git commit -m "chore: Remove .env files from Git"

```

노출된 키·비밀번호를 먼저 폐기·재발급하세요. 과거 커밋에서도 제거해야 할 때는 협업자와 범위를 확인한 뒤 별도로 진행합니다.

---

## 추가 문서

- **[프로젝트 개요](https://github.com/MediFlow-EMR/MediFlow-docs/blob/main/1.PROJECT_OVERVIEW.md)** - 전체 시스템 소개
- **[기능명세서](https://github.com/MediFlow-EMR/MediFlow-docs/blob/main/3.FEATURES.md)** - 기능명세서
- **[아키텍처](https://github.com/MediFlow-EMR/MediFlow-docs/blob/main/4.ARCHITECTURE.md)** - AWS 배포 구조와 CI/CD
- **[법적 고려사항](https://github.com/MediFlow-EMR/MediFlow-docs/blob/main/8.LEGAL_COMPLIANCE.md)** - 의료법/개인정보보호법

---


최종 UI·정렬·저장 안정성 변경과 재현 가능한 검증 결과는 [품질 검증 기록](portfolio-docs/10.QUALITY_AND_VALIDATION.md)에 정리했습니다.

## 현재 UI 시연

2026.10.11 배포 사이트의 가상 계정으로 촬영한 최신 PC 화면입니다. 가로형 대시보드, 날짜·근무조 전환, 병실·카테고리·작성자 다중 필터, 정렬·약품 조회를 보여줍니다. 인수인계 영상은 수기 초안 임시저장·재조회·승인·작성자 수정·버전 이력과 실제 Gemini 초안 생성·편집·원본 비교·임시저장을 포함합니다. [캡처 정보 및 파일 검증값](portfolio-docs/assets/demos/manifest.json)을 확인하세요.

![이메일 로그인과 데모 체험](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/login.gif)

![PC 가로형 대시보드·날짜·근무조·병실 다중 필터](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/patientdashboard.gif)

![간호기록 카테고리·작성자 다중 필터와 기간 달력](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/nursingrecord.gif)

![오더 날짜 정렬과 약품 정보 조회](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/ordertab.gif)

![수기 초안 저장·승인·수정 이력과 실제 AI 초안 편집](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/aihandover.gif)
