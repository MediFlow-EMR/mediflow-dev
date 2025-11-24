import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * 인증 상태 관리 스토어 (Zustand)
 * 
 * ===================================================================
 * 인증 방식 변경 이력 (로컬스토리지 → HTTP-Only 쿠키)
 * ===================================================================
 *
 * [변경 전] 로컬스토리지 기반 인증
 * - JWT 토큰(accessToken, refreshToken)을 로컬스토리지에 저장
 * - 문제점:
 *   1. XSS 공격에 취약 (JavaScript로 토큰 접근 가능)
 *   2. 토큰이 클라이언트에 노출되어 보안 위험
 *   3. 민감한 정보가 브라우저 개발자 도구에서 보임
 *
 * [변경 후] HTTP-Only 쿠키 기반 인증 (현재)
 * - JWT 토큰은 HTTP-Only, Secure 쿠키로 백엔드에서 자동 설정
 * - 장점:
 *   1. XSS 공격 방어 (JavaScript로 쿠키 접근 불가)
 *   2. CSRF 방어는 SameSite 속성으로 처리
 *   3. 토큰 관리를 백엔드에서 안전하게 처리
 *   4. 토큰 갱신(refresh)도 쿠키로 자동 처리
 *
 * - 프론트엔드 역할:
 *   - 사용자 정보(user)와 인증 상태(isAuthenticated)만 관리
 *   - API 요청 시 쿠키는 브라우저가 자동으로 포함
 *   - 토큰은 직접 저장하거나 관리하지 않음
 *
 * ===================================================================
 */
export const useAuthStore = create(
    persist(
        (set) => ({
            user: null,
            isAuthenticated: false,

            /**
             * 로그인 성공 시 사용자 정보 저장
             *
             * @param {string} accessToken - [사용하지 않음] 백엔드 호환성을 위해 파라미터 유지
             * @param {string} refreshToken - [사용하지 않음] 백엔드 호환성을 위해 파라미터 유지
             * @param {Object} user - 사용자 정보 (이름, 이메일 등)
             *
             * 참고:
             * - accessToken, refreshToken은 받지만 저장하지 않음
             * - 실제 토큰은 HTTP-Only 쿠키로 백엔드에서 자동 설정됨
             * - 파라미터를 유지하는 이유: 기존 authService 코드 호환성
             */
            setAuth: (accessToken, refreshToken, user) =>
                set({
                    user,
                    isAuthenticated: true,
                }),

            /**
             * 사용자 정보 업데이트
             *
             * @param {Object} user - 업데이트할 사용자 정보
             *
             * 사용 사례:
             * - 프로필 수정 후 정보 갱신
             * - 토큰 갱신 후 사용자 정보 동기화
             */
            setUser: (user) =>
                set({
                    user,
                    isAuthenticated: true,
                }),

            /**
             * 로그아웃 또는 인증 만료 시 모든 상태 초기화
             *
             * 처리 사항:
             * - 로컬스토리지의 사용자 정보 삭제
             * - 인증 상태 false로 설정
             * - 쿠키는 백엔드 로그아웃 API 호출 시 서버에서 삭제
             */
            clear: () =>
                set({
                    user: null,
                    isAuthenticated: false,
                }),
        }),
        {
            name: 'auth-storage', // 로컬 스토리지 키 이름
            partialize: (state) => ({
                /**
                 * 로컬스토리지에 저장할 데이터 선택
                 *
                 * 저장 항목:
                 * - user: 사용자 기본 정보 (이름, 이메일 등)
                 * - isAuthenticated: 인증 상태
                 *
                 * 저장하지 않는 항목:
                 * - accessToken: HTTP-Only 쿠키로 관리
                 * - refreshToken: HTTP-Only 쿠키로 관리
                 *
                 * 이유: 토큰은 보안상 로컬스토리지에 저장하지 않음
                 */
                user: state.user,
                isAuthenticated: state.isAuthenticated,
            }),
        }
    )
);

