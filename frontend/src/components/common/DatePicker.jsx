import { currentRecordDateTime } from '../../utils/dateFormat';
import { useEffect, useId, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import usePopover from '../../hooks/usePopover';
import { calendarDate, dateValue, shiftDay, shiftMonth } from '../../utils/calendar';
import styles from './DatePicker.module.scss';

export default function DatePicker({ id, label = '날짜 선택', selectedDate, onDateChange, min, max, disabled = false, clearable = false, placeholder = '날짜 선택' }) {
    const selected = calendarDate(selectedDate);
    const today = calendarDate(currentRecordDateTime());
    const [month, setMonth] = useState(selected || today);
    const [focused, setFocused] = useState(dateValue(selected || today));
    const dialogId = useId();
    const dayRefs = useRef({});
    const { triggerRef, panelRef, open, setOpen, close, position } = usePopover({ width: 294 });
    const allowed = date => (!min || dateValue(date) >= dateValue(calendarDate(min))) && (!max || dateValue(date) <= dateValue(calendarDate(max)));
    const initial = () => {
        let date = selected || today;
        if (min && dateValue(date) < dateValue(calendarDate(min))) date = calendarDate(min);
        if (max && dateValue(date) > dateValue(calendarDate(max))) date = calendarDate(max);
        setMonth(date); setFocused(dateValue(date)); setOpen(true);
    };
    useEffect(() => { if (open) dayRefs.current[focused]?.focus({ preventScroll: true }); }, [focused, open]);
    const select = date => { if (allowed(date)) { onDateChange(date); close(true); } };
    const keyboard = (event, date) => {
        const changes = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 };
        let next;
        if (event.key in changes) next = shiftDay(date, changes[event.key]);
        else if (event.key === 'Home') next = shiftDay(date, -date.getDay());
        else if (event.key === 'End') next = shiftDay(date, 6 - date.getDay());
        else if (event.key === 'PageUp') next = shiftMonth(date, -1);
        else if (event.key === 'PageDown') next = shiftMonth(date, 1);
        if (next) { event.preventDefault(); if (allowed(next)) { setMonth(next); setFocused(dateValue(next)); } }
    };
    const count = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
    const first = new Date(month.getFullYear(), month.getMonth(), 1).getDay();
    const changeMonth = delta => { const next = shiftMonth(month, delta); setMonth(next); };
    return <div className={styles.datePicker}>
        <button ref={triggerRef} type="button" id={id} aria-label={`${label}${selected ? `: ${dateValue(selected)}` : ''}`}
            aria-haspopup="dialog" aria-expanded={open} aria-controls={open ? dialogId : undefined} disabled={disabled}
            className={styles.dateButton} onClick={() => open ? close() : initial()}>
            <span>{selected ? dateValue(selected).replaceAll('-', '.') : placeholder}</span><span aria-hidden="true">▦</span>
        </button>
        {open && createPortal(<div ref={panelRef} id={dialogId} role="dialog" aria-label={`${label} 달력`} className={styles.dropdown} style={position}>
            <div className={styles.header}>
                <button type="button" aria-label="이전 달" onClick={() => changeMonth(-1)}>‹</button>
                <strong aria-live="polite">{month.getFullYear()}.{String(month.getMonth() + 1).padStart(2, '0')}</strong>
                <button type="button" aria-label="다음 달" onClick={() => changeMonth(1)}>›</button>
            </div>
            <div className={styles.weekdays} aria-hidden="true">{['일','월','화','수','목','금','토'].map(day => <span key={day}>{day}</span>)}</div>
            <div className={styles.days}>
                {Array.from({ length: first }, (_, index) => <span key={`empty-${index}`} />)}
                {Array.from({ length: count }, (_, index) => {
                    const date = new Date(month.getFullYear(), month.getMonth(), index + 1);
                    const value = dateValue(date);
                    return <button key={value} ref={node => { dayRefs.current[value] = node; }} type="button" aria-label={value}
                        aria-current={value === dateValue(today) ? 'date' : undefined} aria-pressed={value === dateValue(selected)}
                        tabIndex={value === focused || (!focused.startsWith(value.slice(0, 7)) && index === 0) ? 0 : -1}
                        disabled={!allowed(date)} onKeyDown={event => keyboard(event, date)} onClick={() => select(date)}
                        className={`${styles.day} ${value === dateValue(today) ? styles.today : ''} ${value === dateValue(selected) ? styles.selected : ''}`}>
                        {index + 1}
                    </button>;
                })}
            </div>
            <div className={styles.footer}>
                <button type="button" disabled={!allowed(today)} onClick={() => select(today)}>오늘</button>
                {clearable && <button type="button" onClick={() => { onDateChange(null); close(true); }}>날짜 해제</button>}
                <button type="button" onClick={() => close(true)}>닫기</button>
            </div>
        </div>, document.body)}
    </div>;
}
