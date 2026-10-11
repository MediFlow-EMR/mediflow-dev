import { useEffect, useId } from 'react';
import { useNavigationGuard } from '../stores/navigationGuard';

export default function useUnsavedRecord(dirty, onDirtyChange, busy = false) {
    const id = useId();
    const setDirty = useNavigationGuard(state => state.setDirty);
    useEffect(() => {
        setDirty(id, dirty, busy);
        onDirtyChange?.(dirty || busy);
        return () => { setDirty(id, false); onDirtyChange?.(false); };
    }, [id, dirty, busy, onDirtyChange, setDirty]);
    useEffect(() => {
        if (!dirty && !busy) return;
        const beforeUnload = event => { event.preventDefault(); event.returnValue = ''; };
        window.addEventListener('beforeunload', beforeUnload);
        return () => window.removeEventListener('beforeunload', beforeUnload);
    }, [dirty, busy]);
}
