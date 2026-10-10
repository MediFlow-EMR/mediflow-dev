import { useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import styles from './OrderTab.module.scss';

/**
 * 오더 조회 탭 컴포넌트
 */
const OrderTab = ({ patientId }) => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [showDrugInfo, setShowDrugInfo] = useState(false);
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [loadingDrugInfo, setLoadingDrugInfo] = useState(false);

  // 투약 등록 관련 상태
  const [completedOrderId, setCompletedOrderId] = useState(null);
  const [showMedicationForm, setShowMedicationForm] = useState(false);
  const [medicationFormData, setMedicationFormData] = useState({
    administeredAt: '',
    notes: '',
  });

  // 투약 경로 매핑
  const routeLabels = {
    PO: '경구',
    IV: '정맥',
    IM: '근육',
    SC: '피하',
    TOPICAL: '국소',
    INHALATION: '흡입',
  };

  // 오더 상태 매핑
  const statusLabels = {
    PENDING: '대기',
    IN_PROGRESS: '진행중',
    COMPLETED: '완료',
    CANCELLED: '취소',
  };

  const statusColors = {
    PENDING: '#f59e0b',
    IN_PROGRESS: '#3b82f6',
    COMPLETED: '#10b981',
    CANCELLED: '#ef4444',
  };

  // 식약처 허가정보는 긴 단일 문자열로 내려오므로 문장/번호 항목별로 나눠 표시한다.
  const normalizeDrugText = (text) => text
    .replaceAll('&nbsp;', ' ')
    .replaceAll('&#x2022;', '•')
    .replaceAll('&#x2981;', '•')
    .replace(/\s*ㆍ\s*/g, '\n')
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>')
    .replaceAll('&amp;', '&');

  const formatDrugDocument = (text) => {
    if (!text) return null;

    const items = normalizeDrugText(text)
      .replace(/\s*(\[\[SECTION:[^\]]+\]\])\s*/g, '\n$1\n')
      .replace(/\s+(?=○\s)/g, '\n')
      .replace(/\s+(?=\d+[.)]\s)/g, '\n')
      .split(/\n+|(?<=다\.)\s+(?=[가-힣A-Z0-9])/)
      .map((item) => item.trim())
      .filter(Boolean);

    return (
      <ul className={styles.documentList}>
        {items.map((item, index) => {
          const sectionMatch = item.match(/^\[\[SECTION:(.+)\]\]$/);
          if (sectionMatch) {
            return (
              <li className={styles.clinicalDocumentHeading} key={`${index}-${item.slice(0, 12)}`}>
                {sectionMatch[1].replace(/[<>]/g, '')}
              </li>
            );
          }
          const numberMatch = item.match(/^(\d+[.)])\s*(.*)$/);
          if (numberMatch) {
            return (
              <li className={styles.clinicalNumberRow} key={`${index}-${item.slice(0, 12)}`}>
                <span>{numberMatch[1]}</span>
                <p>{numberMatch[2]}</p>
              </li>
            );
          }
          const topicMatch = item.match(
            /^○\s*(유효균종|적응증|성인 및 12세 이상 소아|신생아|영ㆍ유아 및 소아(?:\([^)]*\))?|고령자|수막염|임질|수술 전ㆍ후 감염증예방|신장 및 간기능 장애)\s*[:：-]?\s*(.*)$/
          );
          if (topicMatch) {
            const detailItems = topicMatch[1] === '적응증'
              ? topicMatch[2].split(/\s+-\s+/).map((value) => value.trim()).filter(Boolean)
              : [];
            return (
              <li className={styles.clinicalTopicRow} key={`${index}-${item.slice(0, 12)}`}>
                <span>{topicMatch[1]}</span>
                {detailItems.length > 1 ? (
                  <div className={styles.indicationGrid}>
                    {detailItems.map((value) => <span key={value}>{value}</span>)}
                  </div>
                ) : (
                  <p>{topicMatch[2]}</p>
                )}
              </li>
            );
          }
          if (item.startsWith('○')) {
            return (
              <li className={styles.clinicalTopicRow} key={`${index}-${item.slice(0, 12)}`}>
                <p>{item.replace(/^○\s*/, '')}</p>
              </li>
            );
          }
          return <li key={`${index}-${item.slice(0, 12)}`}>{item}</li>;
        })}
      </ul>
    );
  };

  const formatCautionDocument = (text) => {
    if (!text) return null;

    const rows = normalizeDrugText(text)
      .replace(/\s*(\[\[SECTION:[^\]]+\]\])\s*/g, '\n$1\n')
      .replace(/\s+(?=\d+\)\s)/g, '\n')
      .split(/\n+/)
      .map((row) => row.trim())
      .filter(Boolean);

    return (
      <div className={styles.cautionRows}>
        {rows.map((row, index) => {
          const sectionMatch = row.match(/^\[\[SECTION:(.+)\]\]$/);
          const match = row.match(/^(\d+\))\s*(.*)$/);

          if (sectionMatch) {
            return (
              <div className={styles.cautionSectionTitle} key={`${index}-${row.slice(0, 12)}`}>
                {sectionMatch[1].replace(/[<>]/g, '')}
              </div>
            );
          }

          return (
            <div className={styles.cautionRow} key={`${index}-${row.slice(0, 12)}`}>
              {match ? (
                <>
                  <span className={styles.cautionNumber}>{match[1]}</span>
                  <p>{match[2]}</p>
                </>
              ) : (
                <p className={styles.cautionIntro}>{row}</p>
              )}
            </div>
          );
        })}
      </div>
    );
  };

  // 오더 목록 조회
  const fetchOrders = async () => {
    if (!patientId) return;
    
    setError(null);
    try {
      const response = await apiClient.get(`/orders/patient/${patientId}`);
      setOrders(response.data.data || []);
      setLoading(false);
    } catch (err) {
      console.error('오더 목록 조회 실패:', err);
      setError('오더 목록을 불러올 수 없습니다');
      setLoading(false);
    }
  };

  useEffect(() => {
    if (patientId) {
      setLoading(true);
      fetchOrders();
    }
  }, [patientId]);

  // 오더 상태 변경
  const handleStatusChange = async (orderId, newStatus) => {
    console.log('오더 상태 변경 시작:', { orderId, newStatus });
    try {
      const response = await apiClient.patch(`/orders/${orderId}/status`, null, {
        params: { status: newStatus },
      });
      console.log('오더 상태 변경 성공:', response.data);

      // 오더 목록 새로고침
      await fetchOrders();
      setSelectedOrder(null);
    } catch (err) {
      console.error('오더 상태 변경 실패:', err);
      console.error('에러 상세:', err.response?.data);
      alert(`오더 상태 변경에 실패했습니다: ${err.response?.data?.message || err.message}`);
    }
  };

  // 완료 버튼 클릭 (투약 등록 버튼 표시)
  const handleComplete = (orderId) => {
    console.log('완료 버튼 클릭 - orderId:', orderId);
    setCompletedOrderId(orderId);
  };

  // 투약 등록 버튼 클릭
  const handleOpenMedicationForm = (order) => {
    console.log('투약 등록 버튼 클릭 - order:', order);
    setSelectedOrder(order);
    setMedicationFormData({
      administeredAt: getCurrentDateTime(),
      notes: '',
    });
    setShowMedicationForm(true);
  };

  // 약품 상세 정보 보기
  const handleViewDrugInfo = async (order) => {
    setSelectedOrder(order);
    setShowDrugInfo(true);
    
    // 약품 정보가 없으면 API 호출
    if (!order.drugInfo) {
      setLoadingDrugInfo(true);
      try {
        const response = await apiClient.get(`/orders/${order.id}/drug-detail`);
        const drugInfo = response.data.data;
        
        // 오더 목록에서 해당 오더의 drugInfo 업데이트
        setOrders(prevOrders => 
          prevOrders.map(o => 
            o.id === order.id ? { ...o, drugInfo } : o
          )
        );
        
        // 선택된 오더도 업데이트
        setSelectedOrder({ ...order, drugInfo });
      } catch (err) {
        console.error('약품 상세 정보 조회 실패:', err);
        setSelectedOrder({ ...order, drugInfo: null });
      } finally {
        setLoadingDrugInfo(false);
      }
    }
  };

  // 현재 시간 가져오기
  const getCurrentDateTime = () => {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };


  // 투약 등록 폼 입력 변경
  const handleMedicationFormChange = (e) => {
    const { name, value } = e.target;
    setMedicationFormData(prev => ({ ...prev, [name]: value }));
  };

  // 투약 등록 제출
  const handleMedicationSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!selectedOrder) return;

    try {
      const payload = {
        orderId: selectedOrder.id,
        patientId,
        drugName: selectedOrder.orderName,
        drugCode: null,
        dose: selectedOrder.dose,
        route: selectedOrder.route,
        frequency: selectedOrder.frequency,
        administeredAt: medicationFormData.administeredAt || null,
        orderDoctor: selectedOrder.orderDoctor,
        notes: medicationFormData.notes || null,
      };

      await apiClient.post('/medications', payload);
      // 서버가 투약 기록과 처방 완료를 하나의 트랜잭션으로 처리한다.
      await fetchOrders();

      // 폼 닫기 및 상태 초기화
      setShowMedicationForm(false);
      setCompletedOrderId(null);
      setSelectedOrder(null);
      setMedicationFormData({
        administeredAt: '',
        notes: '',
      });

      alert('투약이 등록되었습니다.');
    } catch (err) {
      console.error('투약 등록 실패:', err);
      setError(err.response?.data?.message || '투약 등록에 실패했습니다');
    }
  };

  // 투약 등록 취소
  const handleCancelMedicationForm = () => {
    setShowMedicationForm(false);
    setCompletedOrderId(null);
    setSelectedOrder(null);
    setMedicationFormData({
      administeredAt: '',
      notes: '',
    });
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

  // 필터링된 오더
  const filteredOrders = filterStatus === 'ALL' 
    ? orders 
    : orders.filter(order => order.status === filterStatus);

  return (
    <div className={styles.orderTab}>
      <div className={styles.header}>
        <div>
          <h3>처방 · 투약 수행 워크리스트</h3>
          <p>처방 확인부터 투약 수행 기록까지 한 화면에서 처리합니다.</p>
        </div>
        <div className={styles.filters}>
          <button
            className={`${styles.filterButton} ${filterStatus === 'ALL' ? styles.active : ''}`}
            onClick={() => setFilterStatus('ALL')}
          >
            전체
          </button>
          <button
            className={`${styles.filterButton} ${filterStatus === 'PENDING' ? styles.active : ''}`}
            onClick={() => setFilterStatus('PENDING')}
          >
            대기 {orders.filter((order) => order.status === 'PENDING').length}
          </button>
          <button
            className={`${styles.filterButton} ${filterStatus === 'IN_PROGRESS' ? styles.active : ''}`}
            onClick={() => setFilterStatus('IN_PROGRESS')}
          >
            진행 {orders.filter((order) => order.status === 'IN_PROGRESS').length}
          </button>
          <button
            className={`${styles.filterButton} ${filterStatus === 'COMPLETED' ? styles.active : ''}`}
            onClick={() => setFilterStatus('COMPLETED')}
          >
            완료 {orders.filter((order) => order.status === 'COMPLETED').length}
          </button>
        </div>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {/* 오더 목록 */}
      <div className={styles.orderList}>
        <div className={styles.tableHeader} aria-hidden="true">
          <span>상태 · 처방</span>
          <span>용량 · 경로 · 빈도 · 지시</span>
          <span>수행</span>
        </div>
        {loading && orders.length === 0 && (
          <div className={styles.loading}>오더 정보를 불러오는 중...</div>
        )}
        {filteredOrders.length === 0 ? (
          <div className={styles.empty}>
            {filterStatus === 'ALL' ? '등록된 오더가 없습니다' : `${statusLabels[filterStatus]} 오더가 없습니다`}
          </div>
        ) : (
          filteredOrders.map((order) => (
            <div key={order.id} className={styles.orderItem}>
              <div className={styles.orderHeader}>
                <div className={styles.orderHeaderLeft}>
                  <span 
                    className={styles.status}
                    style={{ backgroundColor: statusColors[order.status] }}
                  >
                    {statusLabels[order.status]}
                  </span>
                  <span className={styles.orderType}>{order.orderType === 'MEDICATION' ? '투약' : order.orderType}</span>
                  <span className={styles.orderName}>{order.orderName}</span>
                </div>
                <div className={styles.orderHeaderRight}>
                  <span className={styles.time}>{formatTime(order.orderedAt)}</span>
                  {order.orderDoctor && (
                    <span className={styles.doctor}>처방의 {order.orderDoctor}</span>
                  )}
                </div>
              </div>

              <div className={styles.orderDetails}>
                {order.dose && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>용량</span>
                    <span className={styles.detailValue}>{order.dose}</span>
                  </div>
                )}
                {order.route && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>경로</span>
                    <span className={styles.detailValue}>{routeLabels[order.route] || order.route}</span>
                  </div>
                )}
                {order.frequency && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>빈도</span>
                    <span className={styles.detailValue}>{order.frequency}</span>
                  </div>
                )}
                {order.instructions && (
                  <div className={styles.detailItem}>
                    <span className={styles.detailLabel}>지시사항</span>
                    <span className={styles.detailValue}>{order.instructions}</span>
                  </div>
                )}
              </div>

              <div className={styles.orderActions}>
                {order.orderType === 'MEDICATION' && order.orderName && (
                  <button
                    className={styles.drugInfoButton}
                    onClick={() => handleViewDrugInfo(order)}
                  >
                    약품 정보
                  </button>
                )}
                {order.status === 'PENDING' && (
                  <button
                    className={styles.startButton}
                    onClick={() => handleStatusChange(order.id, 'IN_PROGRESS')}
                  >
                    투약 시작
                  </button>
                )}
                {order.status === 'IN_PROGRESS' && completedOrderId !== order.id && (
                  <button
                    className={styles.completeButton}
                    onClick={() => handleComplete(order.id)}
                  >
                    완료
                  </button>
                )}
                {completedOrderId === order.id && order.orderType === 'MEDICATION' && (
                  <button
                    className={styles.registerMedicationButton}
                    onClick={() => handleOpenMedicationForm(order)}
                  >
                    투약 등록
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* 약품 상세 정보 모달 */}
      {showDrugInfo && selectedOrder && (
        <div className={styles.modal} onClick={() => setShowDrugInfo(false)}>
          <div className={`${styles.modalContent} ${styles.drugModalContent}`} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <div>
                <h3>약품정보 조회</h3>
                <span className={styles.modalSubtitle}>처방 약품의 허가사항과 투약 정보를 확인합니다.</span>
              </div>
              <button className={styles.closeButton} onClick={() => setShowDrugInfo(false)}>
                ✕
              </button>
            </div>
            <div className={styles.modalBody}>
              {loadingDrugInfo ? (
                <div className={styles.loadingSpinner}>
                  <div className={styles.spinner}></div>
                  <p>약품 정보를 불러오는 중...</p>
                </div>
              ) : selectedOrder.drugInfo?.available ? (
                <>
                  <div className={styles.prescriptionStrip}>
                    <div><span>처방명</span><strong>{selectedOrder.orderName}</strong></div>
                    <div><span>용량</span><strong>{selectedOrder.dose || '-'}</strong></div>
                    <div><span>경로</span><strong>{routeLabels[selectedOrder.route] || selectedOrder.route || '-'}</strong></div>
                    <div><span>빈도</span><strong>{selectedOrder.frequency || '-'}</strong></div>
                  </div>
                  <div className={styles.drugIdentity}>
                    <div className={styles.drugIdentityTitle}>
                      <span className={styles.matchedBadge}>허가품목 일치</span>
                      <strong>{selectedOrder.drugInfo.itemName}</strong>
                    </div>
                    <div className={styles.infoItem}>
                      <span className={styles.infoLabel}>제조사</span>
                      <span className={styles.infoValue}>{selectedOrder.drugInfo.entpName}</span>
                    </div>
                    <div className={styles.infoItem}>
                      <span className={styles.infoLabel}>품목기준코드</span>
                      <span className={styles.infoValue}>{selectedOrder.drugInfo.itemSeq}</span>
                    </div>
                  </div>

                  <div className={styles.drugDocumentGrid}>
                    {selectedOrder.drugInfo.efcyQesitm && (
                      <section className={styles.clinicalPanel}>
                        <h4>효능·효과</h4>
                        <div className={styles.documentText}>{formatDrugDocument(selectedOrder.drugInfo.efcyQesitm)}</div>
                      </section>
                    )}

                    {selectedOrder.drugInfo.useMethodQesitm && (
                      <section className={styles.clinicalPanel}>
                        <h4>용법·용량</h4>
                        <div className={styles.documentText}>{formatDrugDocument(selectedOrder.drugInfo.useMethodQesitm)}</div>
                      </section>
                    )}
                  </div>

                  {selectedOrder.drugInfo.atpnWarnQesitm && (
                    <details className={`${styles.documentDetails} ${styles.warningDetails}`} open>
                      <summary><span>중요 경고</span><small>식약처 허가정보</small></summary>
                      <div className={styles.expandedDocument}>{formatCautionDocument(selectedOrder.drugInfo.atpnWarnQesitm)}</div>
                    </details>
                  )}

                  {selectedOrder.drugInfo.atpnQesitm && (
                    <details className={styles.documentDetails} open>
                      <summary><span>상세 주의사항</span><small>식약처 허가정보</small></summary>
                      <div className={styles.expandedDocument}>{formatCautionDocument(selectedOrder.drugInfo.atpnQesitm)}</div>
                    </details>
                  )}

                  <div className={styles.dataSourceNotice}>
                    기준정보: {selectedOrder.drugInfo.source} · 실제 투약 전 원내 처방 및 최신 허가사항 확인
                  </div>
                </>
              ) : selectedOrder.drugInfo ? (
                <div className={styles.noInfo}>
                  <strong>
                    {{
                      NOT_COVERED: '현재 데이터셋 미수록 품목',
                      NOT_FOUND: '일치하는 허가품목 확인 필요',
                      AMBIGUOUS_MATCH: '제품명과 함량 확인 필요',
                    }[selectedOrder.drugInfo.status] || '공공 API 조회 실패'}
                  </strong>
                  <p>{selectedOrder.drugInfo.message}</p>
                  <span>조회명: {selectedOrder.drugInfo.requestedName}</span>
                  <span>조회원: {selectedOrder.drugInfo.source}</span>
                </div>
              ) : (
                <div className={styles.noInfo}>약품 정보 응답을 확인할 수 없습니다.</div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* 투약 등록 모달 */}
      {showMedicationForm && selectedOrder && (
        <div className={styles.modal} onClick={handleCancelMedicationForm}>
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3>투약 등록</h3>
              <button className={styles.closeButton} onClick={handleCancelMedicationForm}>
                ✕
              </button>
            </div>
            <div className={styles.modalBody}>
              <form onSubmit={handleMedicationSubmit}>
                {/* 자동 입력 정보 (읽기 전용) */}
                <div className={styles.formSection}>
                  <h4>오더 정보</h4>
                  <div className={styles.formGroup}>
                    <label>약물명</label>
                    <input
                      type="text"
                      value={selectedOrder.orderName || ''}
                      readOnly
                      className={styles.readOnlyInput}
                    />
                  </div>
                  <div className={styles.formRow}>
                    <div className={styles.formGroup}>
                      <label>용량</label>
                      <input
                        type="text"
                        value={selectedOrder.dose || '-'}
                        readOnly
                        className={styles.readOnlyInput}
                      />
                    </div>
                    <div className={styles.formGroup}>
                      <label>투약 경로</label>
                      <input
                        type="text"
                        value={routeLabels[selectedOrder.route] || selectedOrder.route || '-'}
                        readOnly
                        className={styles.readOnlyInput}
                      />
                    </div>
                  </div>
                  <div className={styles.formRow}>
                    <div className={styles.formGroup}>
                      <label>빈도</label>
                      <input
                        type="text"
                        value={selectedOrder.frequency || '-'}
                        readOnly
                        className={styles.readOnlyInput}
                      />
                    </div>
                    <div className={styles.formGroup}>
                      <label>처방 의사</label>
                      <input
                        type="text"
                        value={selectedOrder.orderDoctor || '-'}
                        readOnly
                        className={styles.readOnlyInput}
                      />
                    </div>
                  </div>
                </div>

                {/* 간호사 입력 정보 */}
                <div className={styles.formSection}>
                  <h4>투약 정보 입력</h4>
                  <div className={styles.formGroup}>
                    <label>투약 시간 *</label>
                    <input
                      type="datetime-local"
                      name="administeredAt"
                      value={medicationFormData.administeredAt}
                      onChange={handleMedicationFormChange}
                      max={getCurrentDateTime()}
                      required
                    />
                  </div>
                  <div className={styles.formGroup}>
                    <label>특이사항</label>
                    <textarea
                      name="notes"
                      value={medicationFormData.notes}
                      onChange={handleMedicationFormChange}
                      placeholder="투약 시 특이사항을 입력하세요"
                      rows="4"
                      className={styles.textarea}
                    />
                  </div>
                </div>

                {error && <div className={styles.error}>{error}</div>}

                <div className={styles.formActions}>
                  <button type="submit" className={styles.submitButton}>
                    투약 등록
                  </button>
                  <button
                    type="button"
                    className={styles.cancelButton}
                    onClick={handleCancelMedicationForm}
                  >
                    취소
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default OrderTab;
