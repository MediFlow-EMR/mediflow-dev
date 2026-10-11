import apiClient from './apiClient';

/*
 인증 서비스:
 - me: 현재 로그인한 사용자 정보 조회
 - refresh: 액세스 토큰 재발급 요청 (이 호출은 인증 리프레시 로직을 건너뜀)
 - logout: 서버에 로그아웃 요청
*/
export const AuthService = {
    me: () => apiClient.get('/users/me'),
    refresh: () => apiClient.post('/auth/refresh', {}, { skipAuthRefresh: true }),
    logout: () => apiClient.post('/auth/logout'),
};

/*
 이메일 인증 서비스:
 - checkEmail: 이메일 중복 확인
 - verifyCode: 인증 코드 검증
 - getRemainingTime: 인증 코드 남은 시간 조회
 - signup: 회원가입 완료
 - login: 이메일 로그인
*/
export const EmailAuthService = {
    checkEmail: (email) => apiClient.get('/auth/email/check', { params: { email } }),
    sendCode: (email) => apiClient.post('/auth/email/send-code', { email }),
    verifyCode: (email, code) => apiClient.get('/auth/email/verify-code', { params: { email, code } }),
    getRemainingTime: (email) => apiClient.get('/auth/email/remaining-time', { params: { email } }),
    signup: (email, password) => apiClient.post('/auth/join', { email, password }),
    login: (email, password) => apiClient.post('/auth/email/login', { email, password }, { skipAuthRefresh: true }),
};
