import { useEffect, useRef, useState } from 'react';
import styles from './SignUpForm.module.scss';

const PasswordInput = ({ onPasswordChange }) => {
    const passwordRef = useRef();

    const [password, setPassword] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [passwordStrength, setPasswordStrength] = useState(0);

    // 패스워드 패턴 검증
    const validatePassword = (password) => {
        const passwordPattern =
            /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,72}$/;
        return passwordPattern.test(password);
    };

    // 비밀번호 강도 계산
    const calculatePasswordStrength = (password) => {
        let strength = 0;
        if (password.length >= 8) strength++;
        if (password.length >= 12) strength++;
        if (/[A-Z]/.test(password) && /[a-z]/.test(password)) strength++;
        if (/\d/.test(password)) strength++;
        if (/[@$!%*#?&]/.test(password)) strength++;
        return Math.min(strength, 4);
    };

    const changeHandler = (e) => {
        const newPassword = e.target.value;
        setPassword(newPassword);

        // 비밀번호가 비어있으면 에러 메시지를 표시하지 않음
        if (newPassword.length === 0) {
            setErrorMessage('');
            setPasswordStrength(0);
            if (onPasswordChange) {
                onPasswordChange(newPassword, false, 0);
            }
            return;
        }

        // 비밀번호 유효성 검증
        const isValid = validatePassword(newPassword);

        // 비밀번호 강도 계산
        let strength = calculatePasswordStrength(newPassword);

        // 유효하지 않은 비밀번호는 강도를 최대 2(약함)로 제한
        if (!isValid && strength > 2) {
            strength = 2;
        }

        setPasswordStrength(strength);

        if (isValid) {
            setErrorMessage('');
            // 부모 컴포넌트에 유효한 비밀번호와 강도 전달
            if (onPasswordChange) {
                onPasswordChange(newPassword, true, strength);
            }
        } else {
            setErrorMessage(
                '비밀번호는 8~72자이며, 숫자, 문자, 특수문자를 모두 포함해야 합니다.'
            );
            // 부모 컴포넌트에 유효하지 않은 비밀번호와 강도 전달
            if (onPasswordChange) {
                onPasswordChange(newPassword, false, strength);
            }
        }
    };

    // 비밀번호 강도 레벨 텍스트
    const getStrengthText = () => {
        if (!password) return '';
        switch (passwordStrength) {
            case 1:
            case 2:
                return '약함';
            case 3:
                return '보통';
            case 4:
                return '강함';
            default:
                return '매우 약함';
        }
    };

    // 비밀번호 강도 색상
    const getStrengthColor = () => {
        switch (passwordStrength) {
            case 1:
            case 2:
                return '#ef4444';
            case 3:
                return '#f59e0b';
            case 4:
                return '#10b981';
            default:
                return '#9ca3af';
        }
    };

    useEffect(() => {
        passwordRef.current.focus();
    }, []);

    return (
        <>
            <p className={styles.infoText}>Step 3: 안전한 비밀번호를 설정해주세요.</p>
            <div className={styles.passwordInputWrapper}>
                <div className={styles.passwordFieldContainer}>
                    <input
                        ref={passwordRef}
                        maxLength={72}
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={changeHandler}
                        className={`${styles.passwordInput} ${errorMessage ? styles.invalidInput : ''}`}
                        placeholder='비밀번호를 입력하세요'
                        autoComplete='new-password'
                        aria-label='새 비밀번호'
                        aria-invalid={!!errorMessage}
                    />
                    <button
                        type='button'
                        className={styles.togglePasswordBtn}
                        onClick={() => setShowPassword(!showPassword)}
                        aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
                        aria-pressed={showPassword}
                    >
                        {showPassword ? (
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88" />
                            </svg>
                        ) : (
                            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z" />
                                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                            </svg>
                        )}
                    </button>
                </div>

                {/* 비밀번호 강도 표시 */}
                {password && (
                    <div className={styles.passwordStrengthContainer}>
                        <div className={styles.strengthBars}>
                            {[1, 2, 3, 4].map((level) => (
                                <div
                                    key={level}
                                    className={`${styles.strengthBar} ${
                                        level <= passwordStrength ? styles.active : ''
                                    }`}
                                    style={{
                                        backgroundColor: level <= passwordStrength ? getStrengthColor() : '#e5e7eb'
                                    }}
                                />
                            ))}
                        </div>
                        <span className={styles.strengthText} style={{ color: getStrengthColor() }}>
                            {getStrengthText()}
                        </span>
                    </div>
                )}
            </div>
            {errorMessage && <p className={styles.errorMessage}>{errorMessage}</p>}
        </>
    );
};

export default PasswordInput;

