import { useRef, useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import styles from './EmailInput.module.scss';

const EmailInput = () => {
    const emailRef = useRef();
    const debounceTimerRef = useRef(null);

    // 에러 상태메시지를 관리
    const [error, setError] = useState('');
    const [email, setEmail] = useState('');
    const [isChecking, setIsChecking] = useState(false);

    // 화면이 렌더링되자마자 입력창에 포커싱
    useEffect(() => {
        emailRef.current.focus();
    }, []);

    // 컴포넌트 언마운트 시 타이머 정리
    useEffect(() => {
        return () => {
            if (debounceTimerRef.current) {
                clearTimeout(debounceTimerRef.current);
            }
        };
    }, []);

    // 이메일 중복 확인 함수
    const checkEmailDuplicate = async (emailValue) => {
        try {
            setIsChecking(true);
            const response = await apiClient.get(`/auth/check-email?email=${emailValue}`);
            const { isDuplicate, message } = response.data;

            if (isDuplicate) {
                setError(message || '이미 사용 중인 이메일입니다.');
            } else {
                setError('');
            }
        } catch (err) {
            console.error('이메일 중복 확인 실패:', err);
            setError('이메일 중복 확인에 실패했습니다.');
        } finally {
            setIsChecking(false);
        }
    };

    // 이메일 입력 이벤트
    const handleEmail = (e) => {
        const inputValue = e.target.value;
        setEmail(inputValue);

        // 기존 타이머 취소
        if (debounceTimerRef.current) {
            clearTimeout(debounceTimerRef.current);
        }

        // 입력값이 없으면 에러 메시지 초기화
        if (!inputValue) {
            setError('');
            setIsChecking(false);
            return;
        }

        // 이메일 패턴 검증
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(inputValue)) {
            setError('이메일이 올바르지 않습니다.');
            setIsChecking(false);
            return;
        }

        // 패턴이 올바르면 중복 확인 (디바운싱 적용)
        debounceTimerRef.current = setTimeout(() => {
            checkEmailDuplicate(inputValue);
        }, 500); // 0.5초 대기
    };

    // 인증 코드 요청 핸들러
    const handleSubmit = () => {
        if (!email) {
            setError('이메일을 입력해주세요.');
            return;
        }

        if (error) {
            return;
        }

        // TODO: 인증 코드 요청 API 호출
        console.log('인증 코드 요청:', email);
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
                    placeholder='example@email.com'
                    value={email}
                    onChange={handleEmail}
                />
                {error && <p className={styles.errorMessage}>{error}</p>}
            </div>
            
            <button
                type='button'
                className={styles.submitButton}
                onClick={handleSubmit}
                disabled={!email || !!error || isChecking}
            >
                {isChecking ? '확인 중...' : '인증 코드 받기'}
            </button>
        </div>
    );
};

export default EmailInput;