# MediFlow 배포 체크리스트

## 배포 전

- [ ] 기존에 노출된 Gemini/API 키 폐기 및 재발급
- [ ] 실제 환자정보가 포함되지 않았는지 확인
- [ ] AWS Budget 알림 생성
- [ ] GitHub 원격 저장소 연결
- [ ] CI 성공

## AWS

- [ ] RDS private 생성
- [ ] RDS 3306 소스가 EC2 보안 그룹인지 확인
- [ ] EC2 IAM Role에 SSM과 배포 버킷 읽기 권한 연결
- [ ] EC2 9005 포트를 인터넷에 공개하지 않음
- [ ] EC2 환경변수 파일 권한 `640`
- [ ] S3 프런트 버킷 퍼블릭 차단 유지
- [ ] CloudFront OAC 연결
- [ ] SPA 403/404 응답 설정
- [ ] ACM/Route 53 연결

## GitHub Actions

- [ ] OIDC Provider 생성
- [ ] GitHub Actions IAM Role 최소 권한 적용
- [ ] `production` Environment 생성
- [ ] Repository Variables 등록
- [ ] 백엔드 수동 배포 성공
- [ ] 프런트 수동 배포 성공

## 기능 점검

- [ ] `/actuator/health` → `UP`
- [ ] 이메일 로그인
- [ ] Google/Kakao OAuth 콜백
- [ ] 쿠키 생성 및 Refresh Token 갱신
- [ ] 환자/오더/V/S/간호기록 조회
- [ ] 식약처 약품정보 조회
- [ ] Gemini 초안 생성
- [ ] CloudFront `/app` 새로고침

## 배포 후

- [ ] `JPA_DDL_AUTO=validate` 전환
- [ ] CloudWatch 로그 확인
- [ ] RDS 백업 확인
- [ ] 비용 알림 확인
- [ ] 현재 UI의 로그인·환자 필터·간호기록·오더·인수인계 GIF와 Docs 반영 확인

## DB 마이그레이션 배포 확인

- 배포 전 RDS 백업·복구 상태와 연결 대상 DB, 스키마 권한을 확인합니다.
- 기존 DB 최초 도입에만 FLYWAY_BASELINE_ON_MIGRATE=true로 20261010 기준선을 등록합니다. 이후 false로 유지합니다.
- 새 JAR의 prod 시작 시 Flyway가 버전별 SQL을 실행하고 Hibernate validate 후 건강 상태를 확인합니다.
- flyway_schema_history의 성공 기록 및 기존 인수인계 보존을 확인합니다. 체크섬 불일치·실패는 서버 기동을 막습니다.
- 앱 롤백은 DB 변경을 되돌리지 않습니다. [마이그레이션 절차](../database/README.md)를 따릅니다.
