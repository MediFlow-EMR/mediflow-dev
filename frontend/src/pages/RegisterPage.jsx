import SignUpForm from '../components/auth/SignUpForm';
import mediflowIcon from '../assets/mediflow-icon.svg';
import styles from './RegisterPage.module.scss';

export default function RegisterPage() {
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

                {/* 회원가입 폼 */}
                <div className={styles.card}>
                    <h2 className={styles.cardTitle}>
                        이메일로 회원가입
                    </h2>
                    
                    <SignUpForm />
                </div>
            </div>
        </div>
    );
}

