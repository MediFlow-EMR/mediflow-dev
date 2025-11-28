// 라우트 설정 - 데이터 방식 사용
import { createBrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import LoginPage from './pages/LoginPage.jsx';
import EmailLoginPage from './pages/EmailLoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import TermsOfServicePage from './pages/TermsOfServicePage.jsx';
import PrivacyPolicyPage from './pages/PrivacyPolicyPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import AiTestPage from './pages/AiTestPage.jsx';
import AppLayout from './layouts/AppLayout.jsx';
import ProtectedRoute from './components/auth/ProtectedRoute.jsx';
import PublicRoute from './components/auth/PublicRoute.jsx';

export const router = createBrowserRouter([
    {
        path: '/',
        element: (
            <PublicRoute>
                <LoginPage />
            </PublicRoute>
        ),
    },
    {
        path: '/login',
        element: (
            <PublicRoute>
                <EmailLoginPage />
            </PublicRoute>
        ),
    },
    {
        path: '/register',
        element: (
            <PublicRoute>
                <RegisterPage />
            </PublicRoute>
        ),
    },
    {
        path: '/sign-up',
        element: (
            <PublicRoute>
                <RegisterPage />
            </PublicRoute>
        ),
    },
    {
        path: '/terms',
        element: <TermsOfServicePage />,
    },
    {
        path: '/privacy',
        element: <PrivacyPolicyPage />,
    },
    {
        path: '/app',
        element: (
            <ProtectedRoute>
                <AppLayout />
            </ProtectedRoute>
        ),
        children: [
            {
                index: true,
                element: <DashboardPage />,
            },
            // AI 테스트 페이지 - 개발용으로 주석 처리
            // {
            //     path: 'ai-test',
            //     element: <AiTestPage />,
            // },
        ],
    },

]);

export default router;