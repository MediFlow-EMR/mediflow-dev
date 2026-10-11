import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parsePatientSections, replacePatientSection } from '../src/utils/handoverDraft.js';

test('editing one patient preserves other patients and typed whitespace', () => {
    const original = '**[가상환자 A]**\n첫 내용\n\n**[가상환자 B]**\n둘째 내용';
    const edited = replacePatientSection(original, '가상환자 A', '내용 끝 공백 \n새 줄 ');
    assert.ok(edited.includes('내용 끝 공백 \n새 줄 '));
    assert.equal(parsePatientSections(edited)[1].body, '둘째 내용');
    assert.equal(replacePatientSection(edited, '가상환자 A', '내용 끝 공백 \n새 줄 '), edited);
    assert.equal(replacePatientSection(original, '없는 환자', '변경'), original);
});
test('freeform manual content is not interpreted as patient cards', () => {
    assert.deepEqual(parsePatientSections('수기로 작성한 인수인계 내용'), []);
});
