import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { EmailAuthService } from '../../services/authService';
import { useAuthStore } from '../../stores/authStore';
import styles from './LoginForm.module.scss';
import DemoAccountHint from './DemoAccountHint';

const LoginForm = () => {
    const navigate = useNavigate();
    const { setAuth } = useAuthStore();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');

        try {
            const response = await EmailAuthService.login(email, password);

            setAuth(null, null, {
                email: response.data.email,
                role: response.data.role,
            });

            // 로그인 성공 시 앱으로 이동
            navigate('/app');
        } catch (error) {
            let errorMessage = '로그인에 실패했습니다.';

            if (error.response?.status === 401) {
                errorMessage = '이메일 또는 비밀번호가 올바르지 않습니다.';
            } else if (error.response?.status === 400) {
                errorMessage = '이메일 인증을 완료해주세요.';
            } else if (!error.response || error.response.status >= 500) {
                errorMessage = '로그인 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.';
            } else if (error.response?.data?.message) {
                errorMessage = error.response.data.message;
            }

            setError(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <>
            <form onSubmit={handleSubmit} className={styles.loginForm}>
                <h2 className={styles.title}>
                    이메일로 로그인
                </h2>

                {error && (
                    <div role="alert" className={styles.errorMessage}>
                        {error}
                    </div>
                )}

                <div className={styles.formGroup}>
                    <label
                        htmlFor='email'
                        className={styles.label}
                    >
                        이메일
                    </label>
                    <input
                        id='email'
                        type='email'
                        name='email'
                        autoComplete='username'
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        disabled={isLoading}
                        className={styles.input}
                        placeholder="example@email.com"
                    />
                </div>

                <div className={styles.formGroup}>
                    <label
                        htmlFor='password'
                        className={styles.label}
                    >
                        비밀번호
                    </label>
                    <input
                        id='password'
                        type='password'
                        name='password'
                        autoComplete='current-password'
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        disabled={isLoading}
                        className={styles.input}
                        placeholder="비밀번호를 입력하세요"
                    />
                </div>

                <button
                    type='submit'
                    disabled={isLoading}
                    className={styles.submitButton}
                >
                    {isLoading ? '로그인 중...' : '로그인'}
                </button>

                <DemoAccountHint onUse={(account) => {
                    setEmail(account.email);
                    setPassword(account.password);
                    setError('');
                }} />

                <div className={styles.divider}>
                    <Link
                        to='/register'
                        className={styles.signupLink}
                    >
                        회원이 아니십니까? 회원가입을 해보세요
                    </Link>
                </div>

                <Link
                    to='/auth/signin'
                    className={styles.backLink}
                >
                    ← 뒤로 가기
                </Link>
            </form>
        </>
    );
};

export default LoginForm;
