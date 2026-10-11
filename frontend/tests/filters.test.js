import { test } from 'node:test';
import assert from 'node:assert/strict';
import { filterPatients, filterNotes, filterHandovers, patientFilterDefaults, noteFilterDefaults, handoverFilterDefaults, recordDay } from '../src/utils/filters.js';

test('patient filters intersect and null data does not crash search', () => {
    const patients = [
        {patientId:1,name:'김가상',chartNumber:'DEMO-001',diagnosis:'폐렴',roomNumber:301,allergies:'페니실린',fallRisk:true,isolationType:'접촉'},
        {patientId:2,name:'박가상',roomNumber:302,allergies:'없음'},
        {patientId:3},
    ];
    assert.equal(filterPatients(patients, patientFilterDefaults).length, 3);
    assert.deepEqual(filterPatients(patients, {...patientFilterDefaults,search:' demo-001 ',room:'301',allergy:true,fallRisk:true,isolation:true}).map(p=>p.patientId), [1]);
    assert.equal(filterPatients(patients, {...patientFilterDefaults,search:'폐렴',room:'302'}).length, 0);
});
test('record dates use Korean day with inclusive boundaries', () => {
    assert.equal(recordDay('2026-10-10T15:00:00Z'), '2026-10-11');
    assert.equal(recordDay('2026-10-11T00:00:00'), '2026-10-11');
    assert.equal(recordDay('invalid'), '');
    const notes = [{id:1,createdAt:'2026-10-10T15:00:00Z',plainText:'관찰 내용',category:'OBSERVATION',nurseName:'가상간호사',isImportant:true}, {id:2,createdAt:'2026-10-10T14:59:59Z',content:'<p>관찰 내용</p>'}];
    assert.deepEqual(filterNotes(notes, {...noteFilterDefaults,from:'2026-10-11',to:'2026-10-11',category:'OBSERVATION',author:'가상간호사',important:true,search:'관찰'}).map(n=>n.id), [1]);
    assert.equal(filterNotes(notes, {...noteFilterDefaults,from:'2026-10-12',to:'2026-10-11'}).length, 0);
});
test('handover search, shift, source, date and status work together', () => {
    const records=[{id:1,finalSummary:'수기 내용',createdByName:'김간호사',handoverDate:'2026-10-11',fromShiftType:'DAY',sourceType:'MANUAL',status:'DRAFT'}, {id:2,finalSummary:'AI 내용',createdByName:'이간호사',handoverDate:'2026-10-10',fromShiftType:'NIGHT',sourceType:'AI',status:'APPROVED'}];
    assert.deepEqual(filterHandovers(records,{...handoverFilterDefaults,search:'김간호사',from:'2026-10-11',to:'2026-10-11',shift:'DAY',source:'MANUAL',status:'DRAFT'}).map(r=>r.id), [1]);
    assert.equal(filterHandovers(records,{...handoverFilterDefaults,source:'AI',status:'DRAFT'}).length,0);
});

test('multiple selections use OR within a field and AND across fields', () => {
    const notes = [
        {id:1,category:'OBSERVATION',nurseName:'김'},
        {id:2,category:'CARE',nurseName:'이'},
        {id:3,category:'EDUCATION',nurseName:'김'},
        {id:4,category:'CARE',nurseName:'박'},
    ];
    assert.deepEqual(filterNotes(notes, {...noteFilterDefaults,category:['OBSERVATION','CARE'],author:['김','이']}).map(r=>r.id), [1,2]);
    assert.equal(filterNotes(notes, {...noteFilterDefaults,category:[],author:[]}).length, 4);
    const handovers = [
        {id:1,fromShiftType:'DAY',sourceType:'AI',status:'DRAFT'},
        {id:2,fromShiftType:'NIGHT',sourceType:'MANUAL',status:'APPROVED'},
        {id:3,fromShiftType:'EVENING',sourceType:'AI',status:'DRAFT'},
    ];
    assert.deepEqual(filterHandovers(handovers, {...handoverFilterDefaults,shift:['DAY','NIGHT'],source:['AI','MANUAL'],status:['DRAFT','APPROVED']}).map(r=>r.id), [1,2]);
    assert.deepEqual(filterPatients([{id:1,roomNumber:301},{id:2,roomNumber:302},{id:3,roomNumber:303}], {...patientFilterDefaults,room:['301','302']}).map(r=>r.id), [1,2]);
});
