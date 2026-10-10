import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import styles from './SignUpForm.module.scss';
import EmailInput from './EmailInput.jsx';
import VerificationInput from './VerificationInput.jsx';
import PasswordInput from './PasswordInput.jsx';
import SuccessModal from '../common/SuccessModal.jsx';
import { EmailAuthService } from '../../services/authService.js';

const SignUpForm = () => {
    const navigate = useNavigate();

    // 현재 어떤 스텝인지 확인
    const [step, setStep] = useState(1);
    // 이메일 상태 관리
    const [email, setEmail] = useState('');
    // 비밀번호 상태 관리
    const [password, setPassword] = useState('');
    const [isPasswordValid, setIsPasswordValid] = useState(false);
    const [passwordStrength, setPasswordStrength] = useState(0);
    // 로딩 상태 관리
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    // 성공 모달 상태 관리
    const [showSuccessModal, setShowSuccessModal] = useState(false);

    // 이메일 중복확인이 끝날때 호출될 함수
    const emailSuccessHandler = (verifiedEmail) => {
        setEmail(verifiedEmail);
        setStep(2);
    };

    // 인증 코드 검증이 끝날때 호출될 함수
    const verificationSuccessHandler = () => {
        setStep(3);
    };

    // 비밀번호 변경 핸들러
    const passwordChangeHandler = (newPassword, isValid, strength) => {
        setPassword(newPassword);
        setIsPasswordValid(isValid);
        setPasswordStrength(strength);
    };

    // 회원가입 완료 버튼 클릭 핸들러
    const handleSignUpComplete = async () => {
        setIsLoading(true);
        setError('');

        try {
            console.log('회원가입 요청:', { email, password: '***' });
            const response = await EmailAuthService.signup(email, password);
            console.log('회원가입 성공:', response.data);

            // 성공 모달 표시
            setShowSuccessModal(true);
        } catch (error) {
            console.error('회원가입 실패:', error);
            console.error('에러 응답:', error.response);
            console.error('에러 데이터:', error.response?.data);
            console.error('에러 메시지:', error.response?.data?.message);

            const errorMessage = error.response?.data?.message
                || error.response?.data?.error
                || '회원가입 중 오류가 발생했습니다.';
            setError(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    // 성공 모달이 닫힐 때 메인 페이지로 이동
    const handleModalClose = () => {
        setShowSuccessModal(false);
        navigate('/');
    };

    return (
        <div className={styles.signupForm}>
            {showSuccessModal && (
                <SuccessModal
                    message="회원가입이 완료되었습니다!"
                    onClose={handleModalClose}
                />
            )}

            <div className={styles.formStepActive}>
                {step === 1 && <EmailInput onSuccess={emailSuccessHandler} />}
                {step === 2 && <VerificationInput email={email} onSuccess={verificationSuccessHandler} />}
                {step === 3 && (
                    <>
                        <PasswordInput onPasswordChange={passwordChangeHandler} />
                        {/* 에러 메시지 표시 */}
                        {error && <p className={styles.errorMessage}>{error}</p>}
                        {/* 비밀번호 강도가 3 이상(보통 이상)일 때만 회원가입 버튼 표시 */}
                        {passwordStrength >= 3 && (
                            <button
                                className={styles.signUpCompleteBtn}
                                onClick={handleSignUpComplete}
                                disabled={isLoading || !isPasswordValid}
                            >
                                {isLoading ? '처리 중...' : '회원가입 완료'}
                            </button>
                        )}
                    </>
                )}
            </div>

            <div className={styles.backLinkContainer}>
                <Link
                    to='/'
                    className={styles.backLink}
                >
                    ← 뒤로 가기
                </Link>
            </div>
        </div>
    );
};

export default SignUpForm;
