import LoginForm from '../components/auth/LoginForm';
import mediflowIcon from '../assets/mediflow-icon.svg';
import styles from './EmailLoginPage.module.scss';

export default function EmailLoginPage() {
    return (
        <div className={styles.container}>
            <div className={styles.wrapper}>

                {/* 로고 섹션 */}
                <div className={styles.logoSection}>
                    <div className={styles.logoWrapper}>
                        <img src={mediflowIcon} alt="MediFlow" />
                    </div>
                    <h1 className={styles.title}>
                        MediFlow
                    </h1>
                    <p className={styles.subtitle}>전자의무기록 시스템</p>
                </div>

                {/* 로그인 폼 */}
                <div className={styles.card}>
                    <LoginForm />
                </div>
            </div>
        </div>
    );
}

