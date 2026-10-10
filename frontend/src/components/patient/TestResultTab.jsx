import { useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import styles from './TestResultTab.module.scss';

/**
 * 검사 결과 탭 컴포넌트 (조회 전용)
 */
const TestResultTab = ({ patientId }) => {
  const [testResults, setTestResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filterType, setFilterType] = useState('ALL');

  // 검사 유형 라벨
  const testTypeLabels = {
    BLOOD: '혈액검사',
    URINE: '소변검사',
    CT: 'CT',
    MRI: 'MRI',
    XRAY: 'X-Ray',
    ULTRASOUND: '초음파',
  };

  // 검사 결과 조회
  const fetchTestResults = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(`/test-results/patient/${patientId}`);
      setTestResults(response.data.data || []);
    } catch (err) {
      console.error('검사 결과 조회 실패:', err);
      setError('검사 결과를 불러올 수 없습니다');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (patientId) {
      fetchTestResults();
    }
  }, [patientId]);

  // 시간 포맷
  const formatDateTime = (dateTime) => {
    return formatRecordDateTime(dateTime);
  };

  // 필터링된 검사 결과
  const filteredResults = filterType === 'ALL' 
    ? testResults 
    : testResults.filter(result => result.testType === filterType);

  if (loading && testResults.length === 0) {
    return <div className={styles.loading}>검사 결과를 불러오는 중...</div>;
  }

  return (
    <div className={styles.testResultTab}>
      <div className={styles.header}>
        <h3>검사 결과</h3>
        <div className={styles.filters}>
          <button
            className={`${styles.filterButton} ${filterType === 'ALL' ? styles.active : ''}`}
            onClick={() => setFilterType('ALL')}
          >
            전체
          </button>
          <button
            className={`${styles.filterButton} ${filterType === 'BLOOD' ? styles.active : ''}`}
            onClick={() => setFilterType('BLOOD')}
          >
            혈액
          </button>
          <button
            className={`${styles.filterButton} ${filterType === 'URINE' ? styles.active : ''}`}
            onClick={() => setFilterType('URINE')}
          >
            소변
          </button>
          <button
            className={`${styles.filterButton} ${filterType === 'XRAY' ? styles.active : ''}`}
            onClick={() => setFilterType('XRAY')}
          >
            X-Ray
          </button>
          <button
            className={`${styles.filterButton} ${filterType === 'CT' ? styles.active : ''}`}
            onClick={() => setFilterType('CT')}
          >
            CT
          </button>
          <button
            className={`${styles.filterButton} ${filterType === 'ULTRASOUND' ? styles.active : ''}`}
            onClick={() => setFilterType('ULTRASOUND')}
          >
            초음파
          </button>
        </div>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {/* 검사 결과 목록 */}
      <div className={styles.resultList}>
        {filteredResults.length === 0 ? (
          <div className={styles.empty}>
            {filterType === 'ALL' ? '등록된 검사 결과가 없습니다' : `${testTypeLabels[filterType]} 결과가 없습니다`}
          </div>
        ) : (
          filteredResults.map((result) => (
            <div key={result.id} className={`${styles.resultItem} ${result.isAbnormal ? styles.abnormal : ''}`}>
              <div className={styles.resultHeader}>
                <div className={styles.resultHeaderLeft}>
                  <span className={styles.testType}>{testTypeLabels[result.testType]}</span>
                  <span className={styles.testName}>{result.testName}</span>
                  {result.isAbnormal && <span className={styles.abnormalBadge}>이상</span>}
                </div>
                <div className={styles.resultHeaderRight}>
                  <span className={styles.resultDate}>{formatDateTime(result.resultDate)}</span>
                </div>
              </div>

              <div className={styles.resultBody}>
                <div className={styles.resultValue}>
                  <span className={styles.label}>결과</span>
                  <span className={`${styles.value} ${result.isAbnormal ? styles.abnormalValue : ''}`}>
                    {result.resultValue}
                  </span>
                </div>
                {result.referenceRange && (
                  <div className={styles.referenceRange}>
                    <span className={styles.label}>참고치</span>
                    <span className={styles.value}>{result.referenceRange}</span>
                  </div>
                )}
              </div>

              {result.nurseName && (
                <div className={styles.resultFooter}>
                  <span className={styles.nurse}>확인: {result.nurseName}</span>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default TestResultTab;
import { formatDateTime as formatRecordDateTime } from '../../utils/dateFormat';
