import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import useDashboardStore from '../../stores/useDashboardStore';
import { useAuthStore } from '../../stores/authStore';
import { AuthService } from '../../services/authService';
import DatePicker from '../common/DatePicker';
import styles from './AppHeader.module.scss';

const AppHeader = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { departmentSummary } = useDashboardStore();
    const { clear } = useAuthStore();
    const [selectedDate, setSelectedDate] = useState(new Date());

    const handleLogoClick = () => {
        // /app 경로가 아닌 페이지에서만 navigate 실행
        if (location.pathname !== '/app') {
            navigate('/app');
        }
        // /app 경로에서는 탭을 'my'로 변경
        useDashboardStore.getState().setActiveTab('my');
    };

    const handleLogout = async () => {
        try {
            await AuthService.logout();
        } catch (error) {
            console.error('로그아웃 실패:', error);
        } finally {
            clear();
            navigate('/');
        }
    };

    const handleDateChange = (date) => {
        setSelectedDate(date);
    };

    return (
        <header className={styles.header}>
            <div className={styles.left}>
                <div
                    className={styles.logo}
                    onClick={handleLogoClick}
                    onKeyDown={(event) => {
                        if (event.key === 'Enter' || event.key === ' ') handleLogoClick();
                    }}
                    role="button"
                    tabIndex={0}
                    title="내 담당 환자로 이동"
                >
                    MediFlow
                </div>
                <div className={styles.hospitalInfo}>
                    <div className={styles.hospitalName}>한국대학교병원</div>
                    <div className={styles.departmentName}>
                        {departmentSummary?.departmentCode || '부서 정보...'}
                    </div>
                </div>
            </div>

            <div className={styles.right}>
                {/*<button
                    className={styles.aiTestBtn}
                    onClick={() => navigate('/app/ai-test')}
                >
                    🤖 AI 테스트
                </button>*/}
                <div className={styles.userInfo}>
                    <div className={styles.nurseName}>
                        {departmentSummary?.nurseName || '사용자'}
                    </div>
                    <div className={styles.shiftInfo}>
                        {departmentSummary?.shiftType || '근무조'} 근무
                    </div>
                </div>
                <DatePicker selectedDate={selectedDate} onDateChange={handleDateChange} />
                <button className={styles.logoutBtn} onClick={handleLogout}>
                    로그아웃
                </button>
            </div>
        </header>
    );
};

export default AppHeader;
