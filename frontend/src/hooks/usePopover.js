import { useCallback, useLayoutEffect, useRef, useState } from 'react';

export default function usePopover({ width = 280 } = {}) {
    const triggerRef = useRef(null);
    const panelRef = useRef(null);
    const [open, setOpen] = useState(false);
    const [position, setPosition] = useState({ position: 'fixed', visibility: 'hidden' });
    const close = useCallback((restoreFocus = false) => {
        setOpen(false);
        if (restoreFocus) triggerRef.current?.focus({ preventScroll: true });
    }, []);
    useLayoutEffect(() => {
        if (!open) return;
        const place = () => {
            const rect = triggerRef.current?.getBoundingClientRect();
            if (!rect) return;
            const panelWidth = Math.min(Math.max(width, rect.width), window.innerWidth - 24);
            const panelHeight = panelRef.current?.scrollHeight || 300;
            const below = window.innerHeight - rect.bottom - 12;
            const above = rect.top - 12;
            const flip = below < panelHeight && above > below;
            setPosition({ position: 'fixed', width: panelWidth,
                left: Math.max(12, Math.min(rect.left, window.innerWidth - panelWidth - 12)),
                top: flip ? undefined : rect.bottom + 6,
                bottom: flip ? window.innerHeight - rect.top + 6 : undefined,
                maxHeight: Math.max(100, flip ? above - 6 : below - 6) });
        };
        const outside = event => {
            if (!triggerRef.current?.contains(event.target) && !panelRef.current?.contains(event.target)) close();
        };
        const key = event => { if (event.key === 'Escape') { event.preventDefault(); close(true); } };
        place();
        const observer = new ResizeObserver(place);
        if (panelRef.current) observer.observe(panelRef.current);
        document.addEventListener('pointerdown', outside);
        document.addEventListener('focusin', outside);
        document.addEventListener('keydown', key);
        window.addEventListener('resize', place);
        window.addEventListener('scroll', place, true);
        return () => {
            observer.disconnect();
            document.removeEventListener('pointerdown', outside);
            document.removeEventListener('focusin', outside);
            document.removeEventListener('keydown', key);
            window.removeEventListener('resize', place);
            window.removeEventListener('scroll', place, true);
        };
    }, [open, close, width]);
    return { triggerRef, panelRef, open, setOpen, close, position };
}
