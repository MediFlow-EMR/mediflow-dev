import { useState, useEffect } from 'react';
import { HandoverService } from '../services/handoverService';
import { useAuthStore } from '../stores/authStore';
import useDashboardStore from '../stores/useDashboardStore';
import styles from './HandoverPage.module.scss';

/**
 * 인수인계 페이지 - 전체 환자의 인수인계를 한 번에 관리
 */
const HandoverPage = () => {
  const { user } = useAuthStore();
  const { departmentSummary } = useDashboardStore();
  const [previousHandover, setPreviousHandover] = useState(null);
  const [myHandover, setMyHandover] = useState('');
  const [loading, setLoading] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState(null);
  const [expandedPatients, setExpandedPatients] = useState(new Set());

  // 부서 ID와 현재 근무조 정보
  const departmentId = departmentSummary?.departmentId || 1;
  const currentShiftType = departmentSummary?.shiftType; // DAY, EVENING, NIGHT
  const currentShiftId = getShiftIdFromType(currentShiftType);
  const nextShiftType = getNextShiftType(currentShiftType);
  const prevShiftType = getPreviousShiftType(currentShiftType);

  function getShiftIdFromType(type) {
    const shiftMap = { 'DAY': 1, 'EVENING': 2, 'NIGHT': 3 };
    return shiftMap[type] || 1;
  }

  function getNextShiftType(currentType) {
    const nextShiftMap = { 'DAY': 'Evening', 'EVENING': 'Night', 'NIGHT': 'Day' };
    return nextShiftMap[currentType] || 'Evening';
  }

  function getPreviousShiftType(currentType) {
    const prevShiftMap = { 'DAY': 'Night', 'EVENING': 'Day', 'NIGHT': 'Evening' };
    return prevShiftMap[currentType] || 'Day';
  }

  useEffect(() => {
    loadPreviousHandover();
  }, [departmentId, currentShiftType]);

  // 이전 근무조의 인수인계 불러오기
  const loadPreviousHandover = async () => {
    if (!departmentId) return;

    setLoading(true);
    try {
      const response = await HandoverService.getHandoversByDepartment(departmentId);
      if (response.success && response.data.length > 0) {
        // 가장 최근 인수인계 가져오기
        const latest = response.data[0];
        setPreviousHandover(latest);
        // 자동으로 모든 환자 펼치기
        const patientNames = parsePatientNames(latest.aiSummary);
        const prefixedNames = patientNames.map(name => `prev-${name}`);
        setExpandedPatients(new Set(prefixedNames));
      }
    } catch (err) {
      console.error('이전 인수인계 조회 실패:', err);
    } finally {
      setLoading(false);
    }
  };

  // AI 자동 요약 생성
  const handleGenerateAiSummary = async () => {
    if (!currentShiftType) {
      setError('근무조 정보를 찾을 수 없습니다');
      return;
    }

    setIsGenerating(true);
    setError(null);

    try {
      const response = await HandoverService.generateAiSummary({
        departmentId,
        fromShiftId: currentShiftId,
        toShiftId: currentShiftId + 1 > 3 ? 1 : currentShiftId + 1,
      });

      if (response.success) {
        setMyHandover(response.data.aiSummary);
        // 모든 환자 펼치기
        const patientNames = parsePatientNames(response.data.aiSummary);
        const prefixedNames = patientNames.map(name => `my-${name}`);
        setExpandedPatients(new Set([...expandedPatients, ...prefixedNames]));
      } else {
        setError(response.message || 'AI 요약 생성에 실패했습니다');
      }
    } catch (err) {
      console.error('AI 요약 생성 실패:', err);
      setError(err.response?.data?.message || 'AI 요약 생성 중 오류가 발생했습니다');
    } finally {
      setIsGenerating(false);
    }
  };

  const parsePatientNames = (summary) => {
    if (!summary) return [];
    const lines = summary.split('\n');
    const names = [];
    lines.forEach(line => {
      const match = line.match(/^\[(.+?)\s*\(/);
      if (match) {
        names.push(match[1].trim());
      }
    });
    return names;
  };

  const parseAiSummary = (summary) => {
    if (!summary) return [];

    const lines = summary.split('\n');
    const patients = [];
    let currentPatient = null;

    lines.forEach((line) => {
      const patientMatch = line.match(/^\[(.+?)\s*\((.+?)\)\]/);

      if (patientMatch) {
        if (currentPatient) {
          patients.push(currentPatient);
        }
        currentPatient = {
          name: patientMatch[1].trim(),
          info: patientMatch[2].trim(),
          content: [],
          isImportant: line.includes('★') || line.includes('중요') || line.includes('이상'),
        };
      } else if (currentPatient && line.trim()) {
        currentPatient.content.push(line);
      }
    });

    if (currentPatient) {
      patients.push(currentPatient);
    }

    return patients;
  };

  const togglePatient = (patientName) => {
    const newExpanded = new Set(expandedPatients);
    if (newExpanded.has(patientName)) {
      newExpanded.delete(patientName);
    } else {
      newExpanded.add(patientName);
    }
    setExpandedPatients(newExpanded);
  };

  const previousPatients = parseAiSummary(previousHandover?.aiSummary);
  const myPatients = parseAiSummary(myHandover);

  if (loading) {
    return (
      <div className={styles.handoverPage}>
        <div className={styles.loading}>로딩 중...</div>
      </div>
    );
  }

  return (
    <div className={styles.handoverPage}>
      <div className={styles.header}>
        <h1>인수인계</h1>
        <p className={styles.description}>
          {currentShiftType ? (
            <>현재 <strong>{currentShiftType}</strong> 근무조입니다.
            이전 근무조의 인수인계를 확인하고, 다음 근무조를 위한 인수인계를 작성하세요.</>
          ) : (
            '근무조 정보를 불러오는 중...'
          )}
        </p>
      </div>

      <div className={styles.content}>
        {/* 이전 근무조 인수인계 (읽기) */}
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <h4>{prevShiftType} 근무조로부터 받은 인수인계</h4>
            {previousHandover && (
              <span className={styles.timestamp}>
                {previousHandover.handoverDate} {previousHandover.createdByName}
              </span>
            )}
          </div>

          {previousHandover ? (
            <div className={styles.patientList}>
              {previousPatients.map((patient, index) => (
                <div
                  key={index}
                  className={`${styles.patientCard} ${patient.isImportant ? styles.important : ''}`}
                >
                  <div
                    className={styles.patientHeader}
                    onClick={() => togglePatient(`prev-${patient.name}`)}
                  >
                    <div className={styles.patientInfo}>
                      {patient.isImportant && (
                        <span className={styles.importantBadge}>중요</span>
                      )}
                      <span className={styles.patientName}>{patient.name}</span>
                      <span className={styles.patientMeta}>({patient.info})</span>
                    </div>
                    <button className={styles.toggleButton}>
                      {expandedPatients.has(`prev-${patient.name}`) ? '▼' : '▶'}
                    </button>
                  </div>

                  {expandedPatients.has(`prev-${patient.name}`) && (
                    <div className={styles.patientContent}>
                      {patient.content.map((line, i) => (
                        <p key={i}>{line}</p>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className={styles.emptySection}>
              <p>이전 근무조의 인수인계가 없습니다.</p>
            </div>
          )}
        </div>

        {/* 내가 작성하는 인수인계 */}
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <h4>📤 {nextShiftType} 근무조를 위한 인수인계 작성</h4>
          </div>

          <div className={styles.generateSection}>
            <button
              className={styles.generateButton}
              onClick={handleGenerateAiSummary}
              disabled={isGenerating || !currentShiftType}
            >
              {isGenerating ? (
                <>
                  <span className={styles.spinner}></span>
                  AI가 전체 환자의 인수인계 요약을 생성 중입니다...
                </>
              ) : (
                <>
                  <span className={styles.icon}>✨</span>
                  전체 환자 AI 자동 요약 생성
                </>
              )}
            </button>

            {error && <div className={styles.error}>{error}</div>}
          </div>

          {myHandover ? (
            <div className={styles.patientList}>
              {myPatients.map((patient, index) => (
                <div
                  key={index}
                  className={`${styles.patientCard} ${patient.isImportant ? styles.important : ''}`}
                >
                  <div
                    className={styles.patientHeader}
                    onClick={() => togglePatient(`my-${patient.name}`)}
                  >
                    <div className={styles.patientInfo}>
                      {patient.isImportant && (
                        <span className={styles.importantBadge}>중요</span>
                      )}
                      <span className={styles.patientName}>{patient.name}</span>
                      <span className={styles.patientMeta}>({patient.info})</span>
                    </div>
                    <button className={styles.toggleButton}>
                      {expandedPatients.has(`my-${patient.name}`) ? '▼' : '▶'}
                    </button>
                  </div>

                  {expandedPatients.has(`my-${patient.name}`) && (
                    <div className={styles.patientContent}>
                      {patient.content.map((line, i) => (
                        <p key={i}>{line}</p>
                      ))}
                    </div>
                  )}
                </div>
              ))}

              <div className={styles.actions}>
                <button className={styles.saveButton}>
                  💾 다음 근무조에 전달
                </button>
              </div>
            </div>
          ) : (
            <div className={styles.emptySection}>
              <p>AI 자동 요약 버튼을 클릭하여</p>
              <p>이번 근무 동안의 전체 환자 데이터를 기반으로</p>
              <p>인수인계문을 생성하세요.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default HandoverPage;

