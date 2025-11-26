import { useState, useEffect } from 'react';
import { HandoverService } from '../../services/handoverService';
import { useAuthStore } from '../../stores/authStore';
import SuccessModal from '../common/SuccessModal';
import ConfirmModal from '../common/ConfirmModal';
import styles from './HandoverTab.module.scss';

const HandoverTab = () => {
  const { user } = useAuthStore();
  const [activeTab, setActiveTab] = useState('previous');
  const [aiSummary, setAiSummary] = useState('');
  const [editedSummary, setEditedSummary] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState(null);
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [deleteTargetId, setDeleteTargetId] = useState(null);
  const [previousHandovers, setPreviousHandovers] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (activeTab === 'previous') {
      loadPreviousHandovers();
    }
  }, [activeTab]);

  const loadPreviousHandovers = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await HandoverService.getHandoversByDepartment(user?.department?.id || 1);
      if (response.success && response.data.length > 0) {
        const formatted = response.data.map(h => ({
          id: h.id,
          date: h.handoverDate,
          shift: `${h.fromShiftType} → ${h.toShiftType}`,
          summary: h.aiSummary,
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
  };

  const handleGenerateAiSummary = async () => {
    setIsGenerating(true);
    setError(null);

    console.log('=== AI 인수인계 생성 요청 ===');
    console.log('사용자 정보:', user);
    console.log('사용자 ID:', user?.id);
    console.log('부서 ID:', user?.department?.id);
    console.log('요청 파라미터 - departmentId:', user?.department?.id || 1, 'fromShiftId: 1');

    try {
      const response = await HandoverService.generateAiSummary({
        departmentId: user?.department?.id || 1,
        fromShiftId: 1
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
    if (!editedSummary.trim()) {
      setError('인수인계 내용을 입력해주세요');
      return;
    }

    setIsSaving(true);
    setError(null);

    try {
      const response = await HandoverService.saveHandover({
        departmentId: user?.department?.id || 1,
        fromShiftId: 1,
        toShiftId: 2,
        aiSummary: editedSummary
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
              {previousHandovers.map((handover) => (
                <div key={handover.id} className={styles.handoverCard}>
                  <div className={styles.cardHeader}>
                    <div className={styles.headerInfo}>
                      <span className={styles.date}>{handover.date}</span>
                      <span className={styles.shift}>{handover.shift}</span>
                      <span className={styles.author}>작성자: {handover.createdByName}</span>
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
                  <div className={styles.cardContent}>
                    <pre>{handover.summary}</pre>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'generate' && (
        <div className={styles.generateSection}>
          <div className={styles.infoBox}>
            현재 근무조에 배정된 환자 정보를 분석하여 인수인계문을 자동 생성합니다.
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
                <h3>인수인계 요약</h3>
                <span className={styles.editHint}>내용을 수정할 수 있습니다</span>
              </div>
              <textarea
                className={styles.summaryTextarea}
                value={editedSummary}
                onChange={(e) => setEditedSummary(e.target.value)}
                rows={20}
                placeholder="인수인계 내용을 입력하세요"
              />
              <div className={styles.actions}>
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
                  {isSaving ? '저장 중...' : '저장'}
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
