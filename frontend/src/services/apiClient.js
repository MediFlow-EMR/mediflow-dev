import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

// 현재 환경이 production인지 확인하는 플래그를 만듭니다.
const isProduction = import.meta.env.PROD;

// axios 인스턴스 만들기
// - baseURL: 로컬(dev)에서는 /api (Vite Proxy), 배포(prod)에서는 전체 URL을 사용합니다.
// - withCredentials: HTTP-Only 쿠키를 자동으로 포함/수신
const apiClient = axios.create({
    // baseURL을 환경에 따라 동적으로 설정
    // 배포 환경이면 .env.production의 전체 주소(https://api...)를 사용하고,
    // 로컬 환경이면 '/api' (Vite Proxy)를 사용합니다.
    baseURL: isProduction ? import.meta.env.VITE_API_BASE_URL : '/api',
    withCredentials: true,
});

// =====================================================
//  아주 쉽게 보는 "Access Token 자동 갱신" 이야기
// -----------------------------------------------------
// 1) 우리가 서버에 요청을 보냈는데, Access Token 이 만료되면 401(인증 실패)이 와요.
// 2) 그러면 Refresh Token 으로 "새로운 Access Token"을 받아옵니다.
// 3) 새 Access Token 을 받으면, 방금 실패했던 요청을 그대로 다시 보내요.
// 4) 사용자는 서비스가 끊기지 않고 자연스럽게 이어집니다.
// =====================================================

// 한 번에 여러 요청이 401이 될 수 있기 때문에,
// "한 명만" 토큰을 갱신하고, 나머지는 그 결과를 기다렸다가 재시도합니다.
let isRefreshing = false; // 지금 갱신 중인지 알려주는 논리값
let refreshPromise = null; // 갱신이 끝나길 기다릴 약속(같은 약속을 여러 요청이 함께 기다림)

// 요청 전 단계(여기서는 아무 것도 하지 않아요)
apiClient.interceptors.request.use(
    (config) => config,
    (error) => Promise.reject(error)
);

// 응답 후 단계: 401 을 만나면 자동 갱신을 시도합니다.
apiClient.interceptors.response.use(
    (res) => res,
    async (error) => {
        const { clear } = useAuthStore.getState();
        const originalRequest = error?.config || {}; // 방금 실패한 원래 요청
        const status = error?.response?.status;

        console.log('originalRequest: ', originalRequest);


        // 네트워크 에러 등 상태 코드가 없으면 그대로 실패 반환
        if (!status) return Promise.reject(error);


        // 401(인증 실패)이 아닌 경우, 여기서는 처리하지 않음
        // 또한, 아래에서 refresh 를 호출할 때는 skipAuthRefresh 플래그로 여기 재진입을 막습니다.
        if (status !== 401 || originalRequest?.skipAuthRefresh) {
            return Promise.reject(error);
        }

        // 만약 이 에러가 "리프레시 요청 자체"에서 난 거라면 → Refresh Token 도 만료
        // 이 경우에는 더 이상 방법이 없으니 로그인 상태를 비웁니다.
        const isRefreshCall = originalRequest?.url?.includes('/auth/refresh');
        if (isRefreshCall) {
            clear();
            return Promise.reject(error);
        }

        // 같은 요청을 무한히 재시도하지 않도록 안전장치
        if (originalRequest._retry) {
            clear();
            return Promise.reject(error);
        }

        try {
            if (!isRefreshing) {
                console.log('액세스 토큰 재발급 시작');

                // 내가 갱신 담당일 때: 한 번만 새 토큰을 받아옵니다.
                isRefreshing = true;

                // # 왜 _retry 플래그를 사용하는가??
                // 1. 원래 요청이 401로 실패하면, 우리가 "Access Token을 새로 받아서" 한 번만 재시도해요.
                // 2. 이때, originalRequest._retry = true; 로 표시해둡니다.
                // 3. 만약 재시도한 요청도 또 401이 뜬다면? → 이건 진짜로 토큰이 만료됐거나, refresh도 실패한 상황!
                //    (예: refresh token도 만료, 서버에서 세션 만료 등)
                // 4. 그래서 위에서 _retry가 true인 요청이 또 401로 오면, 더 이상 무한 재시도하지 않고
                //    clear()로 인증 상태를 비우고, 에러를 반환하는 거예요.
                //
                // 즉, _retry는 "이 요청이 이미 한 번 토큰 갱신 후 재시도된 적이 있다"는 표시!
                // 무한 루프 방지용 안전장치라고 생각하면 됩니다.
                originalRequest._retry = true; // 이 요청은 한 번 재시도 허용

                // 동적 import 로 AuthService 를 가져옵니다.
                // 정적 import 로 하면 순환 의존(서로가 서로를 가져옴)이 생길 수 있어서요.
                // 여기서 AuthService를 동적으로 import하는 이유를 좀 더 자세히 설명할게요!
                //
                // 만약 파일 상단에서
                //   import { AuthService } from '../services/authService';
                // 이렇게 "정적 import"를 해버리면,
                // 1. apiClient.js → authService.js 를 import
                // 2. 그런데 authService.js 안에서도 apiClient를 import하고 있죠!
                //    (export const AuthService = { ... apiClient.get ... })
                // 3. 즉, 두 파일이 서로를 동시에 import하려고 하니까
                //    "순환 의존성(circular dependency)" 문제가 생깁니다.
                //    → import 시점에 아직 완성되지 않은 객체가 넘어와서, undefined가 되거나
                //      예기치 않은 동작이 발생할 수 있어요.
                //
                // 그래서 여기서는 "동적 import" (import() 함수)를 사용합니다.
                // 동적 import는 이 코드가 실제로 실행될 때(= 401 에러가 발생해서 토큰 갱신이 필요할 때)
                // 그때서야 authService.js를 불러오니까,
                // 순환 의존 문제가 발생하지 않습니다!
                //
                // 즉, "정적 import는 파일 로딩 시점에, 동적 import는 함수 실행 시점에" 불러온다는 차이!
                // 이런 상황에서는 동적 import가 안전한 선택이에요.
                const { AuthService } = await import('../services/authService');

                // refreshPromise 에 "리프레시 요청"을 저장해 두고, 다른 요청들도 이 약속을 함께 기다리게 합니다.
                refreshPromise = AuthService.refresh();

                // 새 Access Token 발급이 끝날 때까지 기다립니다.
                const res = await refreshPromise;
                console.log('res if: ', res);
            } else {
                // 누군가 이미 갱신 중이면, 그 갱신이 끝날 때까지 기다립니다.
                await refreshPromise;
                originalRequest._retry = true;
            }

            // 여기까지 왔다면, 갱신이 끝난 상태 → 방금 실패한 요청을 다시 보냅니다.
            return apiClient(originalRequest);
        } catch (refreshErr) {
            // 리프레시도 실패한 경우 → 로그인 상태 초기화 후 실패 반환
            clear();
            return Promise.reject(refreshErr);
        } finally {
            // 갱신 끝! 다음 401을 위해 깃발을 내립니다.
            isRefreshing = false;
        }
    }
);

export default apiClient;