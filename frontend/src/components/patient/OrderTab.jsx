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
        params: {
          status: newStatus,
          completedBy: '현재 간호사'
        },
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

      // 오더 상태를 COMPLETED로 변경
      await handleStatusChange(selectedOrder.id, 'COMPLETED');

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
        <h3>오더 조회</h3>
        <div className={styles.filters}>
          <button
            className={`${styles.filterButton} ${filterStatus === 'ALL' ? styles.active : ''}`}
            onClick={() => setFilterStatus('ALL')}
          >
            전체
          </button>
          <button
            className={`${styles.filterButton} ${filterStatus === 'IN_PROGRESS' ? styles.active : ''}`}
            onClick={() => setFilterStatus('IN_PROGRESS')}
          >
            진행중
          </button>
          <button
            className={`${styles.filterButton} ${filterStatus === 'COMPLETED' ? styles.active : ''}`}
            onClick={() => setFilterStatus('COMPLETED')}
          >
            완료
          </button>
        </div>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {/* 오더 목록 */}
      <div className={styles.orderList}>
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
                  <span className={styles.doctor}>{order.orderDoctor}</span>
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
          <div className={styles.modalContent} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3>약품 상세 정보</h3>
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
              ) : selectedOrder.drugInfo ? (
                <>
                  <div className={styles.drugInfoSection}>
                    <h4>기본 정보</h4>
                    <div className={styles.infoItem}>
                      <span className={styles.infoLabel}>약품명</span>
                      <span className={styles.infoValue}>{selectedOrder.drugInfo.itemName}</span>
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

                  {selectedOrder.drugInfo.efcyQesitm && (
                    <div className={styles.drugInfoSection}>
                      <h4>효능·효과</h4>
                      <p>{selectedOrder.drugInfo.efcyQesitm}</p>
                    </div>
                  )}

                  {selectedOrder.drugInfo.useMethodQesitm && (
                    <div className={styles.drugInfoSection}>
                      <h4>사용법</h4>
                      <p>{selectedOrder.drugInfo.useMethodQesitm}</p>
                    </div>
                  )}

                  {selectedOrder.drugInfo.atpnWarnQesitm && (
                    <div className={styles.drugInfoSection}>
                      <h4>주의사항 경고</h4>
                      <p className={styles.warning}>{selectedOrder.drugInfo.atpnWarnQesitm}</p>
                    </div>
                  )}

                  {selectedOrder.drugInfo.atpnQesitm && (
                    <div className={styles.drugInfoSection}>
                      <h4>주의사항</h4>
                      <p>{selectedOrder.drugInfo.atpnQesitm}</p>
                    </div>
                  )}
                </>
              ) : (
                <div className={styles.noInfo}>약품 상세 정보를 불러올 수 없습니다</div>
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
