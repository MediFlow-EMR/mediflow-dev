import { formatDateTime as formatRecordDateTime } from '../../utils/dateFormat';
import useRecordList from '../../hooks/useRecordList';
import RequestError from '../common/RequestError';
import SortControl from '../common/SortControl';
import { sortRecords } from '../../utils/sortRecords';
import { useState } from 'react';
import styles from './MedicationTab.module.scss';

/**
 * 투약 기록 탭 컴포넌트
 */
const MedicationTab = ({ patientId }) => {
  const { items: medications, loading, loadError, refresh: fetchMedications } = useRecordList(`/medications/patient/${patientId}`, '투약 목록을 불러올 수 없습니다');

  const [sortOrder, setSortOrder] = useState('desc');

  // 투약 경로 옵션
  const routes = [
    { value: 'PO', label: '경구 (PO)' },
    { value: 'IV', label: '정맥 (IV)' },
    { value: 'IM', label: '근육 (IM)' },
    { value: 'SC', label: '피하 (SC)' },
    { value: 'TOPICAL', label: '국소' },
    { value: 'INHALATION', label: '흡입' },
  ];

  const formatTime = (dateTime) => {
    return formatRecordDateTime(dateTime);
  };

  if (loading && medications.length === 0) {
    return <div className={styles.loading}>투약 정보를 불러오는 중...</div>;
  }

  return (
    <div className={styles.medicationTab}>
      <div className={styles.header}>
        <h3>투약 기록</h3>
      </div>
      <div className="record-toolbar"><SortControl value={sortOrder} onChange={setSortOrder} basis="투약일시" /></div>

      <RequestError className={styles.error} message={loadError} onRetry={loadError ? fetchMedications : undefined} />

      {/* 투약 이력 목록 */}
      <div className={styles.medicationList}>
        {medications.length === 0 ? (
          <div className={styles.empty}>등록된 투약 기록이 없습니다</div>
        ) : (
          sortRecords(medications, 'administeredAt', sortOrder).map((medication) => (
            <div key={medication.id} className={styles.medicationItem}>
              <div className={styles.medicationHeader}>
                <div className={styles.medicationHeaderLeft}>
                  <span className={styles.drugName}>{medication.drugName}</span>
                  <span className={styles.route}>{routes.find(r => r.value === medication.route)?.label || medication.route}</span>
                </div>
                <div className={styles.medicationHeaderRight}>
                  <span className={styles.time}>{formatTime(medication.administeredAt)}</span>
                  <span className={styles.nurse}>{medication.nurseName}</span>
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
