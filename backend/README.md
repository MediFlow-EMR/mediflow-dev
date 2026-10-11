# MediFlow Backend

Java 21 · Spring Boot 3.5.6 · JPA/QueryDSL · MariaDB 기반 REST API입니다.

## 역할

- 이메일·Google·Kakao 인증과 HttpOnly 쿠키 기반 JWT 처리
- 담당·부서 환자 조회, 간호기록·V/S·섭취배설·투약·오더·검사결과 관리
- Gemini 기반 인수인계 초안 및 환자 단위 재생성
- 식약처 e약은요·제품 허가정보 조회, 품목번호 기반 데모 처방 매핑
- 선택적으로 S3 약품 캐시를 읽고, 유효한 캐시가 없으면 허가정보 API 조회

AI 요청의 이름·차트번호는 요청 내 가명으로 대체하지만 자유 입력 기록의 개인정보를 자동 제거하지는 않습니다. 실제 개인정보를 입력하지 마세요.

## 실행 및 테스트

`.env.example`과 [루트 실행 안내](../README.md)를 참고하세요. 기존 MariaDB는 `local` 프로필로 실행합니다. `local`·`dev` 기본값은 validate입니다. local은 Flyway·데모 초기화를 실행하지 않습니다. 빈 테스트 DB 초기 생성에만 JPA_DDL_AUTO=create를 명시하며 운영 DB에 사용하지 않습니다.

```sh
bash ./gradlew test bootJar
bash ./gradlew bootRun --args='--spring.profiles.active=local'
```

`dev`·`demo` 프로필에서 가상 환자와 공용 테스트 계정이 초기화됩니다. `prod` 단독에서는 데모 초기화가 실행되지 않습니다.

## 배포

EC2의 systemd 서비스로 실행하며 Nginx가 HTTPS 요청을 전달합니다. GitHub Actions는 OIDC 인증 후 S3 배포 파일과 SSM을 이용합니다. [AWS 가이드](../deploy/aws/README.md)와 [워크플로우](../.github/workflows/deploy-backend.yml)를 기준으로 설정하세요.


## 인수인계와 마이그레이션

수기·AI 초안을 서버에 임시저장하고 재조회·수정·승인합니다. 승인 후에는 작성자만 수정하며 매 저장마다 `handover_revision`에 스냅샷을 추가합니다. 승인 후 AI 원본·작성 구분 변경 및 초안 전환을 금지합니다. PUT version 불일치는 409입니다. 초안은 작성자 전용, 승인 기록·이력은 같은 부서 조회입니다.

prod는 Flyway 실행 후 Hibernate validate로 시작합니다. 기존 DB 최초 전환에는 백업과 baseline 설정이 필요합니다. [마이그레이션 안내](../deploy/database/README.md)를 확인하세요. JAR 롤백은 DB 롤백이 아닙니다. 최신 API는 [swagger.yaml](../swagger.yaml)이며 내장 Springdoc UI는 설치되어 있지 않습니다.

쿠키는 설정한 secure·same-site·domain을 그대로 적용합니다. 로컬은 host-only, HttpOnly, Lax; 운영은 Secure·None으로 설정합니다. 쿠키와 로그아웃 삭제 범위도 동일합니다.


## 임상 기록 수정 계약

간호기록·V/S·I/O 조회·저장 응답에 `version`을 반환합니다. PUT 요청의 version은 필수이며 누락·오래된 값·동시 수정은 409로 처리합니다. 환자 변경은 400, 작성자 또는 소속 부서 접근 거부는 403입니다. 저장 후 flush한 버전을 반환하므로 같은 화면에서 다시 수정할 수 있습니다. 기존 version 컬럼을 사용하므로 이번 계약 변경만을 위한 추가 DB 마이그레이션은 없습니다.

V/S·I/O는 측정값을 하나 이상 요구하며 I/O의 0은 유효합니다. 오더 등록은 필수값과 문자열 길이를 검증합니다. 잘못된 JSON/파라미터는 400이며 권한 거부는 403으로 반환합니다. 오더 약품 상세 조회·삭제도 환자 접근 경계를 통과해야 합니다.

[검증 기록](../portfolio-docs/10.QUALITY_AND_VALIDATION.md)에 격리 H2 통합 테스트와 로컬 UI 확인 범위를 기록했습니다.
