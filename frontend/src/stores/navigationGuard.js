import { create } from 'zustand';

export const useNavigationGuard = create((set, get) => ({
    owners: {},
    setDirty: (id, dirty, busy = false) => set(state => {
        const owners = { ...state.owners };
        if (dirty || busy) owners[id] = { busy }; else delete owners[id];
        return { owners };
    }),
    confirmation: null,
    reset: () => set({ owners: {}, confirmation: null }),
    closeConfirmation: () => set({ confirmation: null }),
    requestNavigation: action => {
        const owners = Object.values(get().owners);
        if (owners.some(owner => owner.busy)) {
            set({ confirmation: { blocked: true, action: () => {}, message: '저장 또는 생성 작업이 진행 중입니다. 완료 후 이동해주세요.' } });
        } else if (owners.length) {
            set({ confirmation: { action, message: '저장하지 않은 변경이 있습니다. 화면을 나가시겠습니까?' } });
        } else action();
    },
}));
