package com.mediflow.emr.scheduler;

import com.mediflow.emr.service.DemoDataMaintenanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.daily-refresh-enabled", havingValue = "true")
public class DemoDataScheduler {
    private final DemoDataMaintenanceService service;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { refresh(); }

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void refresh() {
        service.refreshAt(LocalDateTime.now(DemoDataMaintenanceService.ZONE));
    }
}
