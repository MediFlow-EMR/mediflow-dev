import { useId } from 'react';
import CustomSelect from './CustomSelect';
import DatePicker from './DatePicker';
import SortControl from './SortControl';
import { dateValue } from '../../utils/calendar';
import styles from './FilterBar.module.scss';

export default function FilterBar({ label, values, onChange, onReset, searchPlaceholder, fields, count, total, sortValue, onSortChange, sortBasis }) {
    const id = useId();
    const set = (key, value) => onChange({ ...values, [key]: value });
    const active = Object.values(values).some(value => Array.isArray(value) ? value.length > 0 : Boolean(value));
    return <section className={styles.filterBar} aria-label={`${label} 필터`}>
        <div className={styles.searchRow}>
            <input type="search" aria-label={`${label} 검색`} placeholder={searchPlaceholder}
                value={values.search} onChange={event => set('search', event.target.value)} />
            <button className={styles.reset} type="button" onClick={() => { onReset(); onSortChange?.('desc'); }} disabled={!active && (!sortValue || sortValue === 'desc')}>초기화</button>
        </div>
        <div className={styles.tools}>
            <details>
                <summary>상세 필터{active ? ' · 적용 중' : ''}</summary>
                <div className={styles.fields}>
                    {fields.map(field => field.type === 'checkbox' ? <label key={field.key} className={styles.checkbox}>
                        <input type="checkbox" checked={values[field.key]} onChange={event => set(field.key, event.target.checked)} />{field.label}
                    </label> : <div key={field.key} className={styles.field}>
                        <label htmlFor={`${id}-${field.key}`}>{field.label}</label>
                        {field.type === 'select' ? <CustomSelect multiple label={field.label} id={`${id}-${field.key}`} value={values[field.key]}
                            onChange={value => set(field.key, value)} options={[{ value: '', label: '전체' }, ...field.options]} />
                        : field.type === 'date' ? <DatePicker label={field.label} id={`${id}-${field.key}`} selectedDate={values[field.key]}
                            clearable min={field.key === 'to' ? values.from : undefined} max={field.key === 'from' ? values.to : undefined}
                            onDateChange={date => set(field.key, dateValue(date))} />
                        : <input aria-label={field.label} id={`${id}-${field.key}`} value={values[field.key]} onChange={event => set(field.key, event.target.value)} />}
                    </div>)}
                </div>
            </details>
            {onSortChange && <SortControl value={sortValue} onChange={onSortChange} basis={sortBasis} />}
        </div>
        {values.from && values.to && values.from > values.to && <p role="alert">시작일은 종료일보다 늦을 수 없습니다.</p>}
        <p className={styles.result} role="status">{count} / {total}{label === '환자' ? '명' : '건'}</p>
    </section>;
}
