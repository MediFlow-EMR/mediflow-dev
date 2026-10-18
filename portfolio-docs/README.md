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

> 간호사의 실무 경험을 기반으로, 기존 EMR/OCS 위에서 입원환자 상태 확인과 근무조 인수인계를 지원하는 포트폴리오용 PoC

**MediFlow**는 병원 전체 업무를 대체하는 완제품 EMR이 아닙니다. 담당 환자의 오더·바이탈·I/O·투약·검사·간호기록을 모아 보여주고, 근무조 기록으로부터 AI 인수인계 초안을 생성하는 병동 간호 모듈입니다. 모든 데이터는 가상이며 전자의무기록시스템 인증을 취득한 제품이 아닙니다.

### 현재 검증 중인 핵심 가설

- 분산된 임상정보를 환자 안전정보와 함께 배치하면 상태 파악 시간을 줄일 수 있는가
- 근무조 기록을 구조화하면 누락이 적은 인수인계 초안을 만들 수 있는가
- 외부 AI에 환자 식별정보를 보내지 않고도 유용한 초안을 생성할 수 있는가

![Google Gemini](https://img.shields.io/badge/Google_Gemini-AI_Model-8E75B2?logo=google&logoColor=white)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.2.3-61DAFB?logo=react&logoColor=black)](https://react.dev)
[![MariaDB](https://img.shields.io/badge/MariaDB-10.11-003545?logo=mariadb&logoColor=white)](https://mariadb.org)
[![AWS](https://img.shields.io/badge/AWS-Cloud-FF9900?logo=amazonaws&logoColor=white)](https://aws.amazon.com)

---

## 프로젝트 개요

### 프로젝트 목표 구조
![프로젝트 개요](assets/project-overview.png)

### 핵심 문제 인식

간호사로 근무하며 경험한 EMR 시스템의 핵심 문제점:

#### 1. 분산된 정보 구조로 인한 시간 낭비
- 환자 한 명의 상태 파악을 위해 **최소 5개 이상의 페이지**를 이동해야 함
- 신규 간호사는 정보 위치를 몰라 **환자 상태 파악에 과도한 시간 소요**
- 응급 상황에서 빠른 의사결정에 치명적인 장애

#### 2. 비효율적인 인수인계 프로세스
- 대면 인수인계로 인한 **시간 소모** 및 **정보 누락 위험**
- 간호사가 당일 근무 내용을 수기로 정리하여 구두 전달
- 표준화되지 않은 인수인계 방식으로 일관성 부족

### 기술적 해결 방안

| 문제 영역 | 기존 방식 | MediFlow 솔루션 | 기술적 구현 |
|---------|----------|----------------|-----------|
| **정보 조회** | 다중 페이지 이동 (5+ 클릭) | One-View 통합 대시보드 | React 컴포넌트 모듈화 + 실시간 데이터 aggregation |
| **인수인계** | 수기 작성 + 구두 전달 | 수기·AI 초안 임시저장·수정·승인 | Gemini API 활용한 의료 기록 요약 파이프라인 |
| **신규 간호사 적응** | 별도 교육 필요 | 직관적 UI/UX | 간호 업무 플로우 기반 정보 아키텍처 설계 |

### 프로젝트 정보

- **개발 인원**: 1인 (Full-stack)
- **현재 단계**: 병동 간호 업무 흐름을 검증하는 PoC
- **배포 URL**:
  - Frontend: `https://www.mediflow-emr.cloud`
  - Backend API: `https://api.mediflow-emr.cloud`

---

## 핵심 기능

### 1. 통합 환자 뷰 - 바이탈 사인 & I/O 관리

환자 정보를 한 화면에서 확인하고 바이탈 사인 및 섭취배설량을 간편하게 입력

![바이탈 사인 & I/O](assets/vitalsign-io.png)

**기존 EMR 방식:**
```
환자 클릭 → 어디에 뭐가 있는지 모름 → 여러 메뉴 클릭하며 탐색
→ 활력징후 찾음 → I/O 어디있지? → pacs? → 간호기록은?
```

**MediFlow 방식:**
```
환자 이름 클릭 → 활력징후·I/O·검사결과·오더조회·투약·간호기록 탭이 한 화면에 정렬
→ 원하는 탭만 클릭 (1번 클릭으로 즉시 접근)
```

### 2. 의료 오더 조회 & 식약처 API 약품 정보

의사 처방 오더를 조회하고, 식약처 API를 통해 약품 정보를 실시간으로 확인

![의료 오더](assets/order.png)

### 3. 리치 텍스트 에디터 간호기록

형광펜, 글자색, 서식을 활용한 직관적인 간호기록 작성

![간호기록](assets/nursingnote.png)

### 4. AI 기반 인수인계 자동 요약

Google Gemini AI를 활용하여 선택한 근무조의 간호·임상 기록으로 검토 가능한 인수인계 초안을 생성

![AI 인수인계](assets/ai-handoff.png)

**AI 요약 결과 예시:**
```markdown
## 응급실 DAY → EVENING 인수인계

### 홍길동 (P2024001, 45세/남)
- **주요 변화**: 혈압 145/95로 정상 범위 초과, 지속 모니터링 필요
- **수행한 처치**: 타이레놀 500mg PO 2회, 항생제 1g IV 투여 완료
- **지속 관찰 사항**:
  - 혈압 경향성 체크
  - WBC 12000으로 감염 의심, 추가 검사 결과 확인 필요
- **특이사항**: 환자 상태 양호, 통증 호소 없음
```

---

## 시스템 아키텍처

### 전체 시스템 아키텍처 (Runtime)

![시스템 아키텍처](assets/Runtime-arcitecture.png)

위 그림은 ALB를 포함한 초기 설계도입니다. 현재 배포 코드 기준 구성은 다음과 같습니다:

- **프론트엔드**: 비공개 S3 + CloudFront CDN으로 정적 화면 제공
- **백엔드**: EC2의 Nginx → Spring Boot (9005), systemd 서비스
- **데이터베이스**: 비공개 RDS MariaDB, 배포 시 Flyway → validate

### 도메인 분리 전략

![도메인 아키텍처](assets/domain.png)

| 항목 | 프론트엔드 | 백엔드 |
|-----|-----------|--------|
| **도메인** | `www.mediflow-emr.cloud` | `api.mediflow-emr.cloud` |
| **인프라** | CloudFront + S3 | Nginx + EC2 |
| **인증서 리전** | **us-east-1** (CloudFront 요구사항) | EC2 Nginx TLS 설정 |
| **배포 방식** | GitHub Actions → S3 Sync | GitHub Actions OIDC → S3 → SSM → EC2 |

### 네트워크 및 보안 아키텍처

![보안 아키텍처](assets/security.png)

위 보안 그림은 초기 ALB 설계이며, 현재 배포 가이드의 **보안 그룹 규칙**은 다음과 같습니다:

| 보안 그룹 | 인바운드 규칙 | 소스 | 목적 |
|---------|------------|------|------|
| **mediflow-ec2-sg** | HTTPS (443), HTTP (80) | 0.0.0.0/0 | Nginx HTTPS 및 리다이렉션 |
| **mediflow-ec2-sg** | 9005 외부 비공개 | EC2 내부 | Nginx → Spring Boot |
| **mediflow-rds-sg** | MariaDB (3306) | mediflow-ec2-sg | EC2 → RDS 연결 |


### CI/CD 파이프라인

![CI/CD](assets/cicd.png)

위 그림은 초기 배포 개념도입니다. 현재 프론트엔드는 빌드·S3 업로드·CloudFront 무효화, 백엔드는 테스트·JAR 빌드·OIDC·S3·SSM·systemd 재시작·헬스 체크로 배포합니다. 완료 시간은 실행 결과에 따라 달라집니다. [현재 아키텍처](4.ARCHITECTURE.md)와 [배포 가이드](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/deploy/aws/README.md)를 참고하세요.

---

## 데이터베이스 설계

### ERD (Entity Relationship Diagram)

![ERD](assets/erd.png)

### 테이블 구조

![테이블 분류](assets/table.png)

### 인덱스 설계 전략

![인덱스 설계](assets/index.png)

**복합 인덱스 순서 원칙:**
```sql
-- 올바른 예시: 등호 조건 앞, 범위 조건 뒤
CREATE INDEX idx_vital_good ON vital_sign(
    patient_id,           -- WHERE patient_id = ?
    measured_at DESC      -- ORDER BY measured_at DESC
);
```

### 데이터 무결성 보장

![데이터 무결성](assets/data%20integrity.png)

### 정규화 수준

![정규화](assets/normalization.png)

---

## 기술 스택

### Frontend

| 기술 | 버전 | 용도 |
|-----|------|------|
| ![React](https://img.shields.io/badge/React-19.2.3-61DAFB?logo=react&logoColor=black) | 19.2.3 | UI 컴포넌트 라이브러리 |
| ![Vite](https://img.shields.io/badge/Vite-7.3.7-646CFF?logo=vite&logoColor=white) | 7.3.7 | 빌드 도구 (HMR 지원) |
| ![Zustand](https://img.shields.io/badge/Zustand-5.0.8-443E38) | 5.0.8 | 경량 상태 관리 |
| ![Axios](https://img.shields.io/badge/Axios-1.20.0-5A29E4?logo=axios&logoColor=white) | 1.20.0 | HTTP 클라이언트 |
| ![Recharts](https://img.shields.io/badge/Recharts-3.5.0-22B5BF) | 3.5.0 | 바이탈 사인 시계열 차트 |

### Backend

| 기술 | 버전 | 용도 |
|-----|------|------|
| ![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white) | 21 | 프로그래밍 언어 |
| ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?logo=springboot&logoColor=white) | 3.5.6 | 백엔드 프레임워크 |
| ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white) | - | 인증/인가 (JWT + OAuth2) |
| ![QueryDSL](https://img.shields.io/badge/QueryDSL-5.0.0-0078D4) | 5.0.0 | 타입 안전 동적 쿼리 |
| ![JWT](https://img.shields.io/badge/JWT-0.12.3-000000?logo=jsonwebtokens&logoColor=white) | 0.12.3 | 토큰 기반 인증 |

### Database

| 기술 | 버전 | 용도 |
|-----|------|------|
| ![MariaDB](https://img.shields.io/badge/MariaDB-10.11-003545?logo=mariadb&logoColor=white) | 10.11 | 운영 데이터베이스 |
| ![H2](https://img.shields.io/badge/H2_Database-In_Memory-003545) | - | 격리된 자동 테스트용 인메모리 DB (로컬은 MariaDB) |

### External APIs

| 기술 | 용도 |
|-----|------|
| ![Google Gemini](https://img.shields.io/badge/Google_Gemini-AI_Model-8E75B2?logo=google&logoColor=white) | AI 인수인계 요약 생성 |
| ![KFDA API](https://img.shields.io/badge/공공데이터포털-식약처_API-113366) | 약품 정보 실시간 검색 |
| ![Google OAuth2](https://img.shields.io/badge/Google_OAuth2-Login-4285F4?logo=google&logoColor=white) | 소셜 로그인 (구글) |
| ![Kakao OAuth2](https://img.shields.io/badge/Kakao_OAuth2-Login-FFCD00?logo=kakao&logoColor=black) | 소셜 로그인 (카카오) |

### Infrastructure

| 기술 | 용도 |
|-----|------|
| ![AWS EC2](https://img.shields.io/badge/AWS_EC2-Backend-FF9900?logo=amazonec2&logoColor=white) | 백엔드 서버 호스팅 |
| ![AWS RDS](https://img.shields.io/badge/AWS_RDS-MariaDB-527FFF?logo=amazonrds&logoColor=white) | MariaDB 관리형 데이터베이스 |
| ![AWS S3](https://img.shields.io/badge/AWS_S3-Storage-569A31?logo=amazons3&logoColor=white) | 정적 파일 저장 |
| ![AWS CloudFront](https://img.shields.io/badge/AWS_CloudFront-CDN-D05C42?logo=amazoncloudfront&logoColor=white) | 프론트엔드 CDN |
| ![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-CI%2FCD-2088FF?logo=githubactions&logoColor=white) | CI/CD 파이프라인 |

---

## API 문서

### API 도메인 구조

![API 도메인](assets/api-domain.png)

### 주요 엔드포인트

| 도메인 | 엔드포인트 | 설명 |
|--------|-----------|------|
| **Dashboard** | `GET /api/dashboard/my-patients` | 내 담당 환자 목록 조회 |
| **Vital Signs** | `POST /api/vitals` | 바이탈 사인 등록 |
| **Nursing Notes** | `POST /api/nursing-notes` | 간호기록 등록 |
| **Medications** | `GET /api/medications/drugs/search` | 약품 검색 (식약처 API) |
| **Handovers** | `POST /api/handovers/ai-summary` | AI 인수인계 요약 생성 |

### API 문서 확인 방법

1. **[Swagger Editor](https://editor.swagger.io)** 접속
2. 왼쪽 편집기에 **[swagger.yaml](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/swagger.yaml)** 파일 내용 복사 & 붙여넣기
3. 오른쪽에서 실시간으로 API 문서 확인 및 테스트

---

## 보안 및 인증

### JWT 토큰 구조

![인증 구조](assets/Authentication.png)

**이중 토큰 전략:**
- **Access Token**: 15분 (짧은 유효기간으로 보안 강화)
- **Refresh Token**: 7일 (자동 갱신으로 사용자 편의성 확보)

### 3가지 인증 방식

#### 1. 로컬 이메일 인증
![로컬 인증](assets/Local%20Email%20Authentication.png)

#### 2. Google OAuth2
![Google OAuth2](assets/Google%20OAuth2.png)

#### 3. Kakao OAuth2
![Kakao OAuth2](assets/Kakao%20OAuth2.png)

### 주요 보안 특징

- **HttpOnly 쿠키**: 스크립트의 인증 쿠키 접근 제한
- **SameSite 정책**: 환경별 쿠키 정책 적용 (운영은 Secure/None, 이것만으로 모든 CSRF를 방지하지 않음)
- **BCrypt 암호화**: 비밀번호 안전 저장
- **State 파라미터**: OAuth2 요청 위조 방지
- **providerId 분리**: 사용자 데이터 충돌 방지

---

## 배포 아키텍처

### RDS 구성

![현재 DB 구조](assets/db.png)

이미지는 업무 엔티티 18개의 관계이며, FK가 없는 데모 관리 테이블 `demo_refresh_state`는 별도입니다. 아래 값은 초기 배포 설계 선택 기록이며 실제 운영 용량·비용·백업 설정을 재확인한 수치가 아닙니다. 현재 운영 절차는 [배포 가이드](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/deploy/aws/README.md)를 따릅니다.

| 항목 | 값 | 선택 이유 |
|-----|---|---------|
| **인스턴스 클래스** | db.t4g.micro | t4g가 t3 대비 20% 저렴, ARM 기반 성능 우수 |
| **스토리지** | 20GB gp3 | gp2 대비 20% 저렴, IOPS 3000 기본 제공 |
| **엔진** | MariaDB 10.11 | MySQL 호환, 오픈소스, Spring Boot JPA 최적화 |
| **타임존** | Asia/Seoul | KST 시간대 자동 처리 |
| **백업 보존** | 7일 | 주간 단위 롤백 가능 |

---

## 법적 고려사항

### 관련 법령 및 규제 체계

![법적 고려사항](assets/legal.png)

### 현재 단계 (포트폴리오 PoC)

![PoC 단계](assets/8.PoC.png)

**명확한 범위 설정:**
- 사용 데이터: 가상의 환자명 (홍길동, 김영희), 임의의 차트번호, 시연용 더미 데이터
- 미사용 데이터: 실제 병원 환자 정보, 실제 주민등록번호, 실제 의료 기록
- 목적: 기술 구현 능력 및 시스템 아키텍처 시연

### 실제 서비스 전환 시 고려사항

아래 그림은 향후 검토를 위한 기존 설계 자료이며 현재 제품의 인증·법적 적합성을 의미하지 않습니다. 현재 구현의 한계는 [적용 범위](8.LEGAL_COMPLIANCE.md)에 정리했습니다.

#### 개인정보보호법 준수

![개인정보 보호](assets/8.observe.png)

#### AI 서버 운영 전략

**Option 1: 병원 내부 On-Premise AI 서버 (권장)**
![On-Premise AI](assets/On-Premise%20AI.png)

**Option 2: AWS Bedrock (서울 리전)**
![AWS Bedrock](assets/AWS%20Bedrock.png)

**Option 3: Open Source LLM 자체 호스팅**
![Open Source LLM](assets/Open%20Source%20LLM%20.png)

#### 권장 전환 전략

![전환 전략](assets/recommend.png)

### 의료기기법 적용 여부

![의료기기 해당 여부](assets/feat-device.png)

### AI 책임 소재

![법적 책임](assets/8.legal%20liability.png)

---

## 향후 계획

### 3단계 확장 로드맵

![비전](assets/vision.png)

### Phase 1. 간호사 현장 업무 효율화

![Phase 1](assets/phase1.png)

### Phase 2. AI 기반 관리 자동화 및 CDS 확장

![Phase 2](assets/phase2.png)


#### AI CDS 알고리즘 예시

![AI CDS](assets/aicds.png)

### Phase 3. 표준화 및 외부 시스템 통합

![Phase 3](assets/phase3.png)

#### HL7/FHIR 통합 아키텍처

![HL7/FHIR](assets/HL7FHIR.png)

---

## 참고 문서

- [1. 프로젝트 개요](1.PROJECT_OVERVIEW.md)
- [2. 경쟁사 분석](2.COMPETITOR_ANALYSIS.md)
- [3. 기능 명세서](3.FEATURES.md)
- [4. 아키텍처 설계](4.ARCHITECTURE.md)
- [5. 데이터베이스 설계](5.DATABASE_DESIGN.md)
- [6. API 문서](6.API_DOCUMENTATION.md)
- [7. 향후 업데이트 계획](7.ROADMAP.md)
- [8. 법적 고려사항](8.LEGAL_COMPLIANCE.md)

---


최종 UI·정렬·저장 안정성 변경과 재현 가능한 검증 결과는 [품질 검증 기록](10.QUALITY_AND_VALIDATION.md)에 정리했습니다.

## 현재 UI 시연

2026.10.11 배포 사이트의 가상 계정으로 촬영한 최신 PC 화면입니다. 실제 촬영 범위·프레임 수·파일 검증값은 [캡처 정보](assets/demos/manifest.json), 초안 저장·재조회·승인·수정의 검증 결과는 [품질 검증 기록](10.QUALITY_AND_VALIDATION.md)을 참고하세요.

![이메일 로그인과 데모 체험](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/login.gif)

![PC 가로형 대시보드·날짜·근무조·병실 다중 필터](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/patientdashboard.gif)

![간호기록 카테고리·작성자 다중 필터와 기간 달력](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/nursingrecord.gif)

![오더 날짜 정렬과 약품 정보 조회](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/ordertab.gif)

![수기 초안 저장·승인·수정 이력과 실제 AI 초안 편집](https://raw.githubusercontent.com/MediFlow-EMR/mediflow-dev/644421f36b15e6dc6bb47614c52f20612ee7e661/frontend/public/assets/aihandover.gif)
