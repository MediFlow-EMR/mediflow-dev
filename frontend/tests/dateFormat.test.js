import { test } from 'node:test';
import assert from 'node:assert/strict';
import { formatDate, formatDateTime } from '../src/utils/dateFormat.js';

test('date-only values and calendar selections keep their original day', () => {
  assert.equal(formatDate('2026-10-11'), '2026.10.11');
  assert.equal(formatDate(new Date(2026, 0, 2)), '2026.01.02');
});

test('record timestamps use padded Korean dates and 24-hour time', () => {
  assert.equal(formatDateTime('2026-10-11T04:00:00Z'), '2026.10.11 13:00');
  assert.equal(formatDateTime('2026-10-10T15:00:00Z'), '2026.10.11 00:00');
});

test('missing and invalid dates do not render Invalid Date', () => {
  for (const value of [null, undefined, '', 'not-a-date']) {
    assert.equal(formatDate(value), '-');
    assert.equal(formatDateTime(value), '-');
  }
});
