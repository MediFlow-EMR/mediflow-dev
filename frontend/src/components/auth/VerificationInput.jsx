import { useRef, useEffect, useState } from 'react';
import styles from './SignUpForm.module.scss';
import { EmailAuthService } from '../../services/authService';

const VerificationInput = ({ email, onSuccess }) => {
    const [codes, setCodes] = useState(['', '', '', '']);
    const [error, setError] = useState('');
    const [remainingSeconds, setRemainingSeconds] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [verifying, setVerifying] = useState(false);
    const inputRefs = useRef([]);
    const successRef = useRef(onSuccess);
    const expired = remainingSeconds <= 0;
    useEffect(() => { successRef.current = onSuccess; }, [onSuccess]);

    useEffect(() => {
        let active = true;
        setIsLoading(true);
        setCodes(['', '', '', '']);
        setError('');
        setRemainingSeconds(0);
        EmailAuthService.getRemainingTime(email)
            .then(response => { if (active) setRemainingSeconds(Math.max(0, response.data.remainingSeconds || 0)); })
            .catch(() => { if (active) setError('인증 만료 시간을 확인할 수 없습니다. 이전 화면에서 다시 요청해주세요.'); })
            .finally(() => { if (active) setIsLoading(false); });
        return () => { active = false; };
    }, [email]);

    useEffect(() => {
        if (!isLoading) inputRefs.current[0]?.focus();
    }, [isLoading]);

    useEffect(() => {
        if (isLoading || remainingSeconds <= 0) return;
        const timer = setTimeout(() => setRemainingSeconds(seconds => Math.max(0, seconds - 1)), 1000);
        return () => clearTimeout(timer);
    }, [remainingSeconds, isLoading]);

    useEffect(() => {
        const code = codes.join('');
        let active = true;
        setVerifying(false);
        if (code.length !== 4 || expired) return;
        const timer = setTimeout(async () => {
            setVerifying(true);
            try {
                const response = await EmailAuthService.verifyCode(email, code);
                if (!active) return;
                if (response.data.isMatch) {
                    setError('');
                    successRef.current?.();
                } else {
                    const remaining = await EmailAuthService.getRemainingTime(email);
                    if (!active) return;
                    setRemainingSeconds(Math.max(0, remaining.data.remainingSeconds || 0));
                    setError('새 인증코드를 재발송했습니다. 이메일을 다시 확인해주세요.');
                    setCodes(['', '', '', '']);
                    inputRefs.current[0]?.focus();
                }
            } catch {
                if (active) setError('인증에 실패했습니다. 코드를 다시 입력해주세요.');
            } finally {
                if (active) setVerifying(false);
            }
        }, 350);
        return () => { active = false; clearTimeout(timer); };
        // Expiration is handled by the input disable state; timer ticks must not restart verification.
    }, [codes, email, expired]);

    const updateCodes = (index, digits) => {
        setError('');
        const next = [...codes];
        [...digits].slice(0, 4 - index).forEach((digit, offset) => { next[index + offset] = digit; });
        if (!digits) next[index] = '';
        setCodes(next);
        inputRefs.current[Math.min(3, index + digits.length)]?.focus();
    };
    const keyDown = (index, event) => {
        if (event.key === 'ArrowLeft' && index > 0) inputRefs.current[index - 1]?.focus();
        if (event.key === 'ArrowRight' && index < 3) inputRefs.current[index + 1]?.focus();
        if (event.key === 'Backspace' && !codes[index] && index > 0) {
            event.preventDefault();
            updateCodes(index - 1, '');
        }
    };
    const minutes = Math.floor(remainingSeconds / 60);
    const seconds = String(remainingSeconds % 60).padStart(2, '0');
    return <>
        <p className={styles.infoText}>Step 2: 이메일로 전송된 인증번호 4자리를 입력해주세요.</p>
        {isLoading ? <div className={styles.skeletonTimer} /> : remainingSeconds > 0 ?
            <p className={styles.timerText}>남은 시간: <span className={styles.timer}>{minutes}:{seconds}</span></p> :
            <p className={styles.expiredText}>인증 시간이 만료되었습니다. 이전 화면에서 다시 요청해주세요.</p>}
        {error && <p role="alert" className={styles.errorMessage}>{error}</p>}
        <div className={styles.codeInputContainer} aria-busy={verifying || isLoading}>
            {codes.map((code, index) => <input key={index} ref={input => { inputRefs.current[index] = input; }}
                type="text" inputMode="numeric" aria-label={`인증 코드 ${index + 1}번째 숫자`} pattern="[0-9]"
                className={styles.codeInput} maxLength={1} value={code} disabled={isLoading || remainingSeconds === 0 || verifying}
                onChange={event => updateCodes(index, event.target.value.replace(/\D/g, ''))}
                onPaste={event => { event.preventDefault(); updateCodes(index, event.clipboardData.getData('text').replace(/\D/g, '')); }}
                onKeyDown={event => keyDown(index, event)} />)}
        </div>
        {verifying && <p role="status" className={styles.timerText}>인증 확인 중…</p>}
    </>;
};
export default VerificationInput;
