package com.mediflow.emr.scheduler;

import com.mediflow.emr.entity.Shift;
import com.mediflow.emr.entity.enums.ShiftType;
import com.mediflow.emr.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 근무조 자동 생성 스케줄러
 * - 매일 자정에 다음 날 근무조 자동 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShiftScheduler {

    private final ShiftRepository shiftRepository;

    /**
     * 매일 자정(00:00)에 다음 날 근무조 생성
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void createTomorrowShifts() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        log.info("=== 내일({}) 근무조 자동 생성 시작 ===", tomorrow);
        
        // 이미 내일 근무조가 있는지 확인
        List<Shift> existingShifts = shiftRepository.findByDate(tomorrow);
        if (!existingShifts.isEmpty()) {
            log.info("내일({}) 근무조가 이미 존재합니다. 생성 건너뜀.", tomorrow);
            return;
        }
        
        // DAY 근무조 (08:00 ~ 16:00)
        Shift dayShift = Shift.builder()
                .date(tomorrow)
                .type(ShiftType.DAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(16, 0))
                .build();
        
        // EVENING 근무조 (16:00 ~ 00:00)
        Shift eveningShift = Shift.builder()
                .date(tomorrow)
                .type(ShiftType.EVENING)
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(0, 0))
                .build();
        
        // NIGHT 근무조 (00:00 ~ 08:00)
        Shift nightShift = Shift.builder()
                .date(tomorrow)
                .type(ShiftType.NIGHT)
                .startTime(LocalTime.of(0, 0))
                .endTime(LocalTime.of(8, 0))
                .build();
        
        shiftRepository.saveAll(List.of(dayShift, eveningShift, nightShift));
        log.info("=== 내일({}) 근무조 3개 생성 완료 ===", tomorrow);
    }
}
