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
    try {
      await apiClient.patch(`/orders/${orderId}/status`, null, {
        params: { status: newStatus, completedBy: '현재 간호사' },
      });
      fetchOrders();
      setSelectedOrder(null);
    } catch (err) {
      console.error('오더 상태 변경 실패:', err);
      alert('오더 상태 변경에 실패했습니다');
    }
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
            className={`${styles.filterButton} ${filterStatus === 'PENDING' ? styles.active : ''}`}
            onClick={() => setFilterStatus('PENDING')}
          >
            대기
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
                    시작
                  </button>
                )}
                {order.status === 'IN_PROGRESS' && (
                  <button
                    className={styles.completeButton}
                    onClick={() => handleStatusChange(order.id, 'COMPLETED')}
                  >
                    완료
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
    </div>
  );
};

export default OrderTab;
