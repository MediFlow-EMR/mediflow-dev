package com.mediflow.emr.service;

import com.mediflow.emr.dto.DrugDetailInfo;
import com.mediflow.emr.dto.MedicalOrderRequest;
import com.mediflow.emr.dto.MedicalOrderResponse;
import com.mediflow.emr.entity.MedicalOrder;
import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.entity.enums.Role;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.MedicalOrderRepository;
import com.mediflow.emr.repository.PatientRepository;
import com.mediflow.emr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalOrderService {

    private final MedicalOrderRepository medicalOrderRepository;
    private final PatientRepository patientRepository;
    private final DrugApiService drugApiService;
    private final DemoDrugCatalog demoDrugCatalog;
    private final PatientAccessService patientAccessService;
    private final UserRepository userRepository;

    /**
     * 환자별 오더 목록 조회
     * 성능 최적화: 약품 상세 정보는 별도 API로 조회
     */
    @Transactional(readOnly = true)
    public List<MedicalOrderResponse> getOrdersByPatient(Long patientId) {
        List<MedicalOrder> orders = medicalOrderRepository.findByPatientIdOrderByOrderedAtDesc(patientId);
        
        return orders.stream()
                .map(MedicalOrderResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 오더의 약품 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public DrugDetailInfo getOrderDrugDetail(Long orderId, Long userId) {
        MedicalOrder order = medicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        patientAccessService.requireAccessiblePatient(userId, order.getPatient().getId());
        
        if (!"MEDICATION".equals(order.getOrderType()) || order.getOrderName() == null) {
            return null;
        }
        
        try {
            var mapped = demoDrugCatalog.find(order.getOrderCode())
                    .filter(product -> product.accepts(order.getOrderName()));
            if (mapped.isPresent()) {
                var product = mapped.get();
                if (!product.verified()) {
                    return DrugDetailInfo.builder().available(false).status("DEMO_MAPPING_REQUIRED")
                            .source("식품의약품안전처 의약품 제품 허가정보")
                            .requestedName(order.getOrderName()).message(product.unresolvedReason()).build();
                }
                if (!product.route().equals(order.getRoute())) {
                    return DrugDetailInfo.builder().available(false).status("DEMO_MAPPING_REQUIRED")
                            .source("식품의약품안전처 의약품 제품 허가정보")
                            .requestedName(order.getOrderName()).message("제품과 처방의 투여 경로가 달라 자동 연결하지 않습니다.").build();
                }
                var detail = drugApiService.getDrugDetailByItemSeq(product.itemSeq());
                if (detail.isAvailable() && (!product.itemSeq().equals(detail.getItemSeq())
                        || !product.productName().equals(detail.getItemName()))) {
                    return DrugDetailInfo.builder().available(false).status("DEMO_MAPPING_REQUIRED")
                            .source("식품의약품안전처 의약품 제품 허가정보")
                            .requestedName(order.getOrderName()).message("검증된 제품명과 현재 허가정보가 달라 재확인이 필요합니다.").build();
                }
                detail.setRequestedName(order.getOrderName());
                detail.setProductStrength(product.strength());
                detail.setSource("식품의약품안전처 의약품 제품 허가정보"
                        + ("FOUND_PERMIT_CACHE".equals(detail.getStatus()) ? " (S3 캐시)" : ""));
                return detail;
            }
            return drugApiService.getDrugDetail(order.getOrderName());
        } catch (Exception e) {
            log.warn("약품 상세 정보 조회 실패: {}", order.getOrderName(), e);
            return null;
        }
    }

    /**
     * 오더 등록
     */
    @Transactional
    public MedicalOrderResponse createOrder(MedicalOrderRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));

        MedicalOrder order = MedicalOrder.builder()
                .patient(patient)
                .orderType(request.getOrderType())
                .orderName(request.getOrderName())
                .orderCode(request.getOrderCode())
                .dose(request.getDose())
                .route(request.getRoute())
                .frequency(request.getFrequency())
                .instructions(request.getInstructions())
                .status("PENDING")
                .orderedAt(request.getOrderedAt() != null ? request.getOrderedAt() : LocalDateTime.now())
                .orderDoctor(request.getOrderDoctor())
                .build();

        MedicalOrder savedOrder = medicalOrderRepository.save(order);
        return MedicalOrderResponse.from(savedOrder);
    }

    /**
     * 오더 상태 변경
     */
    @Transactional
    public MedicalOrderResponse updateOrderStatus(Long orderId, String status, Long userId) {
        log.info("오더 상태 변경 시작 - orderId: {}, status: {}, userId: {}", orderId, status, userId);

        MedicalOrder order = medicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        patientAccessService.requireAccessiblePatient(userId, order.getPatient().getId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!List.of("IN_PROGRESS", "COMPLETED", "CANCELLED").contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "허용되지 않는 처방 상태입니다");
        }
        if ("COMPLETED".equals(status) && "MEDICATION".equals(order.getOrderType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "투약 처방은 투약 수행 기록으로만 완료할 수 있습니다");
        }
        if ("CANCELLED".equals(status)
                && user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "처방 취소는 의사 또는 관리자만 할 수 있습니다");
        }
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "완료 또는 취소된 처방은 상태를 변경할 수 없습니다");
        }

        log.info("오더 조회 성공 - 현재 상태: {}", order.getStatus());

        order.updateStatus(status, user.getName());

        log.info("오더 상태 업데이트 완료 - 새 상태: {}", order.getStatus());

        // 명시적으로 저장
        MedicalOrder savedOrder = medicalOrderRepository.save(order);

        log.info("오더 저장 완료 - 저장된 상태: {}", savedOrder.getStatus());

        MedicalOrderResponse response = MedicalOrderResponse.from(savedOrder);

        log.info("오더 상태 변경 완료 - response status: {}", response.getStatus());

        return response;
    }

    /**
     * 오더 삭제
     */
    @Transactional
    public void deleteOrder(Long orderId, Long userId) {
        MedicalOrder order = medicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        patientAccessService.requireAccessiblePatient(userId, order.getPatient().getId());
        
        medicalOrderRepository.delete(order);
    }
}
