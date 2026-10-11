import useUnsavedRecord from '../../hooks/useUnsavedRecord';
import { sortRecords } from '../../utils/sortRecords';
import { parsePatientSections, replacePatientSection } from '../../utils/handoverDraft';
import { useState, useEffect, useCallback } from 'react';
import { HandoverService } from '../../services/handoverService';
import { useAuthStore } from '../../stores/authStore';
import useDashboardStore from '../../stores/useDashboardStore';
import SuccessModal from '../common/SuccessModal';
import ConfirmModal from '../common/ConfirmModal';
import FilterBar from '../common/FilterBar';
import { filterHandovers, handoverFilterDefaults } from '../../utils/filters';
import { formatDate, formatDateTime } from '../../utils/dateFormat';
import styles from './HandoverTab.module.scss';

const HandoverTab = ({ onDirtyChange }) => {
  const { user } = useAuthStore();
  const { myPatients, allPatients, departmentSummary, selectedDate } = useDashboardStore();
  const departmentId = departmentSummary?.departmentId || user?.department?.id;
  const [activeTab, setActiveTab] = useState('previous');
  const [sourceType, setSourceType] = useState('AI');
  const [aiSummary, setAiSummary] = useState('');
  const [editedSummary, setEditedSummary] = useState('');
  const [patientBodies, setPatientBodies] = useState({});
  const [baseline, setBaseline] = useState({ content: '', ai: '' });
  const [editingRecord, setEditingRecord] = useState(null);
  const [previousHandovers, setPreviousHandovers] = useState([]);
  const [sortOrder, setSortOrder] = useState('desc');
  const [filters, setFilters] = useState(handoverFilterDefaults);
  const [expandedHandoverId, setExpandedHandoverId] = useState(null);
  const [histories, setHistories] = useState({});
  const [historyId, setHistoryId] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [regeneratingPatientId, setRegeneratingPatientId] = useState(null);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');
  const [confirmation, setConfirmation] = useState(null);
  const dirty = editedSummary !== baseline.content || aiSummary !== baseline.ai;
  const busy = isGenerating || isSaving || regeneratingPatientId !== null;
  const approvedEdit = editingRecord?.status === 'APPROVED';
  const fromShiftId = editingRecord?.fromShiftId || departmentSummary?.shiftId;
  const toShiftId = editingRecord?.toShiftId || departmentSummary?.nextShiftId;
  const fromShiftType = editingRecord?.fromShiftType || departmentSummary?.shiftType?.toUpperCase();
  const toShiftType = editingRecord?.toShiftType || { DAY: 'EVENING', EVENING: 'NIGHT', NIGHT: 'DAY' }[fromShiftType];
  const patientSections = parsePatientSections(editedSummary);
  const filteredHandovers = sortRecords(filterHandovers(previousHandovers, filters), ['handoverDate', 'createdAt'], sortOrder, 'createdAt');

  useUnsavedRecord(dirty, onDirtyChange, busy);

  const loadPreviousHandovers = useCallback(async () => {
    if (!departmentId) return;
    setLoading(true);
    try {
      const response = await HandoverService.getHandoversByDepartment(departmentId);
      setPreviousHandovers(response.data || []);
    } catch (err) { setError(err.response?.data?.message || '인수인계 목록을 불러올 수 없습니다'); }
    finally { setLoading(false); }
  }, [departmentId]);
  useEffect(() => { if (activeTab === 'previous') loadPreviousHandovers(); }, [activeTab, loadPreviousHandovers]);

  const guardChanges = (action, message = '저장하지 않은 초안을 다른 내용으로 바꾸시겠습니까?') => {
    if (dirty) setConfirmation({ title: '초안 변경 확인', message, action });
    else action();
  };
  const clearEditor = source => {
    setEditingRecord(null); setSourceType(source); setAiSummary(''); setEditedSummary(''); setPatientBodies({});
    setBaseline({ content: '', ai: '' }); setError(null); setActiveTab(source === 'AI' ? 'generate' : 'manual');
  };
  const chooseEditor = source => {
    if (source === sourceType) { setActiveTab(source === 'AI' ? 'generate' : 'manual'); return; }
    guardChanges(() => clearEditor(source));
  };
  const openRecord = record => guardChanges(() => {
    const source = record.sourceType || (record.aiModel ? 'AI' : 'MANUAL');
    setPatientBodies({});
    setEditingRecord(record); setSourceType(source); setAiSummary(record.aiSummary || '');
    setEditedSummary(record.finalSummary || ''); setBaseline({ content: record.finalSummary || '', ai: record.aiSummary || '' });
    setError(null); setActiveTab(source === 'AI' ? 'generate' : 'manual');
  });

  const generate = async () => {
    if (!fromShiftId || !departmentId) { setError('부서와 근무조 정보를 불러온 뒤 다시 시도해주세요.'); return; }
    setIsGenerating(true); setError(null);
    try {
      const response = await HandoverService.generateAiSummary({ departmentId, fromShiftId });
      setPatientBodies({}); setAiSummary(response.data); setEditedSummary(response.data);
    } catch (err) { setError(err.response?.data?.message || 'AI 초안 생성에 실패했습니다'); }
    finally { setIsGenerating(false); }
  };
  const save = async status => {
    if (!fromShiftId || !toShiftId || !departmentId) { setError('부서와 근무조 정보를 불러온 뒤 다시 시도해주세요.'); return; }
    if (!editedSummary.trim()) { setError('인수인계 내용을 입력해주세요.'); return; }
    setIsSaving(true); setError(null);
    try {
      const response = await HandoverService.saveHandover({
        departmentId: editingRecord?.departmentId || departmentId, fromShiftId, toShiftId,
        aiDraft: sourceType === 'AI' ? aiSummary : null, finalSummary: editedSummary,
        sourceType, status, version: editingRecord?.version ?? null,
      }, editingRecord?.id);
      setEditingRecord(response.data);
      setBaseline({ content: editedSummary, ai: aiSummary });
      setHistories({});
      setSuccessMessage(status === 'DRAFT' ? '초안을 서버에 임시저장했습니다.' : approvedEdit ? '수정 내용과 이력을 저장했습니다.' : '검토 완료된 인수인계를 승인했습니다.');
      if (status === 'APPROVED') setActiveTab('previous');
      else await loadPreviousHandovers();
    } catch (err) { setError(err.response?.data?.message || '인수인계 저장에 실패했습니다'); }
    finally { setIsSaving(false); }
  };
  const changePatientBody = (section, body) => {
    setPatientBodies(current => ({ ...current, [section.heading]: body }));
    setEditedSummary(current => replacePatientSection(current, section.heading, body));
  };
  const findPatient = section => [...myPatients, ...allPatients].find(patient =>
    (patient.chartNumber && section.heading.includes(patient.chartNumber)) || (patient.name && section.heading.startsWith(patient.name)));
  const regeneratePatient = async section => {
    const patient = findPatient(section);
    if (!patient || !fromShiftId) { setError('환자 또는 근무조 정보를 연결할 수 없습니다.'); return; }
    setRegeneratingPatientId(patient.patientId); setError(null);
    try {
      const response = await HandoverService.regeneratePatientAiSummary({ patientId: patient.patientId, fromShiftId });
      const body = parsePatientSections(response.data)[0]?.body || response.data.trim();
      changePatientBody(section, body);
      // Preserve regenerated AI provenance separately from the human-edited content.
      setAiSummary(current => replacePatientSection(current, section.heading, body));
    } catch (err) { setError(err.response?.data?.message || '환자별 AI 재생성에 실패했습니다'); }
    finally { setRegeneratingPatientId(null); }
  };
  const showHistory = async id => {
    if (historyId === id) { setHistoryId(null); return; }
    setHistoryId(id); setHistoryLoading(true); setError(null);
    try { const response = await HandoverService.getRevisions(id); setHistories(current => ({ ...current, [id]: response.data })); }
    catch (err) { setError(err.response?.data?.message || '수정 이력을 불러올 수 없습니다'); }
    finally { setHistoryLoading(false); }
  };

  return (
    <div className={styles.handoverTab}>
      <div className={styles.header}><h2>인수인계</h2></div>
      <div className={styles.tabs}>
        <button className={`${styles.tab} ${activeTab === 'previous' ? styles.active : ''}`} onClick={() => setActiveTab('previous')} disabled={busy}>인수인계 목록</button>
        <button className={`${styles.tab} ${activeTab === 'manual' ? styles.active : ''}`} onClick={() => chooseEditor('MANUAL')} disabled={busy}>수기 초안 작성</button>
        <button className={`${styles.tab} ${activeTab === 'generate' ? styles.active : ''}`} onClick={() => chooseEditor('AI')} disabled={busy}>AI 자동 생성</button>
      </div>
      {error && <div className={styles.errorMessage} role="alert">{error}</div>}
      {activeTab === 'previous' ? <div className={styles.previousSection}>
        <FilterBar sortValue={sortOrder} onSortChange={setSortOrder} sortBasis="기록일" label="인수인계" values={filters} onChange={setFilters} onReset={() => setFilters(handoverFilterDefaults)}
          searchPlaceholder="내용 · 작성자 검색" count={filteredHandovers.length} total={previousHandovers.length} fields={[
            { key: 'from', label: '시작일', type: 'date' }, { key: 'to', label: '종료일', type: 'date' },
            { key: 'author', label: '작성자', type: 'select', options: [...new Set(previousHandovers.map(record => record.createdByName).filter(Boolean))].map(name => ({ value: name, label: name })) },
            { key: 'shift', label: '인계 근무조', type: 'select', options: ['DAY', 'EVENING', 'NIGHT'].map(value => ({ value, label: value })) },
            { key: 'source', label: '작성 방식', type: 'select', options: [{ value: 'MANUAL', label: '수기' }, { value: 'AI', label: 'AI' }] },
            { key: 'status', label: '저장 상태', type: 'select', options: [{ value: 'DRAFT', label: '초안' }, { value: 'APPROVED', label: '승인됨' }] },
          ]} />
        {loading ? <div className={styles.loading}>로딩 중...</div> : filteredHandovers.length === 0 ?
          <div className={styles.emptyState}><p>{previousHandovers.length ? '필터에 맞는 인수인계가 없습니다' : '등록된 인수인계가 없습니다'}</p></div> :
          <div className={styles.handoverList}>{filteredHandovers.map(record => (
            <div key={record.id} className={styles.handoverCard}>
              <div className={styles.cardHeader}>
                <div className={styles.headerInfo}>
                  <span className={styles.date}>{formatDate(record.handoverDate)}</span>
                  <span className={styles.shift}>{record.fromShiftType} → {record.toShiftType}</span>
                  <span className={styles.author}>작성자: {record.createdByName}</span>
                  <span className={`${styles.approvedBadge} ${record.status === 'DRAFT' ? styles.draftStatus : ''}`}>{record.status === 'DRAFT' ? '임시저장 초안' : '승인됨'}</span>
                  <span className={styles.model}>{record.sourceType === 'MANUAL' ? '수기 작성' : 'AI 작성'}</span>
                </div>
                <div className={styles.cardActions}>
                  {user?.id === record.createdById && <button onClick={() => openRecord(record)}>{record.status === 'DRAFT' ? '초안 이어쓰기' : '승인 기록 수정'}</button>}
                  <button disabled={historyLoading} onClick={() => showHistory(record.id)}>수정 이력</button>
                  {user?.id === record.createdById && <button className={styles.deleteButton} onClick={() => setConfirmation({ title: '인수인계 삭제', message: '이 인수인계를 삭제하시겠습니까?', action: async () => {
                    try { await HandoverService.deleteHandover(record.id); await loadPreviousHandovers(); }
                    catch (err) { setError(err.response?.data?.message || '삭제에 실패했습니다'); }
                  } })}>삭제</button>}
                </div>
              </div>
              <div className={`${styles.cardContent} ${expandedHandoverId === record.id ? styles.expanded : ''}`}>
                <pre>{record.finalSummary?.replaceAll('**', '')}</pre>
                <button className={styles.expandButton} onClick={() => setExpandedHandoverId(expandedHandoverId === record.id ? null : record.id)}>{expandedHandoverId === record.id ? '내용 접기' : '전체 내용 보기'}</button>
              </div>
              {historyId === record.id && <div className={styles.history}>
                <h4>수정 이력</h4>
                {historyLoading ? <p>이력을 불러오는 중...</p> : !histories[record.id]?.length ? <p>아직 수정 이력이 없습니다. 이전 기록은 첫 수정부터 원본이 보존됩니다.</p> :
                  histories[record.id].map(revision => <details key={revision.id}>
                    <summary>{formatDateTime(revision.recordedAt)} · {revision.editedByName} · {revision.status === 'DRAFT' ? '초안' : '승인'} · 버전 {revision.recordVersion ?? '-'}</summary>
                    <pre>{revision.finalSummary}</pre>
                    {revision.aiSummary && <details><summary>이 버전의 AI 원본</summary><pre>{revision.aiSummary}</pre></details>}
                  </details>)}
              </div>}
            </div>
          ))}</div>}
      </div> : <div className={styles.generateSection}>
        <div className={styles.infoBox}>{sourceType === 'AI' ? 'AI 초안을 생성한 뒤 환자별 내용을 직접 수정하세요.' : '인수인계 내용을 직접 작성하세요.'} 임시저장한 초안은 작성자만 조회할 수 있습니다.</div>
        <div className={styles.editorTools}>
          <span>{editingRecord ? `저장된 ${approvedEdit ? '승인 기록' : '초안'} 편집` : '새 초안'} · {dirty ? '저장하지 않은 변경 있음' : '변경 없음'}</span>
          <button disabled={busy} onClick={() => guardChanges(() => clearEditor(sourceType))}>새 초안</button>
        </div>
        {sourceType === 'AI' && !aiSummary && <button className={styles.generateButton} onClick={generate} disabled={busy}>{isGenerating ? '생성 중...' : 'AI 자동 요약 생성'}</button>}
        {(sourceType === 'MANUAL' || aiSummary) && <div className={styles.summarySection}>
          <div className={styles.summaryHeader}>
            <div><span className={styles.eyebrow}>{formatDate(editingRecord?.handoverDate || selectedDate)} · {fromShiftType || '-'} → {toShiftType || '-'}</span><h3>{approvedEdit ? '승인된 인수인계 수정' : sourceType === 'AI' ? 'AI 인수인계 초안 편집' : '수기 인수인계 초안'}</h3></div>
            <span className={styles.draftBadge}>{approvedEdit ? '이력 보존' : '검토 필요'}</span>
          </div>
          <div className={styles.reviewNotice}>{approvedEdit ? '수정 전 내용은 이력에 보존됩니다.' : '내용을 확인한 뒤 임시저장하거나 검토 완료 및 승인해주세요.'}</div>
          {sourceType === 'AI' && patientSections.length > 0 ? <div className={styles.patientDraftList}>
            {patientSections.map((section, index) => <section className={styles.patientDraft} key={`${section.heading}-${index}`}>
              <div className={styles.patientDraftHeader}>
                <span className={styles.patientNumber}>{String(index + 1).padStart(2, '0')}</span><h4>{section.heading}</h4>
                {!approvedEdit && <button className={styles.patientRegenerateButton} disabled={busy || !findPatient(section)} onClick={() => setConfirmation({ title: '환자 초안 재생성', message: '이 환자에 작성한 내용을 AI 결과로 교체하시겠습니까?', action: () => regeneratePatient(section) })}>{regeneratingPatientId === findPatient(section)?.patientId ? '생성 중…' : '이 환자만 재생성'}</button>}
              </div>
              <textarea className={styles.patientDraftTextarea} value={patientBodies[section.heading] ?? section.body} onChange={event => changePatientBody(section, event.target.value)} rows={7} aria-label={`${section.heading} 인수인계 내용`} disabled={busy} />
            </section>)}
          </div> : <textarea className={styles.summaryTextarea} value={editedSummary} onChange={event => setEditedSummary(event.target.value)} rows={14} aria-label="인수인계 초안 내용" placeholder="환자별 상태, 수행 내용, 주의사항과 다음 근무조에 전달할 사항을 작성하세요." disabled={busy} />}
          {sourceType === 'AI' && <details className={styles.originalDraft}><summary>AI 원본 확인</summary><pre>{aiSummary}</pre></details>}
          <div className={styles.actions}>
            <span className={styles.editHint}>{approvedEdit ? '수정 내용과 작성 시각을 함께 보존합니다' : '초안은 서버에 저장되어 다시 열 수 있습니다'}</span>
            {sourceType === 'AI' && !approvedEdit && <button className={styles.regenerateButton} onClick={() => setConfirmation({ title: 'AI 초안 다시 생성', message: '현재 편집 내용을 AI 결과로 교체하시겠습니까?', action: generate })} disabled={busy}>다시 생성</button>}
            {!approvedEdit && <button className={styles.regenerateButton} onClick={() => save('DRAFT')} disabled={busy || !editedSummary.trim()}>초안 임시저장</button>}
            <button className={styles.saveButton} disabled={busy || !editedSummary.trim() || (approvedEdit && !dirty)} onClick={() => setConfirmation({ title: approvedEdit ? '승인 기록 수정 저장' : '인수인계 승인', message: approvedEdit ? '수정 내용과 이전 버전을 이력에 보존하여 저장하시겠습니까?' : '검토를 완료한 내용을 승인된 인수인계로 저장하시겠습니까?', action: () => save('APPROVED') })}>{isSaving ? '저장 중...' : approvedEdit ? '수정 저장' : '검토 완료 및 승인'}</button>
          </div>
        </div>}
      </div>}
      {successMessage && <SuccessModal message={successMessage} onClose={() => setSuccessMessage('')} />}
      <ConfirmModal danger={confirmation?.title === '인수인계 삭제'} isOpen={Boolean(confirmation)} title={confirmation?.title} message={confirmation?.message}
        onClose={() => setConfirmation(null)} onConfirm={() => { const action = confirmation?.action; setConfirmation(null); action?.(); }} />
    </div>
  );
};
export default HandoverTab;
