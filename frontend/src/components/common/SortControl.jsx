import CustomSelect from './CustomSelect';
import styles from './SortControl.module.scss';
const options = [
    { value: 'desc', label: '최신순' },
    { value: 'asc', label: '오래된순' },
];
export default function SortControl({ value, onChange, label = '날짜 정렬', basis }) {
    return <div className={styles.sort}><span>{basis || '날짜'}</span><CustomSelect label={label} value={value} onChange={onChange} options={options} /></div>;
}
