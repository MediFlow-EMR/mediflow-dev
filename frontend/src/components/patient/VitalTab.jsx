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
import styles from './VitalTab.module.scss';

const VitalTab = ({ patientId, onDirtyChange }) => {
  // 바이탈 메트릭 정의
  const vitalMetrics = [
    { key: 'systolicBp', label: '수축기 혈압', color: '#ef4444', unit: 'mmHg' },
    { key: 'diastolicBp', label: '이완기 혈압', color: '#f97316', unit: 'mmHg' },
    { key: 'heartRate', label: '맥박', color: '#ec4899', unit: '회/분' },
    { key: 'bodyTemp', label: '체온', color: '#8b5cf6', unit: '°C' },
    { key: 'respiratoryRate', label: '호흡', color: '#3b82f6', unit: '회/분' },
    { key: 'spo2', label: 'SpO2', color: '#10b981', unit: '%' },
  ];
  const { items: vitals, loading, loadError, refresh: fetchVitals } = useRecordList(`/vitals/patient/${patientId}`, '바이탈 목록을 불러올 수 없습니다');

  const chronological = sortRecords(vitals, 'measuredAt', 'desc');
  const previousVitals = new Map(chronological.map((vital, index) => [vital.id, chronological[index + 1]]));
  const [sortOrder, setSortOrder] = useState('desc');
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const saveLock = useRef(false);
  const [editingVersion, setEditingVersion] = useState(null);

  // 현재 시간 (한국 시간)

  const [formData, setFormData] = useState({
    systolicBp: '',
    diastolicBp: '',
    heartRate: '',
    bodyTemp: '',
    respiratoryRate: '',
    spo2: '',
    measuredAt: '', // 측정시간
  });

  const [editingVitalId, setEditingVitalId] = useState(null);

  const [showChart, setShowChart] = useState(false);

  useUnsavedRecord(showForm, onDirtyChange, saving);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // 바이탈 등록/수정
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
        systolicBp: formData.systolicBp ? parseInt(formData.systolicBp) : null,
        diastolicBp: formData.diastolicBp ? parseInt(formData.diastolicBp) : null,
        heartRate: formData.heartRate ? parseInt(formData.heartRate) : null,
        bodyTemp: formData.bodyTemp ? parseFloat(formData.bodyTemp) : null,
        respiratoryRate: formData.respiratoryRate ? parseInt(formData.respiratoryRate) : null,
        spo2: formData.spo2 ? parseInt(formData.spo2) : null,
        measuredAt: formData.measuredAt || null,
      };

      if (editingVitalId) {

        await apiClient.put(`/vitals/${editingVitalId}`, payload);
      } else {

        await apiClient.post('/vitals', payload);
      }

      resetForm();
      fetchVitals();
    } catch (err) {

      setError(err.response?.data?.message || '바이탈 저장에 실패했습니다');
    } finally { saveLock.current = false; setSaving(false); }
  };

  const resetForm = () => {
    setFormData({
      systolicBp: '',
      diastolicBp: '',
      heartRate: '',
      bodyTemp: '',
      respiratoryRate: '',
      spo2: '',
      measuredAt: '',
    });
    setEditingVitalId(null);
    setEditingVersion(null);
    setShowForm(false);
  };

  const handleEdit = (vital) => {
    setFormData({
      systolicBp: vital.systolicBp || '',
      diastolicBp: vital.diastolicBp || '',
      heartRate: vital.heartRate || '',
      bodyTemp: vital.bodyTemp || '',
      respiratoryRate: vital.respiratoryRate || '',
      spo2: vital.spo2 || '',
      measuredAt: vital.measuredAt ? vital.measuredAt.slice(0, 16) : '',
    });
    setEditingVitalId(vital.id);
    setEditingVersion(vital.version);
    setShowForm(true);
    setShowChart(false); // 차트 닫기
  };

  const prepareChartData = () => {
    return sortRecords(vitals, 'measuredAt', 'asc')
      .map(vital => ({
        time: vital.measuredAt,
        systolicBp: vital.systolicBp,
        diastolicBp: vital.diastolicBp,
        heartRate: vital.heartRate,
        bodyTemp: vital.bodyTemp,
        respiratoryRate: vital.respiratoryRate,
        spo2: vital.spo2,
      }));
  };

  // 정상 범위 체크
  const getStatusClass = (type, value) => {
    if (!value) return '';

    const ranges = {
      systolicBp: { min: 90, max: 140, critical: { min: 70, max: 180 } },
      diastolicBp: { min: 60, max: 90, critical: { min: 40, max: 110 } },
      heartRate: { min: 60, max: 100, critical: { min: 40, max: 150 } },
      bodyTemp: { min: 36.1, max: 37.2, critical: { min: 35.5, max: 38.5 } },
      respiratoryRate: { min: 12, max: 20, critical: { min: 8, max: 30 } },
      spo2: { min: 95, max: 100, critical: { min: 90, max: 100 } },
    };

    const range = ranges[type];
    if (!range) return '';

    if (value < range.critical.min || value > range.critical.max) {
      return styles.critical;
    }
    if (value < range.min || value > range.max) {
      return styles.warning;
    }
    return styles.normal;
  };

  // 이전 바이탈과 비교
  const getChangeIndicator = (currentValue, previousValue) => {
    if (!currentValue || !previousValue) return null;
    
    const diff = currentValue - previousValue;
    if (Math.abs(diff) < 0.1) return null;

    return (
      <span className={diff > 0 ? styles.increase : styles.decrease}>
        {diff > 0 ? '↑' : '↓'} {Math.abs(diff).toFixed(1)}
      </span>
    );
  };

  const formatTime = (dateTime) => {
    return formatDateTime(dateTime);
  };

  if (loading && vitals.length === 0) {
    return <div className={styles.loading}>V/S 정보를 불러오는 중...</div>;
  }

  return (
    <div className={styles.vitalTab}>
      <div className={styles.header}>
        <h3>V/S</h3>
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
            {saving ? '저장 중…' : showForm ? '취소' : '+ V/S 등록'}
          </button>
        </div>
      </div>
      <div className="record-toolbar"><SortControl value={sortOrder} onChange={setSortOrder} basis="측정일시" /></div>

      <RequestError className={styles.error} message={error || loadError} onRetry={loadError ? fetchVitals : undefined} />

      {/* 바이탈 입력 폼 */}
      {showForm && (
        <form className={styles.form} onSubmit={handleSubmit} aria-busy={saving}>
          <fieldset className="record-form-fields" disabled={saving}>
          <div className={styles.formGroup}>
            <label>측정시간</label>
            <DateTimePicker id="measuredAt" label="측정 일시" value={formData.measuredAt} onChange={value => setFormData(prev => ({ ...prev, measuredAt: value }))} />
            <small>측정 시간을 입력하세요 (미입력 시 현재 시간, 미래 시간 입력 불가)</small>
          </div>

          <div className={styles.formGrid}>
            <div className={styles.formGroup}>
              <label>혈압 (mmHg)</label>
              <div className={styles.bpInput}>
                <input
                  type="number"
                  name="systolicBp"
                  aria-label="수축기 혈압 (mmHg)"
                  value={formData.systolicBp}
                  onChange={handleChange}
                  placeholder="수축기"
                  min="50"
                  max="250"
                />
                <span>/</span>
                <input
                  type="number"
                  name="diastolicBp"
                  aria-label="이완기 혈압 (mmHg)"
                  value={formData.diastolicBp}
                  onChange={handleChange}
                  placeholder="이완기"
                  min="30"
                  max="150"
                />
              </div>
              <small>정상: 90-140 / 60-90</small>
            </div>

            <div className={styles.formGroup}>
              <label>맥박 (회/분)</label>
              <input
                type="number"
                name="heartRate"
                  aria-label="맥박 (회/분)"
                value={formData.heartRate}
                onChange={handleChange}
                placeholder="60-100"
                min="30"
                max="250"
              />
              <small>정상: 60-100</small>
            </div>

            <div className={styles.formGroup}>
              <label>체온 (°C)</label>
              <input
                type="number"
                step="0.1"
                name="bodyTemp"
                  aria-label="체온 (°C)"
                value={formData.bodyTemp}
                onChange={handleChange}
                placeholder="36.5"
                min="35.0"
                max="42.0"
              />
              <small>정상: 36.1-37.2</small>
            </div>

            <div className={styles.formGroup}>
              <label>호흡 (회/분)</label>
              <input
                type="number"
                name="respiratoryRate"
                  aria-label="호흡 (회/분)"
                value={formData.respiratoryRate}
                onChange={handleChange}
                placeholder="12-20"
                min="5"
                max="60"
              />
              <small>정상: 12-20</small>
            </div>

            <div className={styles.formGroup}>
              <label>산소포화도 (%)</label>
              <input
                type="number"
                name="spo2"
                  aria-label="산소포화도 (%)"
                value={formData.spo2}
                onChange={handleChange}
                placeholder="95-100"
                min="70"
                max="100"
              />
              <small>정상: 95-100</small>
            </div>
          </div>

          <div className={styles.formActions}>
            <button type="submit" className={styles.submitButton}>
              {editingVitalId ? '수정' : '등록'}
            </button>
            {editingVitalId && (
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
      {showChart && vitals.length > 0 && (
        <TimeSeriesChart
          data={prepareChartData()}
          metrics={vitalMetrics}
          title="V/S 추이"
        />
      )}

      {/* 바이탈 이력 목록 */}
      {!showChart && <div className={styles.vitalList}>
        {vitals.length === 0 ? (
          <div className={styles.empty}>등록된 V/S 정보가 없습니다</div>
        ) : (
          sortRecords(vitals, 'measuredAt', sortOrder).map(vital => {
            const prevVital = previousVitals.get(vital.id);
            
            return (
              <div key={vital.id} className={styles.vitalItem}>
                <div className={styles.vitalHeader}>
                  <span className={styles.time}>{formatTime(vital.measuredAt)}</span>
                  <div className={styles.vitalHeaderRight}>
                    <span className={styles.nurse}>{vital.nurseName}</span>
                    {vital.canEdit && (
                      <button 
                        className={styles.editButton}
                        onClick={() => handleEdit(vital)}
                        title="V/S 수정"
                      >
                        V/S 수정
                      </button>
                    )}
                  </div>
                </div>

                <div className={styles.vitalData}>
                  {vital.systolicBp && vital.diastolicBp && (
                    <div className={styles.dataItem}>
                      <span className={styles.label}>혈압</span>
                      <span className={getStatusClass('systolicBp', vital.systolicBp)}>
                        {vital.systolicBp}/{vital.diastolicBp}
                        {getChangeIndicator(
                          vital.systolicBp,
                          prevVital?.systolicBp
                        )}
                      </span>
                      <span className={styles.unit}>mmHg</span>
                    </div>
                  )}

                  {vital.heartRate && (
                    <div className={styles.dataItem}>
                      <span className={styles.label}>맥박</span>
                      <span className={getStatusClass('heartRate', vital.heartRate)}>
                        {vital.heartRate}
                        {getChangeIndicator(vital.heartRate, prevVital?.heartRate)}
                      </span>
                      <span className={styles.unit}>회/분</span>
                    </div>
                  )}

                  {vital.bodyTemp && (
                    <div className={styles.dataItem}>
                      <span className={styles.label}>체온</span>
                      <span className={getStatusClass('bodyTemp', vital.bodyTemp)}>
                        {vital.bodyTemp.toFixed(1)}
                        {getChangeIndicator(vital.bodyTemp, prevVital?.bodyTemp)}
                      </span>
                      <span className={styles.unit}>°C</span>
                    </div>
                  )}

                  {vital.respiratoryRate && (
                    <div className={styles.dataItem}>
                      <span className={styles.label}>호흡</span>
                      <span className={getStatusClass('respiratoryRate', vital.respiratoryRate)}>
                        {vital.respiratoryRate}
                        {getChangeIndicator(
                          vital.respiratoryRate,
                          prevVital?.respiratoryRate
                        )}
                      </span>
                      <span className={styles.unit}>회/분</span>
                    </div>
                  )}

                  {vital.spo2 && (
                    <div className={styles.dataItem}>
                      <span className={styles.label}>SpO2</span>
                      <span className={getStatusClass('spo2', vital.spo2)}>
                        {vital.spo2}
                        {getChangeIndicator(vital.spo2, prevVital?.spo2)}
                      </span>
                      <span className={styles.unit}>%</span>
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>}
    </div>
  );
};

export default VitalTab;
