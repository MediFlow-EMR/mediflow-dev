import { useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import styles from './MedicationTab.module.scss';

/**
 * 투약 기록 탭 컴포넌트
 */
const MedicationTab = ({ patientId }) => {
  const [medications, setMedications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // 투약 경로 옵션
  const routes = [
    { value: 'PO', label: '경구 (PO)' },
    { value: 'IV', label: '정맥 (IV)' },
    { value: 'IM', label: '근육 (IM)' },
    { value: 'SC', label: '피하 (SC)' },
    { value: 'TOPICAL', label: '국소' },
    { value: 'INHALATION', label: '흡입' },
  ];


  // 투약 목록 조회
  const fetchMedications = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiClient.get(`/medications/patient/${patientId}`);
      setMedications(response.data.data || []);
    } catch (err) {
      console.error('투약 목록 조회 실패:', err);
      setError('투약 목록을 불러올 수 없습니다');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (patientId) {
      fetchMedications();
    }
  }, [patientId]);

  // 수정 모드로 전환 (현재는 투약 등록이 오더탭에서만 가능하므로 비활성화)
  const handleEdit = (medication) => {
    // 투약 수정 기능은 향후 구현 예정
    console.log('투약 수정:', medication);
    alert('투약 수정 기능은 현재 지원되지 않습니다.');
  };

  // 시간 포맷
  const formatTime = (dateTime) => {
    const date = new Date(dateTime);
    return date.toLocaleString('ko-KR', {
      timeZone: 'Asia/Seoul',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    });
  };

  if (loading && medications.length === 0) {
    return <div className={styles.loading}>투약 정보를 불러오는 중...</div>;
  }

  return (
    <div className={styles.medicationTab}>
      <div className={styles.header}>
        <h3>투약 기록</h3>
      </div>

      {error && <div className={styles.error}>{error}</div>}


      {/* 투약 이력 목록 */}
      <div className={styles.medicationList}>
        {medications.length === 0 ? (
          <div className={styles.empty}>등록된 투약 기록이 없습니다</div>
        ) : (
          medications.map((medication) => (
            <div key={medication.id} className={styles.medicationItem}>
              <div className={styles.medicationHeader}>
                <div className={styles.medicationHeaderLeft}>
                  <span className={styles.drugName}>{medication.drugName}</span>
                  <span className={styles.route}>{routes.find(r => r.value === medication.route)?.label || medication.route}</span>
                </div>
                <div className={styles.medicationHeaderRight}>
                  <span className={styles.time}>{formatTime(medication.administeredAt)}</span>
                  <span className={styles.nurse}>{medication.nurseName}</span>
                  {medication.canEdit && (
                    <button 
                      className={styles.editButton}
                      onClick={() => handleEdit(medication)}
                      title="투약 수정"
                    >
                      수정
                    </button>
                  )}
                </div>
              </div>

              <div className={styles.medicationDetails}>
                {medication.dose && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>용량</span>
                    <span className={styles.detailValue}>{medication.dose}</span>
                  </div>
                )}
                {medication.frequency && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>빈도</span>
                    <span className={styles.detailValue}>{medication.frequency}</span>
                  </div>
                )}
                {medication.orderDoctor && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>처방의</span>
                    <span className={styles.detailValue}>{medication.orderDoctor}</span>
                  </div>
                )}
                {medication.orderId && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>연결 처방</span>
                    <span className={styles.detailValue}>#{medication.orderId}</span>
                  </div>
                )}
                {medication.notes && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>특이사항</span>
                    <span className={styles.detailValue}>{medication.notes}</span>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default MedicationTab;
