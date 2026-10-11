import { useNavigationGuard } from './navigationGuard';
import useDashboardStore from './useDashboardStore';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
    persist(
        (set) => ({
            user: null,
            isAuthenticated: false,

            setAuth: (accessToken, refreshToken, user) =>
                set({
                    user,
                    isAuthenticated: true,
                }),

            setUser: (user) =>
                set({
                    user,
                    isAuthenticated: true,
                }),

            clear: () => {
                useDashboardStore.getState().reset();
                useNavigationGuard.getState().reset();
                set({ user: null, isAuthenticated: false });
            },
        }),
        {
            name: 'auth-storage', // 로컬 스토리지 키 이름
            partialize: (state) => ({

                user: state.user,
                isAuthenticated: state.isAuthenticated,
            }),
        }
    )
);

