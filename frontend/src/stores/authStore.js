import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * 인증 상태 관리 스토어 (Zustand)
 * - accessToken: JWT 액세스 토큰
 * - refreshToken: JWT 리프레시 토큰
 * - user: 현재 로그인한 사용자 정보
 * - isAuthenticated: 로그인 상태 여부
 */
export const useAuthStore = create(
    persist(
        (set) => ({
            accessToken: null,
            refreshToken: null,
            user: null,
            isAuthenticated: false,

            // 로그인 성공 시 토큰과 사용자 정보 저장
            setAuth: (accessToken, refreshToken, user) =>
                set({
                    accessToken,
                    refreshToken,
                    user,
                    isAuthenticated: true,
                }),

            // 액세스 토큰만 갱신
            setAccessToken: (accessToken) =>
                set({
                    accessToken,
                }),

            // 사용자 정보 업데이트
            setUser: (user) =>
                set({
                    user,
                }),

            // 로그아웃 또는 인증 만료 시 모든 상태 초기화
            clear: () =>
                set({
                    accessToken: null,
                    refreshToken: null,
                    user: null,
                    isAuthenticated: false,
                }),
        }),
        {
            name: 'auth-storage', // 로컬 스토리지 키 이름
            partialize: (state) => ({
                // 리프레시 토큰만 로컬 스토리지에 저장 (보안상 accessToken은 메모리에만)
                refreshToken: state.refreshToken,
                user: state.user,
                isAuthenticated: state.isAuthenticated,
            }),
        }
    )
);

