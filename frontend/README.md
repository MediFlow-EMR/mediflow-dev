# MediFlow Frontend

React 19 + Vite 기반 병동 간호 업무 데모입니다. 환자 대시보드와 오더·투약·간호기록·V/S·섭취배설·검사결과, 인수인계 초안 검토 화면을 제공합니다.

## 실행 및 검증

Node.js 22를 사용합니다. `.env.example`을 참고해 로컬 `.env`를 설정하세요. 실제 키나 비밀번호는 커밋하지 않습니다.

```sh
npm ci
npm run dev
npm test
npm run lint
npm run build
```

개발 화면: `http://localhost:3000`. 로컬 API는 Vite가 `http://localhost:9005`로 프록시합니다. 백엔드를 먼저 실행하세요. `VITE_API_BASE_URL`은 운영 API 및 OAuth 주소에 사용합니다.

## 데모 체험

[배포 로그인 화면](https://www.mediflow-emr.cloud/auth/signin)에서 이메일 로그인을 선택합니다. 아이디 `nurseer1@mediflow.com`, 비밀번호 `nurse123@` 또는 **데모 계정 입력** 버튼을 사용할 수 있습니다.

공용 계정의 가상 환자·기록은 공유됩니다. 실제 개인정보는 입력하지 마세요. AI 결과는 검토가 필요한 초안이며 진료 판단을 대체하지 않습니다.

## 배포

GitHub Actions가 `main`의 프론트엔드 변경을 빌드해 S3에 업로드하고 CloudFront 캐시를 무효화합니다. [워크플로우](../.github/workflows/deploy-frontend.yml)를 기준으로 확인하세요.

소개 화면의 GIF 4종을 2026.10.11 현재 UI의 실제 브라우저 조작 화면으로 교체했습니다. 로그인 시연 GIF도 추가했으며 GitHub README 및 문서 저장소와 같은 파일을 사용합니다.

## UI 및 기록 흐름

라이트모드가 기본이며 테마 전환은 저장됩니다. 랜딩·인증·메인·상세 모두 테마를 사용합니다. 데스크톱 가로 배치는 유지하고 모바일에서는 일부 배치를 세로로 전환합니다. 공통 입력 포커스·키보드 접근·reduced-motion을 지원합니다.

이메일 로그인 뒤로가기는 `/auth/signin`, 회원가입 뒤로가기는 `/login`으로 이동합니다. API 500·네트워크 오류는 서버 연결 안내를 표시하며, 로그인 자격증명 실패에서는 refresh를 재시도하지 않습니다. 인증 요청과 응답 객체를 콘솔에 출력하지 않습니다.

환자·간호기록·인수인계 목록은 복합 필터와 초기화를 제공합니다. 수기·AI 인수인계는 서버 임시저장 → 재조회·수정 → 승인 → 작성자 수정·이력 조회 흐름입니다. `aiSummary`는 원본, `finalSummary`는 편집본입니다. 수정 요청은 서버 버전을 전송하고 충돌 시 입력 내용을 유지합니다. 초안에서 나갈 때 저장하지 않은 변경을 확인합니다.

Vite HMR WebSocket의 “Page entered Back-Forward Cache” 메시지는 개발 페이지가 브라우저 이력 캐시에 들어갈 때 발생할 수 있습니다. 로그인 REST API의 500과 구분해 서버 실행·9005 포트·프록시부터 확인합니다.


## 공통 컨트롤 및 안정성

날짜 정렬은 최신순이 기본값이며 최신순/오래된순을 선택합니다. DatePicker, CustomSelect, DateTimePicker는 공통 테마와 키보드 탐색을 사용하고 팝오버를 portal로 렌더링합니다. 필터 초기화는 정렬도 최신순으로 되돌립니다.

`useRecordList`는 조회 취소·로딩·오류·재조회를 통합합니다. `useUnsavedRecord`와 navigationGuard는 공통 모달로 이동을 확인하며 저장 중에는 이동하지 않습니다. 간호기록·V/S·I/O 수정은 조회 버전을 보내고 실패 시 입력을 유지합니다. API 클라이언트는 동시 401의 refresh를 한 번으로 모으고 일반 쓰기 실패를 자동 재전송하지 않습니다. AI 생성은 별도 120초 제한을 사용합니다.

화면은 CSS Modules/SCSS와 공통 CSS로 구성합니다. 미사용 Tailwind·개발 전용 페이지·Markdown/STOMP 의존성은 제거했습니다. 라우트와 차트 번들을 나누며 랜딩 진입 시 차트 번들을 미리 로드하지 않습니다. HTML 정제는 DOMPurify, DOM 기반 보안 테스트는 개발 의존성 jsdom을 사용합니다.

검증 명령은 `npm test`, `npm run lint`, `npm run build`, `npm audit`입니다. 상세 결과와 범위는 [검증 기록](../portfolio-docs/10.QUALITY_AND_VALIDATION.md)을 참고하세요.

배포는 assets 업로드 → 기타 정적 파일 → index.html 교체 순서로 진행하며 이전 해시 번들을 보존합니다. 배포 전에 열린 화면의 동적 import가 이전 번들을 계속 사용할 수 있습니다.
