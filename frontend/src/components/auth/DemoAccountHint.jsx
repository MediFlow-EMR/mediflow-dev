import styles from './DemoAccountHint.module.scss';

const DEMO_ACCOUNT = {
    email: 'nurseer1@mediflow.com',
    password: 'nurse123@',
};

export default function DemoAccountHint({ onUse }) {
    return (
        <aside className={styles.hint} aria-label="데모 계정 안내">
            <p className={styles.title}>회원가입 없이 데모 체험</p>
            <dl className={styles.credentials}>
                <div><dt>아이디</dt><dd>{DEMO_ACCOUNT.email}</dd></div>
                <div><dt>비밀번호</dt><dd>{DEMO_ACCOUNT.password}</dd></div>
            </dl>
            {onUse && <button type="button" onClick={() => onUse(DEMO_ACCOUNT)} className={styles.useButton}>데모 계정 입력</button>}
        </aside>
    );
}
