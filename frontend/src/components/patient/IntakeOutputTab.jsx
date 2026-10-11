import { formatDateTime } from '../../utils/dateFormat';
import useUnsavedRecord from '../../hooks/useUnsavedRecord';
import DateTimePicker from '../common/DateTimePicker';
import useRecordList from '../../hooks/useRecordList';
import RequestError from '../common/RequestError';
import SortControl from '../common/SortControl';
import { sortRecords } from '../../utils/sortRecords';
import { useState, useRef } from 'react';
import apiClient from '../../services/apiClient';
import TimeSeriesChart from '../common/TimeSeriesChart';
import styles from './IntakeOutputTab.module.scss';

const IntakeOutputTab = ({ patientId, onDirtyChange }) => {
  // I/O 메트릭 정의
  const ioMetrics = [
    { key: 'intakeOral', label: '경구 섭취', color: '#3b82f6', unit: 'mL' },
    { key: 'intakeIv', label: '정맥 수액', color: '#8b5cf6', unit: 'mL' },
    { key: 'intakeTotal', label: '섭취 총량', color: '#06b6d4', unit: 'mL' },
    { key: 'outputUrine', label: '소변', color: '#f59e0b', unit: 'mL' },
    { key: 'outputDrain', label: '배액', color: '#ef4444', unit: 'mL' },
    { key: 'outputTotal', label: '배설 총량', color: '#dc2626', unit: 'mL' },
  ];

  const { items: records, loading, loadError, refresh: fetchRecords } = useRecordList(`/intake-output/patient/${patientId}`, 'I/O 목록을 불러올 수 없습니다');

  const [sortOrder, setSortOrder] = useState('desc');
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const saveLock = useRef(false);
  const [editingVersion, setEditingVersion] = useState(null);
  const [editingRecordId, setEditingRecordId] = useState(null);
  const [showChart, setShowChart] = useState(false);

  // 현재 시간 (한국 시간)

  const [formData, setFormData] = useState({
    intakeOral: '',
    intakeIv: '',
    outputUrine: '',
    outputDrain: '',
    recordedAt: '',
  });

  useUnsavedRecord(showForm, onDirtyChange, saving);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // I/O 등록/수정
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (saveLock.current) return;
    saveLock.current = true;
    setSaving(true);
    setError(null);

    try {
      const payload = {
        patientId,
        version: editingVersion,
        intakeOral: formData.intakeOral ? parseInt(formData.intakeOral) : null,
        intakeIv: formData.intakeIv ? parseInt(formData.intakeIv) : null,
        outputUrine: formData.outputUrine ? parseInt(formData.outputUrine) : null,
        outputDrain: formData.outputDrain ? parseInt(formData.outputDrain) : null,
        recordedAt: formData.recordedAt || null,
      };

      if (editingRecordId) {
        await apiClient.put(`/intake-output/${editingRecordId}`, payload);
      } else {
        await apiClient.post('/intake-output', payload);
      }
      
      resetForm();
      fetchRecords();
    } catch (err) {

      setError(err.response?.data?.message || 'I/O 저장에 실패했습니다');
    } finally { saveLock.current = false; setSaving(false); }
  };

  const resetForm = () => {
    setFormData({
      intakeOral: '',
      intakeIv: '',
      outputUrine: '',
      outputDrain: '',
      recordedAt: '',
    });
    setEditingRecordId(null);
    setEditingVersion(null);
    setShowForm(false);
  };

  const handleEdit = (record) => {
    setFormData({
      intakeOral: record.intakeOral || '',
      intakeIv: record.intakeIv || '',
      outputUrine: record.outputUrine || '',
      outputDrain: record.outputDrain || '',
      recordedAt: record.recordedAt ? record.recordedAt.slice(0, 16) : '',
    });
    setEditingRecordId(record.id);
    setEditingVersion(record.version);
    setShowForm(true);
    setShowChart(false);
  };

  const prepareChartData = () => {
    return sortRecords(records, 'recordedAt', 'asc')
      .map(record => ({
        time: record.recordedAt,
        intakeOral: record.intakeOral,
        intakeIv: record.intakeIv,
        intakeTotal: record.intakeTotal,
        outputUrine: record.outputUrine,
        outputDrain: record.outputDrain,
        outputTotal: record.outputTotal,
      }));
  };

  // 당일 누적 계산
  const getTodayTotal = () => {
    const today = new Date().toDateString();
    const todayRecords = records.filter(r => 
      new Date(r.recordedAt).toDateString() === today
    );

    const intakeTotal = todayRecords.reduce((sum, r) => sum + (r.intakeTotal || 0), 0);
    const outputTotal = todayRecords.reduce((sum, r) => sum + (r.outputTotal || 0), 0);

    return { intakeTotal, outputTotal, balance: intakeTotal - outputTotal };
  };

  const formatTime = (dateTime) => {
    return formatDateTime(dateTime);
  };

  // 경고 체크
  const getWarning = (record) => {
    const warnings = [];
    
    // 1회 섭취량 > 2000mL
    if (record.intakeTotal > 2000) {
      warnings.push('과다 섭취');
    }
    
    // 배설량 < 100mL (8시간 기준 체크는 복잡하므로 단순화)
    if (record.outputTotal < 100 && record.outputTotal > 0) {
      warnings.push('배설량 부족');
    }

    return warnings;
  };

  const todayTotal = getTodayTotal();

  if (loading && records.length === 0) {
    return <div className={styles.loading}>I/O 정보를 불러오는 중...</div>;
  }

  return (
    <div className={styles.ioTab}>
      <div className={styles.header}>
        <h3>섭취배설량 (I/O)</h3>
        <div className={styles.headerButtons}>
          <button 
            className={styles.chartButton}
            onClick={() => setShowChart(!showChart)}
          >
            {showChart ? '목록 보기' : '한눈에 보기'}
          </button>
          <button 
            className={styles.addButton} disabled={saving}
            onClick={() => {
              if (showForm) {
                resetForm();
              } else {
                setShowForm(true);
              }
            }}
          >
            {saving ? '저장 중…' : showForm ? '취소' : '+ I/O 등록'}
          </button>
        </div>
      </div>
      <div className="record-toolbar"><SortControl value={sortOrder} onChange={setSortOrder} basis="기록일시" /></div>

      <RequestError className={styles.error} message={error || loadError} onRetry={loadError ? fetchRecords : undefined} />

      {/* 당일 누적 */}
      <div className={styles.todaySummary}>
        <div className={styles.summaryItem}>
          <span className={styles.label}>당일 섭취</span>
          <span className={styles.value}>{todayTotal.intakeTotal} mL</span>
        </div>
        <div className={styles.summaryItem}>
          <span className={styles.label}>당일 배설</span>
          <span className={styles.value}>{todayTotal.outputTotal} mL</span>
        </div>
        <div className={styles.summaryItem}>
          <span className={styles.label}>수분 균형</span>
          <span className={`${styles.value} ${todayTotal.balance >= 0 ? styles.positive : styles.negative}`}>
            {todayTotal.balance > 0 ? '+' : ''}{todayTotal.balance} mL
          </span>
        </div>
      </div>

      {/* I/O 입력 폼 */}
      {showForm && (
        <form className={styles.form} onSubmit={handleSubmit} aria-busy={saving}>
          <fieldset className="record-form-fields" disabled={saving}>
          <div className={styles.formGroup}>
            <label>기록시간</label>
            <DateTimePicker id="recordedAt" label="기록 일시" value={formData.recordedAt} onChange={value => setFormData(prev => ({ ...prev, recordedAt: value }))} />
            <small>기록 시간을 입력하세요 (미입력 시 현재 시간, 미래 시간 입력 불가)</small>
          </div>

          <div className={styles.formGrid}>
            <div className={styles.formSection}>
              <h4>섭취량 (Intake)</h4>
              <div className={styles.formGroup}>
                <label>경구 섭취 (mL)</label>
                <input
                  type="number"
                  name="intakeOral"
                  aria-label="경구 섭취량 (mL)"
                  value={formData.intakeOral}
                  onChange={handleChange}
                  placeholder="0"
                  min="0"
                  max="5000"
                />
              </div>
              <div className={styles.formGroup}>
                <label>정맥 수액 (mL)</label>
                <input
                  type="number"
                  name="intakeIv"
                  aria-label="정맥 수액량 (mL)"
                  value={formData.intakeIv}
                  onChange={handleChange}
                  placeholder="0"
                  min="0"
                  max="5000"
                />
              </div>
            </div>

            <div className={styles.formSection}>
              <h4>배설량 (Output)</h4>
              <div className={styles.formGroup}>
                <label>소변 (mL)</label>
                <input
                  type="number"
                  name="outputUrine"
                  aria-label="소변량 (mL)"
                  value={formData.outputUrine}
                  onChange={handleChange}
                  placeholder="0"
                  min="0"
                  max="5000"
                />
              </div>
              <div className={styles.formGroup}>
                <label>배액 (mL)</label>
                <input
                  type="number"
                  name="outputDrain"
                  aria-label="배액량 (mL)"
                  value={formData.outputDrain}
                  onChange={handleChange}
                  placeholder="0"
                  min="0"
                  max="5000"
                />
              </div>
            </div>
          </div>

          <div className={styles.formActions}>
            <button type="submit" className={styles.submitButton}>
              {editingRecordId ? '수정' : '등록'}
            </button>
            {editingRecordId && (
              <button 
                type="button" 
                className={styles.cancelButton}
                onClick={resetForm}
              >
                취소
              </button>
            )}
          </div>
        </fieldset>
        </form>
      )}

      {/* 차트 보기 */}
      {showChart && records.length > 0 && (
        <TimeSeriesChart
          data={prepareChartData()}
          metrics={ioMetrics}
          title="섭취배설량 추이"
        />
      )}

      {/* I/O 이력 목록 */}
      {!showChart && <div className={styles.recordList}>
        {records.length === 0 ? (
          <div className={styles.empty}>등록된 I/O 기록이 없습니다</div>
        ) : (
          sortRecords(records, 'recordedAt', sortOrder).map((record) => {
            const warnings = getWarning(record);
            
            return (
              <div key={record.id} className={styles.recordItem}>
                <div className={styles.recordHeader}>
                  <span className={styles.time}>{formatTime(record.recordedAt)}</span>
                  <div className={styles.recordHeaderRight}>
                    <span className={styles.nurse}>{record.nurseName}</span>
                    {record.canEdit && (
                      <button 
                        className={styles.editButton}
                        onClick={() => handleEdit(record)}
                        title="I/O 수정"
                      >
                        I/O 수정
                      </button>
                    )}
                  </div>
                </div>

                {warnings.length > 0 && (
                  <div className={styles.warnings}>
                    {warnings.map((warning, idx) => (
                      <span key={idx} className={styles.warningBadge}>⚠️ {warning}</span>
                    ))}
                  </div>
                )}

                <div className={styles.recordData}>
                  <div className={styles.dataSection}>
                    <h5>섭취 (Intake)</h5>
                    <div className={styles.dataItems}>
                      {record.intakeOral > 0 && (
                        <div className={styles.dataItem}>
                          <span className={styles.label}>경구</span>
                          <span className={styles.value}>{record.intakeOral} mL</span>
                        </div>
                      )}
                      {record.intakeIv > 0 && (
                        <div className={styles.dataItem}>
                          <span className={styles.label}>정맥</span>
                          <span className={styles.value}>{record.intakeIv} mL</span>
                        </div>
                      )}
                      <div className={`${styles.dataItem} ${styles.total}`}>
                        <span className={styles.label}>총량</span>
                        <span className={styles.value}>{record.intakeTotal} mL</span>
                      </div>
                    </div>
                  </div>

                  <div className={styles.dataSection}>
                    <h5>배설 (Output)</h5>
                    <div className={styles.dataItems}>
                      {record.outputUrine > 0 && (
                        <div className={styles.dataItem}>
                          <span className={styles.label}>소변</span>
                          <span className={styles.value}>{record.outputUrine} mL</span>
                        </div>
                      )}
                      {record.outputDrain > 0 && (
                        <div className={styles.dataItem}>
                          <span className={styles.label}>배액</span>
                          <span className={styles.value}>{record.outputDrain} mL</span>
                        </div>
                      )}
                      <div className={`${styles.dataItem} ${styles.total}`}>
                        <span className={styles.label}>총량</span>
                        <span className={styles.value}>{record.outputTotal} mL</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>}
    </div>
  );
};

export default IntakeOutputTab;
