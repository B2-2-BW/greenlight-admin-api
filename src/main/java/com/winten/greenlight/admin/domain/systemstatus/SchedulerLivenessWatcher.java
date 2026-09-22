package com.winten.greenlight.admin.domain.systemstatus;

import com.winten.greenlight.admin.domain.alert.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerLivenessWatcher {
    static final String ALERTNAME = "SCHEDULER_STOPPED";
    static final String PROCESS_CODE = "PROCESS";
    static final String CREATED_BY = "admin-liveness";

    private final SchedulerStatusClient schedulerStatusClient;
    private final AlertService alertService;
    private final SchedulerRunningStatusStore schedulerRunningStatusStore;

    @Value("${scheduler.api.url:}")
    private String schedulerUrl;

    @Value("${scheduler.liveness.failure-threshold:2}")
    private int failureThreshold;

    private int consecutiveFailures;
    private boolean processDownAlerted;

    @Scheduled(fixedDelayString = "${scheduler.liveness.interval-ms:5000}")
    public void poll() {
        if (schedulerUrl == null || schedulerUrl.isBlank()) {
            return;
        }
        try {
            var schedulers = schedulerStatusClient.getSchedulers();
            consecutiveFailures = 0;
            schedulerRunningStatusStore.saveAll(schedulers);
            if (processDownAlerted) {
                alertService.applyPlatformAlert(
                        ALERTNAME,
                        PROCESS_CODE,
                        false,
                        "스케줄러 프로세스 복구",
                        "GET /schedulers 가 다시 성공했습니다.",
                        CREATED_BY
                );
                processDownAlerted = false;
            }
        } catch (Exception exception) {
            consecutiveFailures += 1;
            log.warn("Scheduler liveness poll failed. consecutive={} message={}",
                    consecutiveFailures, exception.getMessage());
            if (consecutiveFailures >= Math.max(failureThreshold, 1)) {
                schedulerRunningStatusStore.saveAllDisabled();
            }
            if (!processDownAlerted && consecutiveFailures >= Math.max(failureThreshold, 1)) {
                alertService.applyPlatformAlert(
                        ALERTNAME,
                        PROCESS_CODE,
                        true,
                        "스케줄러 프로세스 중단",
                        "GET /schedulers 호출에 실패했습니다. " + exception.getMessage(),
                        CREATED_BY
                );
                processDownAlerted = true;
            }
        }
    }
}
