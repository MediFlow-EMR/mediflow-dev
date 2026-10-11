import RequestError from '../components/common/RequestError';
import { useNavigationGuard } from '../stores/navigationGuard';
import FilterBar from '../components/common/FilterBar';
import { filterPatients, patientFilterDefaults } from '../utils/filters';
import React, { useEffect, useMemo, useState } from 'react';
import useDashboardStore from '../stores/useDashboardStore';
import PatientCard from '../components/dashboard/PatientCard';
import PatientDetail from '../components/dashboard/PatientDetail';
import HandoverTab from '../components/handover/HandoverTab';
import styles from './DashboardPage.module.scss';

const DashboardPage = () => {
  const PATIENTS_PER_PAGE = 5;
  const [patientPage, setPatientPage] = useState(1);
  const [filters, setFilters] = useState(patientFilterDefaults);
  const dirty = useNavigationGuard(state => Object.keys(state.owners).length > 0);
  const {
    selectedDate,
    selectedShiftType,
    myPatients,
    allPatients,
    selectedPatient,
    loading,
    error,
    summaryError,
    activeTab,
    fetchMyPatients,
    fetchAllPatients,
    fetchDepartmentSummary,
    selectPatient,
    setActiveTab,
  } = useDashboardStore();
  useEffect(() => {
    fetchDepartmentSummary();
  }, [fetchDepartmentSummary, selectedDate, selectedShiftType]);
  useEffect(() => {
    if (activeTab === 'my' || activeTab === 'handover') {
      fetchMyPatients();
    } else if (activeTab === 'all') {
      fetchAllPatients();
    }
  }, [activeTab, fetchMyPatients, fetchAllPatients, selectedDate, selectedShiftType]);
  const sourcePatients = activeTab === 'my' ? myPatients : allPatients;
  const displayPatients = useMemo(() => filterPatients(sourcePatients, filters), [sourcePatients, filters]);
  const rooms = [...new Set(sourcePatients.map(patient => String(patient.roomNumber ?? '')).filter(Boolean))].sort();
  const totalPatientPages = Math.max(1, Math.ceil(displayPatients.length / PATIENTS_PER_PAGE));
  const visiblePatients = useMemo(() => {
    const start = (patientPage - 1) * PATIENTS_PER_PAGE;
    return displayPatients.slice(start, start + PATIENTS_PER_PAGE);
  }, [displayPatients, patientPage]);

  useEffect(() => {
    setPatientPage(1);
  }, [activeTab, filters, selectedDate, selectedShiftType]);

  useEffect(() => {
    if (patientPage > totalPatientPages) {
      setPatientPage(totalPatientPages);
    }
  }, [patientPage, totalPatientPages]);

  useEffect(() => {
    if (activeTab === 'handover' || loading || dirty) return;
    if (!displayPatients.some(patient => patient.patientId === selectedPatient?.patientId)) {
      selectPatient(displayPatients[0] || null);
    }
  }, [activeTab, displayPatients, loading, dirty, selectedPatient?.patientId, selectPatient]);

  const changeTab = tab => {
    if (tab !== activeTab) useNavigationGuard.getState().requestNavigation(() => setActiveTab(tab));
  };

  return (
    <div className={`${styles.dashboardPage} ${activeTab === 'handover' ? styles.handoverMode : ''}`}>
      {/* 좌측 패널 - 환자 목록 */}
      <div className={styles.leftPanel}>

          {/* 환자 목록 */}
          <div className={styles.patientList}>
            <div className={styles.listHeader}>
              {/* 탭 버튼 */}
              <div className={styles.tabs}>
                <button
                  className={`${styles.tab} ${activeTab === 'my' ? styles.activeTab : ''}`}
                  onClick={() => changeTab('my')}
                >
                  내 담당
                </button>
                <button
                  className={`${styles.tab} ${activeTab === 'all' ? styles.activeTab : ''}`}
                  onClick={() => changeTab('all')}
                >
                  전체 환자
                </button>
                <button
                  className={`${styles.tab} ${activeTab === 'handover' ? styles.activeTab : ''}`}
                  onClick={() => changeTab('handover')}
                >
                  인수인계
                </button>
              </div>
            </div>

            {activeTab !== 'handover' && <FilterBar label="환자" values={filters} onChange={next => { useNavigationGuard.getState().requestNavigation(() => setFilters(next)); }}
              onReset={() => { useNavigationGuard.getState().requestNavigation(() => setFilters(patientFilterDefaults)); }} searchPlaceholder="이름 · 환자번호 · 진단"
              count={displayPatients.length} total={sourcePatients.length} fields={[
                { key: 'room', label: '병실', type: 'select', options: rooms.map(room => ({ value: room, label: `${room}호` })) },
                { key: 'allergy', label: '알레르기 있음', type: 'checkbox' },
                { key: 'fallRisk', label: '낙상 고위험', type: 'checkbox' },
                { key: 'isolation', label: '격리 환자', type: 'checkbox' },
              ]} />}

            {loading && <div className={styles.loading}>로딩중...</div>}

            <RequestError className={styles.error} message={error || summaryError} onRetry={() => {
              fetchDepartmentSummary();
              if (activeTab === 'my') fetchMyPatients();
              else if (activeTab === 'all') fetchAllPatients();
            }} />

            {!loading && !error && displayPatients.length === 0 && activeTab !== 'handover' && (
              <div className={styles.empty}>
                {sourcePatients.length ? '필터에 맞는 환자가 없습니다.' : activeTab === 'my' ? '배정된 환자가 없습니다.' : '부서에 환자가 없습니다.'}
              </div>
            )}

            {!loading &&
              !error &&
              activeTab !== 'handover' &&
              visiblePatients.map((patient) => (
                <PatientCard
                  key={patient.patientId}
                  patient={patient}
                  isSelected={selectedPatient?.patientId === patient.patientId}
                  onClick={() => { if (patient.patientId !== selectedPatient?.patientId) useNavigationGuard.getState().requestNavigation(() => selectPatient(patient)); }}
                />
              ))}

            {!loading && !error && activeTab !== 'handover' && displayPatients.length > 0 && (
              <div className={styles.pagination}>
                <button
                  type="button"
                  aria-label="이전 환자 목록"
                  onClick={() => setPatientPage((page) => Math.max(1, page - 1))}
                  disabled={patientPage === 1}
                >
                  ‹
                </button>
                <div className={styles.pageStatus}>
                  <strong>{patientPage}</strong> / {totalPatientPages}
                  <span>
                    {((patientPage - 1) * PATIENTS_PER_PAGE) + 1}–
                    {Math.min(patientPage * PATIENTS_PER_PAGE, displayPatients.length)} / {displayPatients.length}명
                  </span>
                </div>
                <button
                  type="button"
                  aria-label="다음 환자 목록"
                  onClick={() => setPatientPage((page) => Math.min(totalPatientPages, page + 1))}
                  disabled={patientPage === totalPatientPages}
                >
                  ›
                </button>
              </div>
            )}
          </div>
        </div>

      {/* 우측 패널 - 환자 상세 또는 인수인계 */}
      <div className={styles.rightPanel}>
        {activeTab === 'handover' ? (
          <HandoverTab key={`${selectedDate}:${selectedShiftType}`} />
        ) : (
          <PatientDetail key={`${selectedDate}:${selectedShiftType}`} patient={selectedPatient} />
        )}
      </div>
    </div>
  );
};

export default DashboardPage;
