const normalized = value => String(value ?? '').toLocaleLowerCase().trim();
const includes = (values, query) => !normalized(query) || values.some(value => normalized(value).includes(normalized(query)));
const matchesSelection = (value, selection) => {
    const selected = Array.isArray(selection) ? selection : selection ? [selection] : [];
    return !selected.length || selected.some(item => String(item) === String(value ?? ''));
};
const dateFormatter = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit' });
export function recordDay(value) {
    if (!value) return '';
    if (/^\d{4}-\d{2}-\d{2}(?:$|T)/.test(value) && !/(?:Z|[+-]\d{2}:\d{2})$/.test(value)) return value.slice(0, 10);
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    const parts = Object.fromEntries(dateFormatter.formatToParts(date).map(part => [part.type, part.value]));
    return `${parts.year}-${parts.month}-${parts.day}`;
}
function inRange(value, from, to) {
    if (!from && !to) return true;
    const day = recordDay(value);
    return Boolean(day) && (!from || day >= from) && (!to || day <= to);
}
export const patientFilterDefaults = { search: '', room: [], allergy: false, fallRisk: false, isolation: false };
export function filterPatients(patients, filters) {
    return patients.filter(patient => includes([patient.name, patient.chartNumber, patient.diagnosis], filters.search)
        && matchesSelection(patient.roomNumber, filters.room)
        && (!filters.allergy || (patient.allergies && patient.allergies !== '없음'))
        && (!filters.fallRisk || patient.fallRisk)
        && (!filters.isolation || (patient.isolationType && patient.isolationType !== '없음')));
}
export const noteFilterDefaults = { search: '', from: '', to: '', category: [], author: [], important: false };
export function filterNotes(notes, filters) {
    return notes.filter(note => includes([note.plainText || String(note.content ?? '').replace(/<[^>]*>/g, ' ')], filters.search)
        && inRange(note.createdAt, filters.from, filters.to)
        && matchesSelection(note.category, filters.category)
        && matchesSelection(note.nurseName, filters.author)
        && (!filters.important || note.isImportant));
}
export const handoverFilterDefaults = { search: '', from: '', to: '', shift: [], source: [], status: [], author: [] };
export function filterHandovers(records, filters) {
    return records.filter(record => includes([record.finalSummary, record.createdByName], filters.search)
        && inRange(record.handoverDate, filters.from, filters.to)
        && matchesSelection(record.createdByName, filters.author)
        && matchesSelection(record.fromShiftType, filters.shift)
        && matchesSelection(record.sourceType || (record.aiModel ? 'AI' : 'MANUAL'), filters.source)
        && matchesSelection(record.status, filters.status));
}
