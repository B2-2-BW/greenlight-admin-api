package com.winten.greenlight.admin.api.controller.queuestatistics;

import com.winten.greenlight.admin.domain.queuestatistics.QueueStatistics;
import com.winten.greenlight.admin.domain.queuestatistics.QueueStatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("queue-statistics")
@RequiredArgsConstructor
@Validated
public class QueueStatisticsController {
    private final QueueStatisticsService queueStatisticsService;

    @GetMapping
    public ResponseEntity<QueueStatistics> getQueueStatistics(@ParameterObject @Valid QueueStatisticsRequest request) {
        return ResponseEntity.ok(queueStatisticsService.getStatistics(request.from(), request.to(), request.roomIds()));
    }
}
