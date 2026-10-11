import { useNavigationGuard } from '../stores/navigationGuard';
import ConfirmModal from '../components/common/ConfirmModal';
import React from 'react';
import { Outlet } from 'react-router-dom';
import AppHeader from '../components/layout/AppHeader';
import AppFooter from '../components/layout/AppFooter';
import styles from './AppLayout.module.scss';

const AppLayout = () => {
    const confirmation = useNavigationGuard(state => state.confirmation);
    const closeConfirmation = useNavigationGuard(state => state.closeConfirmation);
    return (
        <div className={styles.appLayout}>
            <AppHeader />
            <main className={styles.main}>
                <Outlet />
            </main>
            <AppFooter />
            {confirmation && <ConfirmModal title={confirmation.blocked ? '작업 진행 중' : '미저장 변경 확인'}
                message={confirmation.message} confirmText={confirmation.blocked ? '확인' : '화면 나가기'}
                danger={!confirmation.blocked} onClose={closeConfirmation}
                onConfirm={() => { closeConfirmation(); confirmation.action(); }} />}
        </div>
    );
};

export default AppLayout;