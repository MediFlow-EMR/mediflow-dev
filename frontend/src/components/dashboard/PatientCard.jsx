import React from 'react';
import styles from './PatientCard.module.scss';

/**
 * 환자 카드 컴포넌트
 */
const PatientCard = ({ patient, isSelected, onClick }) => {
  const hasAllergy = patient.allergies && patient.allergies !== '없음';
  const hasVitals = patient.lastVitalTime;

  // 트리아지 레벨 클래스
  const getTriageClass = (level) => {
    return `${styles.triageBadge} ${styles[`level${level}`]}`;
  };

  return (
    <div
      className={`${styles.patientCard} ${isSelected ? styles.selected : ''}`}
      onClick={onClick}
    >
      <div className={styles.header}>
        <div className={styles.patientInfo}>
          <div className={styles.name}>
            {patient.name}
          </div>
          <div className={styles.meta}>
            {patient.chartNumber} | {patient.age}세 {patient.gender === 'M' ? '남' : '여'}
          </div>
        </div>

        <div className={styles.badges}>
          {hasAllergy && (
            <span className={styles.allergyBadge}>알러지</span>
          )}
          {patient.triageLevel && (
            <span className={getTriageClass(patient.triageLevel)}>
              T{patient.triageLevel}
            </span>
          )}
        </div>
      </div>

      <div className={styles.diagnosis}>
        {patient.diagnosis || '진단명 없음'}
      </div>

      {hasVitals ? (
        <div className={styles.vitals}>
          <div className={styles.vitalItem}>
            <span className={styles.label}>혈압</span>
            <span className={styles.value}>
              {patient.systolicBp}/{patient.diastolicBp}
            </span>
          </div>
          <div className={styles.vitalItem}>
            <span className={styles.label}>심박수</span>
            <span className={styles.value}>{patient.heartRate}</span>
          </div>
          <div className={styles.vitalItem}>
            <span className={styles.label}>체온</span>
            <span className={styles.value}>{patient.bodyTemp}°C</span>
          </div>
          <div className={styles.vitalItem}>
            <span className={styles.label}>SpO2</span>
            <span className={styles.value}>{patient.spo2}%</span>
          </div>
          <div className={styles.vitalItem}>
            <span className={styles.label}>측정시간</span>
            <span className={styles.value}>
              {new Date(patient.lastVitalTime).toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit',
              })}
            </span>
          </div>
        </div>
      ) : (
        <div className={styles.noVitals}>바이탈 기록 없음</div>
      )}
    </div>
  );
};

export default PatientCard;
