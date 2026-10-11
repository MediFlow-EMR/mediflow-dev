import { calendarDate, dateValue } from '../../utils/calendar';
import { useNavigationGuard } from '../../stores/navigationGuard';
import ThemeToggle from '../common/ThemeToggle';
import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import useDashboardStore from '../../stores/useDashboardStore';
import { useAuthStore } from '../../stores/authStore';
import { AuthService } from '../../services/authService';
import DatePicker from '../common/DatePicker';
import CustomSelect from '../common/CustomSelect';
import styles from './AppHeader.module.scss';

const AppHeader = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { departmentSummary, selectedDate, selectedShiftType, setContext } = useDashboardStore();
    const { clear } = useAuthStore();

    const handleLogoClick = () => useNavigationGuard.getState().requestNavigation(() => {
        // /app 경로가 아닌 페이지에서만 navigate 실행
        if (location.pathname !== '/app') {
            navigate('/app');
        }
        // /app 경로에서는 탭을 'my'로 변경
        useDashboardStore.getState().setActiveTab('my');
    });

    const handleLogout = () => useNavigationGuard.getState().requestNavigation(async () => {
        try {
            await AuthService.logout();
        } catch {
            // Clear local authentication when the logout endpoint is unavailable.
        } finally {
            clear();
            navigate('/');
        }
    });

    const handleDateChange = (date) => {
        const nextDate = dateValue(date);
        if (nextDate && nextDate !== selectedDate) useNavigationGuard.getState().requestNavigation(() => setContext(nextDate));
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
                <ThemeToggle />
                <div className={styles.userInfo}>
                    <div className={styles.nurseName}>
                        {departmentSummary?.nurseName || '사용자'}
                    </div>
                    <div className={styles.shiftInfo}>
                        {departmentSummary?.shiftType || '근무조'} 근무
                    </div>
                </div>
                <DatePicker selectedDate={calendarDate(selectedDate)} onDateChange={handleDateChange} />
                <CustomSelect label="조회 근무조" value={selectedShiftType} options={[
                    { value: '', label: '현재 시간 근무조' }, { value: 'DAY', label: 'Day 근무' },
                    { value: 'EVENING', label: 'Evening 근무' }, { value: 'NIGHT', label: 'Night 근무' },
                ]} onChange={type => { if (type !== selectedShiftType) useNavigationGuard.getState().requestNavigation(() => setContext(selectedDate, type)); }} />
                <button className={styles.logoutBtn} onClick={handleLogout}>
                    로그아웃
                </button>
            </div>
        </header>
    );
};

export default AppHeader;
