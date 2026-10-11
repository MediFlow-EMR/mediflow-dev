import { useEffect, useState } from 'react';
import { FiMoon, FiSun } from 'react-icons/fi';
import { applyTheme, readTheme, saveTheme } from '../../utils/theme';
import styles from './ThemeToggle.module.scss';

export default function ThemeToggle() {
    const [theme, setTheme] = useState(readTheme);
    useEffect(() => {
        const sync = () => setTheme(applyTheme(readTheme()));
        window.addEventListener('storage', sync);
        window.addEventListener('mediflow-theme-change', sync);
        return () => {
            window.removeEventListener('storage', sync);
            window.removeEventListener('mediflow-theme-change', sync);
        };
    }, []);
    const dark = theme === 'dark';
    return (
        <button className={styles.toggle} type="button" aria-label="다크모드" aria-pressed={dark}
            title={dark ? '라이트모드로 전환' : '다크모드로 전환'}
            onClick={() => saveTheme(dark ? 'light' : 'dark')}>
            {dark ? <FiSun aria-hidden="true" /> : <FiMoon aria-hidden="true" />}
            <span>{dark ? '라이트모드' : '다크모드'}</span>
        </button>
    );
}
