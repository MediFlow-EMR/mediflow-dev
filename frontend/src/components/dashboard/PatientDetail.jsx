import { useNavigationGuard } from '../../stores/navigationGuard';
import { useEffect, useState } from 'react';
import apiClient from '../../services/apiClient';
import OrderTab from '../patient/OrderTab';
import NursingNoteTab from '../patient/NursingNoteTab';
import VitalTab from '../patient/VitalTab';
import IntakeOutputTab from '../patient/IntakeOutputTab';
import MedicationTab from '../patient/MedicationTab';
import TestResultTab from '../patient/TestResultTab';
import styles from './PatientDetail.module.scss';
/**
 * 환자 상세 정보 컴포넌트
 */
const PatientDetail = ({ patient }) => {
  const [activeTab, setActiveTab] = useState('orders');
  const [todayNote, setTodayNote] = useState(null);

  useEffect(() => {
    let active = true;

    const fetchTodayNote = async () => {
      if (!patient?.patientId) return;
      try {
        const response = await apiClient.get(`/nursing-notes/patient/${patient.patientId}`);
        const notes = response.data.data || [];
        const today = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Seoul' });
        const todayNotes = notes.filter((note) => (
          new Date(note.createdAt).toLocaleDateString('en-CA', { timeZone: 'Asia/Seoul' }) === today
        ));
        const selected = todayNotes.find((note) => note.isImportant) || todayNotes[0] || null;
        if (active) setTodayNote(selected);
      } catch {
        if (active) setTodayNote(null);
      }
    };

    setTodayNote(null);
    fetchTodayNote();
    return () => { active = false; };
  }, [patient?.patientId]);

  if (!patient) {
    return (
      <div className={styles.patientDetail}>
        <div className={styles.emptyState}>
          <div className={styles.message}>환자를 선택해주세요</div>
        </div>
      </div>
    );
  }

  const tabs = [
    { id: 'orders', label: '오더조회' },
    { id: 'notes', label: '간호기록' },
    { id: 'vitals', label: 'V/S' },
    { id: 'io', label: '섭취배설' },
    { id: 'medications', label: '투약' },
    { id: 'testResults', label: '검사결과' },
  ];

  const hasAllergy = patient.allergies && patient.allergies !== '없음';
  return (
    <div className={styles.patientDetail}>
      <div className={styles.header}>
        <div className={styles.identityRow}>
          <div>
            <div className={styles.patientName}>
              {patient.name}
              <span className={styles.demographics}>
                {patient.age}세 · {patient.gender === 'M' ? '남' : '여'}
              </span>
            </div>
            <div className={styles.patientMeta}>
              MRN {patient.chartNumber} · {patient.departmentName || '부서 미지정'}
            </div>
          </div>
          <div className={styles.safetyBadges}>
            {hasAllergy ? (
              <span className={styles.dangerBadge} title={patient.allergies}>
                약물 알레르기 · {patient.allergies}
              </span>
            ) : (
              <span className={styles.neutralBadge}>알레르기 없음</span>
            )}
            {patient.triageLevel && (
              <span className={styles.triageBadge}>KTAS {patient.triageLevel}</span>
            )}
            {patient.fallRisk && (
              <span className={styles.warningBadge}>낙상 고위험</span>
            )}
            {patient.isolationType && (
              <span className={styles.warningBadge}>격리 · {patient.isolationType}</span>
            )}
          </div>
        </div>
        <div className={styles.clinicalStrip}>
          <div>
            <span>주진단</span>
            <strong>{patient.diagnosis || '미등록'}</strong>
          </div>
          <div>
            <span>입원 위치 · 주치의</span>
            <strong>
              {patient.roomNumber && patient.bedNumber
                ? `${patient.roomNumber}호 ${patient.bedNumber}번 침상`
                : '위치 미등록'}
              {' · '}{patient.attendingPhysician || '주치의 미등록'}
            </strong>
          </div>
          <div className={styles.specialNote}>
            <span>오늘 특이사항</span>
            <strong title={todayNote?.plainText || todayNote?.content || '특이사항 없음'}>
              {todayNote?.isImportant && <em>중요 · </em>}
              {todayNote?.plainText || todayNote?.content || '특이사항 없음'}
            </strong>
          </div>
        </div>
      </div>

      <div className={styles.tabs}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            className={`${styles.tab} ${activeTab === tab.id ? styles.active : ''}`}
            onClick={() => { if (tab.id !== activeTab) useNavigationGuard.getState().requestNavigation(() => setActiveTab(tab.id)); }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className={styles.content}>
        {activeTab === 'orders' && <OrderTab key={patient.patientId} patientId={patient.patientId} />}
        {activeTab === 'notes' && <NursingNoteTab key={patient.patientId} patientId={patient.patientId} />}
        {activeTab === 'vitals' && <VitalTab key={patient.patientId} patientId={patient.patientId} />}
        {activeTab === 'io' && <IntakeOutputTab key={patient.patientId} patientId={patient.patientId} />}
        {activeTab === 'medications' && <MedicationTab key={patient.patientId} patientId={patient.patientId} />}
        {activeTab === 'testResults' && <TestResultTab key={patient.patientId} patientId={patient.patientId} />}
        {activeTab === 'handover' && (
          <div className={styles.comingSoon}>
            {tabs.find((t) => t.id === activeTab)?.label} 기능은 곧 추가됩니다.
          </div>
        )}
      </div>
    </div>
  );
};

export default PatientDetail;
