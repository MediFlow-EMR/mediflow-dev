import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';

/**
 * 로그인하지 않은 사용자만 접근 가능한 라우트
 * (로그인한 사용자가 접근하면 대시보드로 리다이렉트)
 */
const PublicRoute = ({ children }) => {
  const { isAuthenticated } = useAuthStore();

  // 이미 로그인한 경우 앱으로 리다이렉트
  if (isAuthenticated) {
    return <Navigate to="/app" replace />;
  }

  // 로그인하지 않은 경우 자식 컴포넌트 렌더링
  return children;
};

export default PublicRoute;
