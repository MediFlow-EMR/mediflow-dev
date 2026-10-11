import ThemeToggle from '../common/ThemeToggle';
import { useNavigate } from 'react-router-dom';
import styles from './Header.module.scss';

const Header = () => {
    const navigate = useNavigate();

    const handleLoginClick = () => {
        navigate('/auth/signin');
    };

    return (
        <header className={styles.header}>
            <div className={styles.container}>
                <div className={styles.logo}>
                    <span className={styles.logoText}>MediFlow</span>
                </div>
                <nav className={styles.nav}>
                <ThemeToggle />
                    <button 
                        className={styles.loginButton}
                        onClick={handleLoginClick}
                    >
                        로그인
                    </button>
                </nav>
            </div>
        </header>
    );
};

export default Header;
