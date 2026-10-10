import { useState, useEffect, useCallback } from 'react';
import { HandoverService } from '../../services/handoverService';
import { useAuthStore } from '../../stores/authStore';
import useDashboardStore from '../../stores/useDashboardStore';
import SuccessModal from '../common/SuccessModal';
import ConfirmModal from '../common/ConfirmModal';
import styles from './HandoverTab.module.scss';

const parsePatientSections = (summary) => {
  const sections = [];
  const pattern = /\*\*(\[[^\n]+\])\*\*\s*\n([\s\S]*?)(?=\n\s*\*\*\[|$)/g;
  let match;

  while ((match = pattern.exec(summary)) !== null) {
    sections.push({
      start: match.index,
      end: pattern.lastIndex,
      heading: match[1].replace(/^\[|\]$/g, ''),
      body: match[2].trim()
    });
  }

  return sections;
};

const HandoverTab = () => {
  const { user } = useAuthStore();
  const { myPatients, allPatients, departmentSummary } = useDashboardStore();
  const currentShiftType = departmentSummary?.shiftType?.toUpperCase();
  const currentShiftId = departmentSummary?.shiftId;
  const nextShiftId = departmentSummary?.nextShiftId;
  const departmentId = departmentSummary?.departmentId || user?.department?.id;
  const nextShiftType = { DAY: 'EVENING', EVENING: 'NIGHT', NIGHT: 'DAY' }[currentShiftType];
  const [activeTab, setActiveTab] = useState('previous');
  const [aiSummary, setAiSummary] = useState('');
  const [editedSummary, setEditedSummary] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [regeneratingPatientId, setRegeneratingPatientId] = useState(null);
  const [error, setError] = useState(null);
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [deleteTargetId, setDeleteTargetId] = useState(null);
  const [previousHandovers, setPreviousHandovers] = useState([]);
  const [expandedHandoverId, setExpandedHandoverId] = useState(null);
  const [loading, setLoading] = useState(false);

  const loadPreviousHandovers = useCallback(async () => {
    if (!departmentId) return;
    setLoading(true);
    setError(null);
    try {
      const response = await HandoverService.getHandoversByDepartment(departmentId);
      if (response.success && response.data.length > 0) {
        const formatted = response.data.map(h => ({
          id: h.id,
          date: h.handoverDate,
          shift: `${h.fromShiftType} → ${h.toShiftType}`,
          summary: h.finalSummary,
          aiDraft: h.aiSummary,
          status: h.status,
          aiModel: h.aiModel,
          approvedAt: h.approvedAt,
          createdById: h.createdById,
          createdByName: h.createdByName
        }));
        setPreviousHandovers(formatted);
      } else {
        setPreviousHandovers([]);
      }
    } catch (err) {
      console.error('이전 인수인계 조회 실패:', err);
      setError('이전 인수인계 목록을 불러올 수 없습니다');
    } finally {
      setLoading(false);
    }
  }, [departmentId]);

  useEffect(() => {
    if (activeTab === 'previous') {
      loadPreviousHandovers();
    }
  }, [activeTab, loadPreviousHandovers]);

  const handleGenerateAiSummary = async () => {
    if (!currentShiftId) {
      setError('현재 근무조 정보를 불러온 뒤 다시 시도해 주세요.');
      return;
    }
    setIsGenerating(true);
    setError(null);

    try {
      const response = await HandoverService.generateAiSummary({
        departmentId,
        fromShiftId: currentShiftId
      });

      if (response.success) {
        setAiSummary(response.data);
        setEditedSummary(response.data);
      }
    } catch (err) {
      console.error('AI 요약 생성 실패:', err);
      setError(err.response?.data?.message || 'AI 요약 생성 중 오류가 발생했습니다');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSave = async () => {
    if (!currentShiftId || !nextShiftId) {
      setError('현재 근무조 정보를 불러온 뒤 다시 시도해 주세요.');
      return;
    }
    if (!editedSummary.trim()) {
      setError('인수인계 내용을 입력해주세요');
      return;
    }

    setIsSaving(true);
    setError(null);

    try {
      const response = await HandoverService.saveHandover({
        departmentId,
        fromShiftId: currentShiftId,
        toShiftId: nextShiftId,
        aiDraft: aiSummary,
        finalSummary: editedSummary
      });

      if (response.success) {
        setSuccessMessage('인수인계가 저장되었습니다');
        setShowSuccessModal(true);
        setAiSummary('');
        setEditedSummary('');
        setActiveTab('previous');
        loadPreviousHandovers();
      }
    } catch (err) {
      console.error('인수인계 저장 실패:', err);
      setError(err.response?.data?.message || '저장 중 오류가 발생했습니다');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteClick = (handoverId) => {
    setDeleteTargetId(handoverId);
    setShowConfirmModal(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      const response = await HandoverService.deleteHandover(deleteTargetId);
      if (response.success) {
        setSuccessMessage('인수인계가 삭제되었습니다');
        setShowSuccessModal(true);
        loadPreviousHandovers();
      }
    } catch (err) {
      console.error('인수인계 삭제 실패:', err);
      setError(err.response?.data?.message || '삭제 중 오류가 발생했습니다');
    } finally {
      setShowConfirmModal(false);
      setDeleteTargetId(null);
    }
  };

  const handlePatientSummaryChange = (section, body) => {
    setEditedSummary((currentSummary) => {
      const currentSection = parsePatientSections(currentSummary)
        .find((item) => item.heading === section.heading);
      if (!currentSection) return currentSummary;

      const replacement = `**[${currentSection.heading}]**\n${body}`;
      return `${currentSummary.slice(0, currentSection.start)}${replacement}${currentSummary.slice(currentSection.end)}`;
    });
  };

  const findPatientForSection = (section) => {
    const visiblePatients = [...myPatients, ...allPatients];
    return visiblePatients.find((patient) =>
      section.heading.includes(patient.chartNumber) || section.heading.startsWith(patient.name)
    );
  };

  const handleRegeneratePatient = async (section) => {
    const patient = findPatientForSection(section);
    if (!patient) {
      setError('초안의 환자와 담당 환자 정보를 연결할 수 없습니다. 전체 초안을 다시 생성해 주세요.');
      return;
    }

    setRegeneratingPatientId(patient.patientId);
    setError(null);
    try {
      const response = await HandoverService.regeneratePatientAiSummary({
        patientId: patient.patientId,
        fromShiftId: currentShiftId
      });
      const regeneratedSections = parsePatientSections(response.data);
      const regeneratedBody = regeneratedSections[0]?.body || response.data.trim();
      handlePatientSummaryChange(section, regeneratedBody);
    } catch (err) {
      console.error('환자별 AI 요약 재생성 실패:', err);
      setError(err.response?.data?.message || '해당 환자의 AI 초안을 다시 생성하지 못했습니다');
    } finally {
      setRegeneratingPatientId(null);
    }
  };

  const patientSections = parsePatientSections(editedSummary);

  return (
    <div className={styles.handoverTab}>
      <div className={styles.header}>
        <h2>인수인계</h2>
      </div>

      <div className={styles.tabs}>
        <button
          className={`${styles.tab} ${activeTab === 'previous' ? styles.active : ''}`}
          onClick={() => setActiveTab('previous')}
        >
          이전 인수인계
        </button>
        <button
          className={`${styles.tab} ${activeTab === 'generate' ? styles.active : ''}`}
          onClick={() => setActiveTab('generate')}
        >
          AI 자동 생성
        </button>
      </div>

      {error && (
        <div className={styles.errorMessage}>
          {error}
        </div>
      )}

      {activeTab === 'previous' && (
        <div className={styles.previousSection}>
          {loading ? (
            <div className={styles.loading}>로딩 중...</div>
          ) : previousHandovers.length === 0 ? (
            <div className={styles.emptyState}>
              <p>등록된 인수인계가 없습니다</p>
            </div>
          ) : (
            <div className={styles.handoverList}>
              {previousHandovers.map((handover) => {
                const isExpanded = expandedHandoverId === handover.id;
                const readableSummary = handover.summary?.replaceAll('**', '') || '';
                return (
                <div key={handover.id} className={styles.handoverCard}>
                  <div className={styles.cardHeader}>
                    <div className={styles.headerInfo}>
                      <span className={styles.date}>{formatDate(handover.date)}</span>
                      <span className={styles.shift}>{handover.shift}</span>
                      <span className={styles.author}>작성자: {handover.createdByName}</span>
                      <span className={styles.approvedBadge}>검토·승인됨</span>
                      {handover.aiModel && (
                        <span className={styles.model} title="AI로 작성된 초안">AI 작성</span>
                      )}
                    </div>
                    {user?.id === handover.createdById && (
                      <button
                        className={styles.deleteButton}
                        onClick={() => handleDeleteClick(handover.id)}
                        title="삭제"
                      >
                        삭제
                      </button>
                    )}
                  </div>
                  <div className={`${styles.cardContent} ${isExpanded ? styles.expanded : ''}`}>
                    <pre>{readableSummary}</pre>
                    <button
                      type="button"
                      className={styles.expandButton}
                      onClick={() => setExpandedHandoverId(isExpanded ? null : handover.id)}
                    >
                      {isExpanded ? '내용 접기' : '전체 내용 보기'}
                    </button>
                  </div>
                </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {activeTab === 'generate' && (
        <div className={styles.generateSection}>
          <div className={styles.infoBox}>
            현재 근무조 기록으로 AI 초안을 생성합니다. 환자 식별정보는 외부 전송 전에
            가명화되며, 초안은 간호사가 반드시 검토·수정해야 최종 인수인계로 저장됩니다.
          </div>

          {!aiSummary && (
            <button
              className={styles.generateButton}
              onClick={handleGenerateAiSummary}
              disabled={isGenerating}
            >
              {isGenerating ? (
                <>
                  <span className={styles.spinner}></span>
                  생성 중...
                </>
              ) : (
                'AI 자동 요약 생성'
              )}
            </button>
          )}

          {aiSummary && (
            <div className={styles.summarySection}>
              <div className={styles.summaryHeader}>
                <div>
                  <span className={styles.eyebrow}>{currentShiftType} → {nextShiftType}</span>
                  <h3>AI 인수인계 초안</h3>
                </div>
                <div className={styles.draftMeta}>
                  <span className={styles.patientCount}>환자 {patientSections.length}명</span>
                  <span className={styles.draftBadge}>검토 필요</span>
                </div>
              </div>

              <div className={styles.reviewNotice}>
                AI가 작성한 초안입니다. 환자별 내용을 확인하고 필요한 부분을 직접 수정해 주세요.
              </div>

              {patientSections.length > 0 ? (
                <div className={styles.patientDraftList}>
                  {patientSections.map((section, index) => (
                    <section className={styles.patientDraft} key={`${section.heading}-${index}`}>
                      <div className={styles.patientDraftHeader}>
                        <span className={styles.patientNumber}>{String(index + 1).padStart(2, '0')}</span>
                        <h4>{section.heading}</h4>
                        <button
                          type="button"
                          className={styles.patientRegenerateButton}
                          onClick={() => handleRegeneratePatient(section)}
                          disabled={regeneratingPatientId !== null || !findPatientForSection(section)}
                          title={findPatientForSection(section) ? '이 환자의 초안만 다시 생성' : '담당 환자 연결 필요'}
                        >
                          {regeneratingPatientId === findPatientForSection(section)?.patientId
                            ? '생성 중…'
                            : '이 환자만 재생성'}
                        </button>
                      </div>
                      <textarea
                        className={styles.patientDraftTextarea}
                        value={section.body}
                        onChange={(e) => handlePatientSummaryChange(section, e.target.value)}
                        rows={7}
                        aria-label={`${section.heading} 인수인계 내용`}
                      />
                    </section>
                  ))}
                </div>
              ) : (
                <textarea
                  className={styles.summaryTextarea}
                  value={editedSummary}
                  onChange={(e) => setEditedSummary(e.target.value)}
                  rows={20}
                  placeholder="인수인계 내용을 입력하세요"
                />
              )}

              <div className={styles.actions}>
                <span className={styles.editHint}>승인 시 AI 원본과 수정본이 함께 보존됩니다</span>
                <button
                  className={styles.regenerateButton}
                  onClick={handleGenerateAiSummary}
                  disabled={isGenerating}
                >
                  다시 생성
                </button>
                <button
                  className={styles.saveButton}
                  onClick={handleSave}
                  disabled={isSaving}
                >
                  {isSaving ? '승인 중...' : '검토 완료 및 승인'}
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {showSuccessModal && (
        <SuccessModal
          message={successMessage}
          onClose={() => setShowSuccessModal(false)}
        />
      )}

      <ConfirmModal
        isOpen={showConfirmModal}
        onClose={() => {
          setShowConfirmModal(false);
          setDeleteTargetId(null);
        }}
        onConfirm={handleDeleteConfirm}
        title="인수인계 삭제"
        message="이 인수인계를 삭제하시겠습니까?"
      />
    </div>
  );
};

export default HandoverTab;
import { formatDate } from '../../utils/dateFormat';
