import { useEffect, useId, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import usePopover from '../../hooks/usePopover';
import styles from './CustomSelect.module.scss';

export default function CustomSelect({ id, label, value, options, onChange, disabled = false, multiple = false, className = '' }) {
    const listId = useId();
    const { triggerRef, panelRef, open, setOpen, close, position } = usePopover({ width: 180 });
    const [highlighted, setHighlighted] = useState(0);
    const typeAhead = useRef({ text: '', at: 0 });
    const selected = options.findIndex(option => String(option.value) === String(value));
    const selectedValues = multiple ? (Array.isArray(value) ? value : value ? [value] : []) : [];
    const isSelected = option => multiple
        ? option.value === '' ? selectedValues.length === 0 : selectedValues.some(item => String(item) === String(option.value))
        : String(option.value) === String(value);
    const selectedLabels = options.filter(option => option.value !== '' && isSelected(option)).map(option => option.label);
    const displayLabel = multiple ? selectedLabels.length ? selectedLabels.join(', ') : '전체' : options[selected]?.label || '선택';
    const choose = option => {
        if (!option || option.disabled) return;
        if (multiple) {
            onChange(option.value === '' ? [] : isSelected(option)
                ? selectedValues.filter(item => String(item) !== String(option.value))
                : [...selectedValues, option.value]);
        } else { onChange(option.value); close(true); }
    };
    useEffect(() => {
        if (!open) return;
        const panel = panelRef.current;
        const option = panel?.querySelector(`[data-index="${highlighted}"]`);
        if (!option) return;
        if (option.offsetTop < panel.scrollTop) panel.scrollTop = option.offsetTop;
        else if (option.offsetTop + option.offsetHeight > panel.scrollTop + panel.clientHeight) panel.scrollTop = option.offsetTop + option.offsetHeight - panel.clientHeight;
    }, [highlighted, open, panelRef]);
    const show = () => { setHighlighted(Math.max(0, selected)); setOpen(true); };
    const move = delta => {
        for (let offset = 1; offset <= options.length; offset++) {
            const index = (highlighted + delta * offset + options.length * 2) % options.length;
            if (!options[index].disabled) { setHighlighted(index); break; }
        }
    };
    const keyDown = event => {
        if (['ArrowDown', 'ArrowUp', 'Home', 'End', 'Enter', ' '].includes(event.key)) {
            event.preventDefault();
            if (!open) { show(); return; }
            if (event.key === 'ArrowDown') move(1);
            else if (event.key === 'ArrowUp') move(-1);
            else if (event.key === 'Home') setHighlighted(options.findIndex(option => !option.disabled));
            else if (event.key === 'End') setHighlighted(options.findLastIndex(option => !option.disabled));
            else choose(options[highlighted]);
        } else if (event.key === 'Tab') close();
        else if (event.key.length === 1) {
            const now = Date.now();
            typeAhead.current.text = (now - typeAhead.current.at < 700 ? typeAhead.current.text : '') + event.key.toLocaleLowerCase();
            typeAhead.current.at = now;
            const match = options.findIndex(option => !option.disabled && String(option.label).toLocaleLowerCase().startsWith(typeAhead.current.text));
            if (match >= 0) { setHighlighted(match); setOpen(true); }
        }
    };
    return <div className={`${styles.select} ${className}`}>
        <button ref={triggerRef} id={id} type="button" role="combobox" aria-label={label} aria-expanded={open}
            aria-controls={open ? listId : undefined} aria-haspopup="listbox"
            aria-activedescendant={open ? `${listId}-${highlighted}` : undefined} disabled={disabled}
            onClick={() => open ? close() : show()} onKeyDown={keyDown} className={styles.trigger}>
            <span title={displayLabel}>{displayLabel}</span>{multiple && selectedValues.length > 1 && <span className={styles.count}>{selectedValues.length}개</span>}<span className={styles.chevron} aria-hidden="true">⌄</span>
        </button>
        {open && createPortal(<div ref={panelRef} id={listId} role="listbox" aria-multiselectable={multiple || undefined} aria-label={label} style={position} className={styles.menu}>
            {multiple && <div className={styles.hint}>여러 항목 선택 · 전체로 해제</div>}
            {options.map((option, index) => <div key={option.value} id={`${listId}-${index}`} data-index={index}
                role="option" aria-selected={isSelected(option)} aria-disabled={option.disabled || undefined}
                className={`${styles.option} ${index === highlighted ? styles.highlighted : ''}`}
                onPointerMove={() => !option.disabled && setHighlighted(index)} onMouseDown={event => event.preventDefault()}
                onClick={() => choose(option)}>
                <span>{option.label}</span><span aria-hidden="true">{isSelected(option) ? '✓' : ''}</span>
            </div>)}
        </div>, document.body)}
    </div>;
}
