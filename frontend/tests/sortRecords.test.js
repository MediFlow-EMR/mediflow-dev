import { test } from 'node:test';
import assert from 'node:assert/strict';
import { sortRecords, recordTimestamp } from '../src/utils/sortRecords.js';
import { calendarDate, dateValue, shiftMonth, shiftDay } from '../src/utils/calendar.js';

test('default is newest first; sorting is stable and does not mutate', () => {
    const records = [{id:1,at:'2026-10-11T10:00:00'}, {id:2,at:'2026-10-10T10:00:00'}, {id:3,at:'2026-10-11T10:00:00'}, {id:4,at:null}, {id:5,at:'bad'}];
    assert.deepEqual(sortRecords(records, 'at').map(r => r.id), [1,3,2,4,5]);
    assert.deepEqual(sortRecords(records, 'at', 'asc').map(r => r.id), [2,1,3,4,5]);
    assert.deepEqual(sortRecords(records, 'at', 'desc').map(r => r.id), [1,3,2,4,5]);
    assert.deepEqual(records.map(r => r.id), [1,2,3,4,5]);
});
test('local clinical timestamps and offset timestamps refer to the same Korean instant', () => {
    assert.equal(recordTimestamp('2026-10-11T00:00:00'), recordTimestamp('2026-10-10T15:00:00Z'));
    assert.equal(recordTimestamp('invalid'), null);
    const records = [{id:1,day:null,created:'2026-10-12'}, {id:2,day:'2026-10-11'}];
    assert.deepEqual(sortRecords(records, ['day','created'], 'desc').map(r=>r.id), [1,2]);
});
test('calendar rejects invalid dates and clamps month movement across leap years', () => {
    assert.equal(calendarDate('2026-02-29'), null);
    assert.equal(dateValue(shiftMonth(calendarDate('2024-01-31'), 1)), '2024-02-29');
    assert.equal(dateValue(shiftMonth(calendarDate('2026-03-31'), -1)), '2026-02-28');
    assert.equal(dateValue(shiftDay(calendarDate('2026-12-31'), 1)), '2027-01-01');
});

test('handovers on the same day sort by creation time', () => {
    const records = [{id:1,day:'2026-10-11',created:'2026-10-11T09:00:00'}, {id:2,day:'2026-10-11',created:'2026-10-11T10:00:00'}];
    assert.deepEqual(sortRecords(records, 'day', 'desc', 'created').map(r=>r.id), [2,1]);
    assert.deepEqual(sortRecords(records, 'day', 'asc', 'created').map(r=>r.id), [1,2]);
});
