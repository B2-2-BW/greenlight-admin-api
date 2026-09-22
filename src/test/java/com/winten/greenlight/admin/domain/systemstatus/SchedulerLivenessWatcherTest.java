package com.winten.greenlight.admin.domain.systemstatus;

import com.winten.greenlight.admin.api.controller.systemstatus.SystemStatusResponse.SchedulerItem;
import com.winten.greenlight.admin.domain.alert.AlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerLivenessWatcherTest {
    @Mock private SchedulerStatusClient schedulerStatusClient;
    @Mock private AlertService alertService;
    @Mock private SchedulerRunningStatusStore schedulerRunningStatusStore;
    @InjectMocks private SchedulerLivenessWatcher watcher;

    @Test
    void firesOnceAfterConsecutivePollFailuresAndResolvesOnRecovery() throws Exception {
        ReflectionTestUtils.setField(watcher, "schedulerUrl", "http://scheduler");
        ReflectionTestUtils.setField(watcher, "failureThreshold", 2);
        when(schedulerStatusClient.getSchedulers())
                .thenThrow(new IllegalStateException("down"))
                .thenThrow(new IllegalStateException("down"))
                .thenReturn(List.of(SchedulerItem.builder().schedulerCode("METRIC").status("RUNNING").build()));

        watcher.poll();
        verify(alertService, never()).applyPlatformAlert(anyString(), anyString(), eq(true), anyString(), anyString(), anyString());

        watcher.poll();
        verify(alertService, times(1)).applyPlatformAlert(
                eq("SCHEDULER_STOPPED"),
                eq("PROCESS"),
                eq(true),
                anyString(),
                anyString(),
                eq("admin-liveness")
        );

        watcher.poll();
        verify(alertService, times(1)).applyPlatformAlert(
                eq("SCHEDULER_STOPPED"),
                eq("PROCESS"),
                eq(true),
                anyString(),
                anyString(),
                eq("admin-liveness")
        );
        verify(alertService).applyPlatformAlert(
                eq("SCHEDULER_STOPPED"),
                eq("PROCESS"),
                eq(false),
                anyString(),
                anyString(),
                eq("admin-liveness")
        );
        verify(schedulerRunningStatusStore).saveAllDisabled();
        verify(schedulerRunningStatusStore).saveAll(any());
    }

    @Test
    void skipsWhenSchedulerUrlIsBlank() throws Exception {
        ReflectionTestUtils.setField(watcher, "schedulerUrl", " ");
        watcher.poll();
        verify(schedulerStatusClient, never()).getSchedulers();
        verify(alertService, never()).applyPlatformAlert(anyString(), anyString(), eq(true), anyString(), anyString(), anyString());
    }
}
