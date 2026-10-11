import { useRef, useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../../services/apiClient';
import styles from './EmailInput.module.scss';

const EmailInput = ({ onSuccess }) => {
    const emailRef = useRef();
    const checkController = useRef(null);
    const debounceTimerRef = useRef(null);

    const [error, setError] = useState('');
    const [email, setEmail] = useState('');
    const [isChecking, setIsChecking] = useState(false);
    const [isEmailValid, setIsEmailValid] = useState(false);
    const [isDuplicate, setIsDuplicate] = useState(false);

    useEffect(() => {
        emailRef.current.focus();
    }, []);

    useEffect(() => {
        return () => {
            checkController.current?.abort();
            if (debounceTimerRef.current) {
                clearTimeout(debounceTimerRef.current);
            }
        };
    }, []);

    const checkEmailDuplicate = async (emailValue) => {
        const controller = new AbortController();
        checkController.current = controller;
        try {
            setIsChecking(true);
            const response = await apiClient.get('/auth/email/check', { params: { email: emailValue }, signal: controller.signal });
            if (controller.signal.aborted) return;
            const { isDuplicate, message } = response.data;

            if (isDuplicate) {
                setError(message || '이미 사용 중인 이메일입니다.');
                setIsEmailValid(false);
                setIsDuplicate(true);
            } else {
                setError('');
                setIsEmailValid(true);
                setIsDuplicate(false);
            }
        } catch {
            if (controller.signal.aborted) return;

            setError('이메일 중복 확인에 실패했습니다.');
            setIsEmailValid(false);
            setIsDuplicate(false);
        } finally {
            if (!controller.signal.aborted) setIsChecking(false);
        }
    };

    const handleEmail = (e) => {
        checkController.current?.abort();
        const inputValue = e.target.value.trim();
        setIsDuplicate(false);
        setEmail(inputValue);
        setIsEmailValid(false); // 이메일 변경 시 유효성 초기화

        if (debounceTimerRef.current) {
            clearTimeout(debounceTimerRef.current);
        }

        if (!inputValue) {
            setError('');
            setIsChecking(false);
            return;
        }

        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(inputValue)) {
            setError('이메일이 올바르지 않습니다.');
            setIsChecking(false);
            return;
        }

        debounceTimerRef.current = setTimeout(() => {
            checkEmailDuplicate(inputValue);
        }, 500); // 0.5초 대기
    };

    const handleSubmit = () => {
        if (!email) {
            setError('이메일을 입력해주세요.');
            return;
        }

        if (error || !isEmailValid) {
            return;
        }

        if (onSuccess) {
            onSuccess(email);
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.infoBox}>
                <span className={styles.label}>Step 1:</span> 유효한 이메일을 입력해주세요.
            </div>
            
            <div className={styles.formGroup}>
                <label htmlFor='email'>이메일 주소</label>
                <input
                    ref={emailRef}
                    id='email'
                    className={error ? styles.invalidInput : ''}
                    type='email'
                    name='email'
                    autoComplete='email'
                    aria-invalid={!!error}
                    aria-describedby={error ? 'signup-email-error' : undefined}
                    placeholder='example@email.com'
                    value={email}
                    onChange={handleEmail}
                />
                {error && isDuplicate ? (
                    <Link id="signup-email-error" to="/login" className={styles.errorMessageLink}>
                        {error.split('\n').map((line, index) => (
                            <span key={index}>
                                {line}
                                {index < error.split('\n').length - 1 && <br />}
                            </span>
                        ))}
                    </Link>
                ) : error ? (
                    <p id="signup-email-error" role="alert" className={styles.errorMessage}>{error}</p>
                ) : null}
            </div>
            
            <button
                type='button'
                className={styles.submitButton}
                onClick={handleSubmit}
                disabled={!email || !!error || isChecking || !isEmailValid}
            >
                {isChecking ? '확인 중...' : '인증 코드 받기'}
            </button>
        </div>
    );
};

export default EmailInput;