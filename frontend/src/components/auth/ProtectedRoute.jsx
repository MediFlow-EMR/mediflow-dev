import React, { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import apiClient from '../../services/apiClient';

/**
 * 인증이 필요한 라우트를 보호하는 컴포넌트
 * 
 * 쿠키 기반 인증:
 * - 쿠키가 유효한지 API 호출로 검증
 * - 유효하면 인증 상태 업데이트
 */
const ProtectedRoute = ({ children }) => {
  const location = useLocation();
  const { setAuth, clear } = useAuthStore();
  const [isChecking, setIsChecking] = useState(true);
  const [isValid, setIsValid] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    const checkAuth = async () => {
      try {
        const response = await apiClient.get('/users/me', { signal: controller.signal });
        if (controller.signal.aborted) return;
        setAuth(null, null, response.data.data);
        setIsValid(true);
      } catch {
        if (controller.signal.aborted) return;
        clear();
        setIsValid(false);
      } finally {
        if (!controller.signal.aborted) setIsChecking(false);
      }
    };
    checkAuth();
    return () => controller.abort();
  }, [setAuth, clear]);

  // 인증 확인 중
  if (isChecking) {
    return (
      <div
        style={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          fontSize: '18px',
          color: 'var(--ink-6b7280)',
        }}
      >
        로딩중...
      </div>
    );
  }

  // 인증되지 않은 경우 로그인 페이지로 리다이렉트
  if (!isValid) {
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  // 인증된 경우 자식 컴포넌트 렌더링
  return children;
};

export default ProtectedRoute;
