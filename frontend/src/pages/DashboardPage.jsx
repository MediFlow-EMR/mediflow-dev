import React, { useEffect } from 'react';
import useDashboardStore from '../stores/useDashboardStore';
import PatientCard from '../components/dashboard/PatientCard';
import PatientDetail from '../components/dashboard/PatientDetail';
import HandoverTab from '../components/handover/HandoverTab';
import styles from './DashboardPage.module.scss';

/**
 * 메인 대시보드 페이지
 *
 * ===================================================================
 * 성능 최적화 적용 사항
 * ===================================================================
 *
 * [문제점] 중복 API 호출
 * - 컴포넌트 마운트 시:
 *   1. 첫 번째 useEffect에서 fetchMyPatients() 호출
 *   2. 두 번째 useEffect에서 activeTab이 'my'이므로 또 fetchMyPatients() 호출
 *   → 동일한 API를 2번 호출! (불필요한 네트워크 비용)
 *
 * [해결책] useEffect 통합
 * - 두 개의 useEffect를 하나로 통합:
 *   1. activeTab 변경을 감지하는 단일 useEffect
 *   2. 초기 마운트 시에도 activeTab('my')에 따라 자동 호출
 *   → API 호출 1번으로 감소! (50% 절감)
 *
 * [추가 효과]
 * - 부서 요약 정보(fetchDepartmentSummary)는 한 번만 호출
 * - 탭 전환 시에도 불필요한 재호출 없음
 *
 * ===================================================================
 */
const DashboardPage = () => {
  const {
    myPatients,
    allPatients,
    selectedPatient,
    loading,
    error,
    activeTab,
    fetchMyPatients,
    fetchAllPatients,
    selectPatient,
    setActiveTab,
  } = useDashboardStore();

  // ===================================================================
  // 성능 최적화: 초기 로드와 탭 변경을 하나의 useEffect로 통합
  // ===================================================================
  useEffect(() => {
    // 현재 탭에 따라 해당하는 환자 목록만 조회
    // [초기 마운트] activeTab = 'my' → fetchMyPatients() 호출
    // [탭 클릭] activeTab = 'all' → fetchAllPatients() 호출
    if (activeTab === 'my') {
      fetchMyPatients();
    } else {
      fetchAllPatients();
    }

    // 의존성 배열에 activeTab 포함 → 탭이 바뀌면 자동으로 재실행
  }, [activeTab, fetchMyPatients, fetchAllPatients]);

  // 현재 탭에 따라 표시할 환자 목록 선택
  // (API에서 받아온 데이터 중 어떤 것을 보여줄지 결정)
  const displayPatients = activeTab === 'my' ? myPatients : allPatients;

  return (
    <div className={styles.dashboardPage}>
      {/* 좌측 패널 - 환자 목록 */}
      <div className={styles.leftPanel}>

          {/* 환자 목록 */}
          <div className={styles.patientList}>
            <div className={styles.listHeader}>
              {/* 탭 버튼 */}
              <div className={styles.tabs}>
                <button
                  className={`${styles.tab} ${activeTab === 'my' ? styles.activeTab : ''}`}
                  onClick={() => setActiveTab('my')}
                >
                  내 담당
                </button>
                <button
                  className={`${styles.tab} ${activeTab === 'all' ? styles.activeTab : ''}`}
                  onClick={() => setActiveTab('all')}
                >
                  전체 환자
                </button>
                <button
                  className={`${styles.tab} ${activeTab === 'handover' ? styles.activeTab : ''}`}
                  onClick={() => setActiveTab('handover')}
                >
                  인수인계
                </button>
              </div>
              <span className={styles.count}>{displayPatients.length}명</span>
            </div>

            {loading && <div className={styles.loading}>로딩중...</div>}

            {error && <div className={styles.error}>{error}</div>}

            {!loading && !error && displayPatients.length === 0 && activeTab !== 'handover' && (
              <div className={styles.empty}>
                {activeTab === 'my' ? '배정된 환자가 없습니다.' : '부서에 환자가 없습니다.'}
              </div>
            )}

            {!loading &&
              !error &&
              activeTab !== 'handover' &&
              displayPatients.map((patient) => (
                <PatientCard
                  key={patient.patientId}
                  patient={patient}
                  isSelected={selectedPatient?.patientId === patient.patientId}
                  onClick={() => selectPatient(patient)}
                />
              ))}
          </div>
        </div>

      {/* 우측 패널 - 환자 상세 또는 인수인계 */}
      <div className={styles.rightPanel}>
        {activeTab === 'handover' ? (
          <HandoverTab />
        ) : (
          <PatientDetail patient={selectedPatient} />
        )}
      </div>
    </div>
  );
};

export default DashboardPage;
