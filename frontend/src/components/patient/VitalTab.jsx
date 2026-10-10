import { useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import TimeSeriesChart from '../common/TimeSeriesChart';
import styles from './VitalTab.module.scss';

/**
 * 바이탈 사인 탭 컴포넌트
 */
const VitalTab = ({ patientId }) => {
  // 바이탈 메트릭 정의
  const vitalMetrics = [
    { key: 'systolicBp', label: '수축기 혈압', color: '#ef4444', unit: 'mmHg' },
    { key: 'diastolicBp', label: '이완기 혈압', color: '#f97316', unit: 'mmHg' },
    { key: 'heartRate', label: '맥박', color: '#ec4899', unit: '회/분' },
    { key: 'bodyTemp', label: '체온', color: '#8b5cf6', unit: '°C' },
    { key: 'respiratoryRate', label: '호흡', color: '#3b82f6', unit: '회/분' },
    { key: 'spo2', label: 'SpO2', color: '#10b981', unit: '%' },
  ];
  const [vitals, setVitals] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);

  // 현재 시간 (한국 시간)
  const getCurrentDateTime = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  // 바이탈 입력 폼 상태
  const [formData, setFormData] = useState({
    systolicBp: '',
    diastolicBp: '',
    heartRate: '',
    bodyTemp: '',
    respiratoryRate: '',
    spo2: '',
    measuredAt: '', // 측정시간
  });

  // 수정 모드 상태
  const [editingVitalId, setEditingVitalId] = useState(null);
  
  // 차트 표시 상태
  const [showChart, setShowChart] = useState(false);

  // 바이탈 목록 조회
  const fetchVitals = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(`/vitals/patient/${patientId}`);
      setVitals(response.data.data || []);
    } catch (err) {
      console.error('바이탈 목록 조회 실패:', err);
      setError('바이탈 목록을 불러올 수 없습니다');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (patientId) {
      fetchVitals();
    }
  }, [patientId]);

  // 입력값 변경 핸들러
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // 바이탈 등록/수정
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    try {
      const payload = {
        patientId,
        systolicBp: formData.systolicBp ? parseInt(formData.systolicBp) : null,
        diastolicBp: formData.diastolicBp ? parseInt(formData.diastolicBp) : null,
        heartRate: formData.heartRate ? parseInt(formData.heartRate) : null,
        bodyTemp: formData.bodyTemp ? parseFloat(formData.bodyTemp) : null,
        respiratoryRate: formData.respiratoryRate ? parseInt(formData.respiratoryRate) : null,
        spo2: formData.spo2 ? parseInt(formData.spo2) : null,
        measuredAt: formData.measuredAt || null,
      };

      if (editingVitalId) {
        // 수정
        await apiClient.put(`/vitals/${editingVitalId}`, payload);
      } else {
        // 등록
        await apiClient.post('/vitals', payload);
      }
      
      // 폼 초기화 및 목록 새로고침
      resetForm();
      fetchVitals();
    } catch (err) {
      console.error('바이탈 저장 실패:', err);
      setError(err.response?.data?.message || '바이탈 저장에 실패했습니다');
    }
  };

  // 폼 초기화
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
    setShowForm(false);
  };

  // 수정 모드로 전환
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
    setShowForm(true);
    setShowChart(false); // 차트 닫기
  };

  // 차트 데이터 준비
  const prepareChartData = () => {
    return vitals
      .slice()
      .reverse() // 시간순 정렬 (오래된 것부터)
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

  // 시간 포맷 (한국 시간대)
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
            className={styles.addButton}
            onClick={() => {
              if (showForm) {
                resetForm();
              } else {
                setShowForm(true);
              }
            }}
          >
            {showForm ? '취소' : '+ V/S 등록'}
          </button>
        </div>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {/* 바이탈 입력 폼 */}
      {showForm && (
        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formGroup}>
            <label>측정시간</label>
            <input
              type="datetime-local"
              name="measuredAt"
              value={formData.measuredAt}
              onChange={handleChange}
              max={getCurrentDateTime()}
            />
            <small>측정 시간을 입력하세요 (미입력 시 현재 시간, 미래 시간 입력 불가)</small>
          </div>

          <div className={styles.formGrid}>
            <div className={styles.formGroup}>
              <label>혈압 (mmHg)</label>
              <div className={styles.bpInput}>
                <input
                  type="number"
                  name="systolicBp"
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
          vitals.map((vital, index) => {
            const prevVital = vitals[index + 1];
            
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
import { formatDateTime } from '../../utils/dateFormat';
