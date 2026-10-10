import React from 'react';
import styles from './PatientCard.module.scss';

/**
 * 환자 카드 컴포넌트
 */
const PatientCard = ({ patient, isSelected, onClick }) => {
  const hasAllergy = patient.allergies && patient.allergies !== '없음';
  const hasVitals = patient.lastVitalTime;
  const formatVitalTime = (value) => {
    const date = new Date(value);
    const pad = (number) => String(number).padStart(2, '0');
    return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  };

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
            <span className={styles.allergyBadge} title={patient.allergies}>알레르기</span>
          )}
          {patient.triageLevel && (
            <span className={getTriageClass(patient.triageLevel)}>
              T{patient.triageLevel}
            </span>
          )}
        </div>
      </div>

      <div className={styles.contextLine}>
        <span className={styles.department}>{patient.departmentName || '부서 미지정'}</span>
        {patient.roomNumber && patient.bedNumber && (
          <span className={styles.location}>{patient.roomNumber}-{patient.bedNumber}</span>
        )}
        <span className={styles.diagnosis}>{patient.diagnosis || '진단명 없음'}</span>
      </div>

      {hasVitals ? (
        <div className={styles.vitals}>
          <div className={styles.vitalItem}>
            <span className={styles.label}>BP</span>
            <span className={styles.value}>
              {patient.systolicBp}/{patient.diastolicBp}
            </span>
          </div>
          <div className={styles.vitalItem}>
            <span className={styles.label}>PR</span>
            <span className={styles.value}>{patient.heartRate}</span>
          </div>
          <div className={styles.vitalItem}>
            <span className={styles.label}>RR</span>
            <span className={styles.value}>{patient.respiratoryRate}</span>
          </div>
          <div className={styles.vitalItem}>
            <span className={styles.label}>SpO2</span>
            <span className={styles.value}>{patient.spo2}%</span>
          </div>
          <span className={styles.measuredAt}>
            {formatVitalTime(patient.lastVitalTime)}
          </span>
        </div>
      ) : (
        <div className={styles.noVitals}>V/S 기록 없음</div>
      )}
    </div>
  );
};

export default PatientCard;
