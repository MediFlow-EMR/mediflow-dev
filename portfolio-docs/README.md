# MediFlow — 병동 간호·인수인계 EMR 연계 모듈

> 간호사의 실무 경험에서 출발해 환자 상태 확인, 간호 수행 기록, 근무조 인수인계를 연결한 포트폴리오용 PoC

[서비스 체험](https://www.mediflow-emr.cloud) · [개발 저장소](https://github.com/MediFlow-EMR/mediflow-dev) · [기능 명세](https://github.com/MediFlow-EMR/MediFlow-docs/blob/main/3.FEATURES.md) · [OpenAPI 1.3.2](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/swagger.yaml)

모든 환자 데이터는 가상입니다. 실제 EMR/OCS 연계, 전자서명·의료기관 인증은 구현 범위 밖이며, 전자의무기록시스템 인증을 취득한 제품이 아닙니다. 임상 효과나 시간 절감률을 실제 의료기관에서 검증하지 않았습니다.

## 현재 구현 상태 (2026-10-11)

- 랜딩·로그인·메인·상세 화면은 라이트 기본값의 저장형 다크모드와 모바일 배치를 제공합니다. 공통 포커스·움직임과 reduced-motion을 적용했습니다.
- 환자 검색·병실·안전정보, 간호기록 내용·기간·카테고리·작성자·중요 여부, 인수인계 내용/작성자·기간·근무조·수기/AI·상태를 필터링합니다. 다중 선택은 같은 항목 안에서 OR, 항목 사이에서 AND이며 조회된 목록에 적용합니다.
- 간호기록·인수인계·오더·투약·V/S·섭취배설·검사결과는 최신순/오래된순을 선택합니다. 환자 목록은 KTAS 우선순위를 유지합니다.
- 상단 날짜·근무조로 과거 담당 환자와 부서 요약, 새 인수인계의 대상 근무조를 전환합니다. 환자 기본정보·최근 V/S는 현재 값이며 임상 탭의 기간 필터는 별도입니다.
- 수기·AI 초안을 서버에 임시저장하고 다시 열어 수정·승인합니다. 승인 후 작성자 수정, 원본 AI·수정본·버전별 스냅샷 보존을 지원합니다. 초안은 작성자 전용, 승인본·이력은 같은 부서 조회입니다.
- 운영은 Flyway → Hibernate validate → 헬스 체크 순서입니다. 기존 DB의 최초 기준선·백업·실패 대응은 [DB 전환 안내](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/deploy/database/README.md)를 따릅니다. 로컬 MariaDB 보존은 `local` 프로필, 격리된 자동 테스트는 H2입니다.
- 최신 API는 [swagger.yaml](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/swagger.yaml) 1.3.2, DB는 18개 JPA 엔티티입니다. 소개 GIF는 2026.10.11 운영 화면 기준입니다.

## 저장소 안내

| 저장소 | 역할 |
|---|---|
| [mediflow-dev](https://github.com/MediFlow-EMR/mediflow-dev) | 실행 코드, 배포 스크립트, Swagger 원본, UI 시연 원본 |
| [MediFlow-docs](https://github.com/MediFlow-EMR/MediFlow-docs) | 기능·설계·검증·한계와 개발 저장소 `portfolio-docs/`의 동기화본 |
| [.github](https://github.com/MediFlow-EMR/.github/blob/main/profile/README.md) | 조직 소개와 동일한 최신 시연 GIF |

## 현재 UI 시연

2026.10.11 배포된 사이트의 가상 계정으로 캡처한 5개 GIF입니다. 각 영상은 아래에 적힌 동작을 보여줍니다. AI 영상은 해당 근무조의 기록 없음 초안 편집과 기존 승인 기록의 이력 조회를 포함하며, 실제 Gemini 응답이나 모든 저장 단계를 연속 녹화한 영상은 아닙니다. 수기 초안 저장·재조회·승인·승인 후 수정의 검증 결과는 [품질 검증 기록](https://github.com/MediFlow-EMR/MediFlow-docs/blob/main/10.QUALITY_AND_VALIDATION.md)에 따로 정리했습니다.

### 1. 이메일 로그인과 데모 체험

![이메일 로그인과 데모 체험](assets/demos/login.gif?v=84579e31d4b5)

### 2. 담당·부서 환자와 날짜·근무조 전환

![담당·부서 환자와 날짜·근무조 전환](assets/demos/patientdashboard.gif?v=13837deef289)

### 3. 간호기록 다중 카테고리 필터

![간호기록 다중 카테고리 필터](assets/demos/nursingrecord.gif?v=7972abf1ece9)

### 4. 오더 날짜 정렬과 약품 정보 조회

![오더 날짜 정렬과 약품 정보 조회](assets/demos/ordertab.gif?v=299fa3949df9)

### 5. AI 초안 편집·미저장 보호·승인 기록 이력

![AI 초안 편집·미저장 보호·승인 기록 이력](assets/demos/aihandover.gif?v=91c5d9585f22)

[캡처 정보·프레임 수·SHA-256 검증값](assets/demos/manifest.json)

## 기술과 배포

| 영역 | 현재 코드·배포 구성 |
|---|---|
| Frontend | React 19.2.3 · Vite 7.3.7 · React Router 7.18.4 · Zustand 5.0.8 · Axios 1.20.0 · Recharts 3.5.0 |
| Backend | Java 21 · Spring Boot 3.5.6 · Spring Security · JPA/QueryDSL · JWT · Spring Mail |
| Database | MariaDB · Flyway, 격리된 자동 테스트용 H2 |
| 외부 연동 | Google Gemini 인수인계 초안 · 식약처 약품 정보 · Google/Kakao OAuth2 구현 |
| Frontend 배포 | GitHub Actions → 비공개 S3 → CloudFront |
| Backend 배포 | GitHub Actions OIDC → S3 JAR → SSM → EC2/systemd, Nginx → Spring Boot(9005) → RDS MariaDB |
| 선택 확장 | Lambda → S3 약품 캐시, 캐시가 없으면 식약처 API 조회 |

프론트엔드 버전은 `package-lock.json` 설치 버전 기준입니다. 현재 배포 스크립트는 SSH 배포를 사용하지 않습니다. 세부 구성과 운영 절차는 [아키텍처](https://github.com/MediFlow-EMR/MediFlow-docs/blob/main/4.ARCHITECTURE.md)와 [AWS 배포 가이드](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/deploy/aws/README.md)를 참고하세요.

## 저장과 인증의 경계

- 부서·작성자 접근 제어와 version 기반 낙관적 잠금을 적용하며 오래된 수정 요청은 409로 거절합니다.
- 이메일 중복 확인과 SMTP 발송은 분리되어 있습니다. 4자리 코드의 유효기간은 5분, 재발송 간격은 60초, 오입력 제한은 5회입니다.
- 인증은 HttpOnly 쿠키와 JWT/OAuth2를 사용합니다. 쿠키 설정만으로 모든 XSS·CSRF 위험이 제거되는 것은 아닙니다.
- AI는 선택한 근무조 기록을 초안으로 정리하며 간호사가 검토합니다. 요청의 이름·차트번호는 가명으로 바꾸지만 자유 입력 내용의 개인정보까지 자동 제거하지 않습니다.
- 임상 기록 전체의 정정 이력·전자서명은 구현되지 않았습니다. 현재 버전별 스냅샷은 인수인계에 적용됩니다.

## 데이터베이스

![18개 JPA 엔티티 기준 DB 구조](assets/db.png)

[ERD·테이블 설명](5.DATABASE_DESIGN.md)과 [마이그레이션·백업·롤백 절차](https://github.com/MediFlow-EMR/mediflow-dev/blob/main/deploy/database/README.md)를 함께 확인하세요. `flyway_schema_history`는 Flyway 환경에 추가되는 인프라 테이블입니다.

## 문서 안내

- [1. 프로젝트 개요](1.PROJECT_OVERVIEW.md)
- [2. 경쟁사 분석](2.COMPETITOR_ANALYSIS.md)
- [3. 기능 명세](3.FEATURES.md)
- [4. 현재 아키텍처](4.ARCHITECTURE.md)
- [5. 데이터베이스 설계](5.DATABASE_DESIGN.md)
- [6. API 문서](6.API_DOCUMENTATION.md)
- [7. 향후 계획](7.ROADMAP.md)
- [8. 적용 범위와 법적 고려사항](8.LEGAL_COMPLIANCE.md)
- [9. 2026 개선 기록](9.REBUILD_2026.md)
- [10. 품질 검증 기록](10.QUALITY_AND_VALIDATION.md)

기존 개념 그림은 목표 구조·향후 계획을 포함합니다. 현재 구현 여부는 기능 명세·현재 아키텍처·품질 검증 기록을 기준으로 확인하세요. 실제 의료기관 적용에 필요한 법률·보안·임상 검증을 이 문서나 시연으로 대체할 수 없습니다.
