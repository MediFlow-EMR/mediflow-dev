// 라우트 설정 - 데이터 방식 사용
import { createBrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import LoginPage from './pages/LoginPage.jsx';
import EmailLoginPage from './pages/EmailLoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import AppLayout from './layouts/AppLayout.jsx';

export const router = createBrowserRouter([
    {
        path: '/',
        element: <LoginPage />, // 최초 진입은 로그인 화면
    },
    {
        path: '/login',
        element: <EmailLoginPage />, // 이메일 로그인 폼
    },
    {
        path: '/register',
        element: <RegisterPage />, // 이메일 회원가입
    },
    {
        path: '/sign-up',
        element: <RegisterPage />, // 이메일 회원가입 (별칭)
    },
    {
        path: '/app',
        element: <AppLayout />,
        children: [{ index: true, element: <App /> }],
    },
]);

export default router;