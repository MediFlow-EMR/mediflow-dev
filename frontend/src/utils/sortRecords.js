export function recordTimestamp(value) {
    if (!value) return null;
    // API LocalDateTime values belong to the hospital's Asia/Seoul clock.
    const text = typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value) && !/(?:Z|[+-]\d{2}:\d{2})$/.test(value)
        ? `${value}+09:00` : value;
    const timestamp = new Date(text).getTime();
    return Number.isFinite(timestamp) ? timestamp : null;
}
export function sortRecords(records, key, direction = 'desc', tieKey) {
    const keys = Array.isArray(key) ? key : [key];
    const primary = record => keys.reduce((result, field) => result ?? recordTimestamp(record[field]), null);
    return records.map((record, index) => ({ record, index, time: primary(record), tie: tieKey ? recordTimestamp(record[tieKey]) : null }))
        .sort((a, b) => {
            if (a.time === null) return b.time === null ? a.index - b.index : 1;
            if (b.time === null) return -1;
            const order = direction === 'asc' ? 1 : -1;
            const difference = (a.time - b.time) * order;
            if (difference) return difference;
            if (a.tie !== null && b.tie !== null) return (a.tie - b.tie) * order || a.index - b.index;
            return a.index - b.index;
        }).map(({ record }) => record);
}
