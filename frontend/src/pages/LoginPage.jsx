import { useNavigate } from 'react-router-dom';
import googleLogo from '../assets/google-logo.svg';
import kakaoLogo from '../assets/kakao-logo.svg';
import mediflowIcon from '../assets/mediflow-icon.svg';
import styles from './LoginPage.module.scss';

export default function LoginPage() {
    const navigate = useNavigate();

    const handleGoogleLogin = () => {
        window.location.href = `${import.meta.env.VITE_API_BASE_URL}/oauth2/authorization/google`;
    };

    const handleKakaoLogin = () => {
        window.location.href = `${import.meta.env.VITE_API_BASE_URL}/oauth2/authorization/kakao`;
    };

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

                {/* 로그인 카드 */}
                <div className={styles.card}>
                    {/* 버튼 그룹 */}
                    <div className={styles.buttonGroup}>
                        {/* 로그인 버튼 */}
                        <button
                            onClick={() => navigate('/login')}
                            className={styles.primaryButton}
                        >
                            이메일로 로그인
                        </button>

                        {/* 회원가입 버튼 */}
                        <button
                            onClick={() => navigate('/register')}
                            className={styles.secondaryButton}
                        >
                            이메일로 회원가입
                        </button>

                        {/* 구분선 */}
                        <div className={styles.divider}>
                            <div className={styles.dividerLine}>
                                <div></div>
                            </div>
                            <div className={styles.dividerText}>
                                <span>또는</span>
                            </div>
                        </div>

                        {/* 소셜 로그인 버튼 */}
                        <button
                            onClick={handleGoogleLogin}
                            className={styles.socialButton}
                        >
                            <img src={googleLogo} alt="Google" />
                            Google로 계속하기
                        </button>

                        <button
                            onClick={handleKakaoLogin}
                            className={`${styles.socialButton} ${styles.kakao}`}
                        >
                            <img src={kakaoLogo} alt="Kakao" />
                            카카오로 계속하기
                        </button>
                    </div>
                </div>

                {/* 하단 텍스트 */}
                <p className={styles.footer}>
                    로그인하시면{' '}
                    <button
                        onClick={() => navigate('/terms')}
                    >
                        이용약관
                    </button>
                    과{' '}
                    <button
                        onClick={() => navigate('/privacy')}
                    >
                        개인정보처리방침
                    </button>
                    에 동의하는 것으로 간주됩니다.
                </p>
            </div>
        </div>
    );
}
