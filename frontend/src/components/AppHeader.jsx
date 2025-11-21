import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthService } from '../services/authService';

const AppHeader = () => {
    const navigate = useNavigate();

    const handleLogout = async () => {
        try {
            await AuthService.logout();
            // 로컬 스토리지 정리
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            // 로그인 페이지로 이동
            navigate('/');
        } catch (error) {
            console.error('로그아웃 실패:', error);
            // 에러가 나도 토큰 삭제하고 로그인 페이지로 이동
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            navigate('/');
        }
    };

    return (
        <header className='border-b bg-white shadow-sm'>
            <div className='container mx-auto px-4 h-16 flex items-center justify-between'>
                <Link
                    to='/app'
                    className='text-xl font-bold text-sky-600 hover:text-sky-700 transition-colors'>
                    MediFlow
                </Link>
                <nav className='flex items-center gap-6'>
                    <Link
                        to='/app'
                        className='text-sm text-gray-600 hover:text-sky-600 transition-colors font-medium'>
                        홈
                    </Link>
                    <button
                        onClick={handleLogout}
                        className='px-4 py-2 bg-sky-500 hover:bg-sky-600 text-white text-sm font-medium rounded-lg transition-colors duration-200 shadow-sm'>
                        로그아웃
                    </button>
                </nav>
            </div>
        </header>
    );
};

export default AppHeader;