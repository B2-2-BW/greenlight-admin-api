package com.winten.greenlight.admin.domain.systemstatus;

import com.winten.greenlight.admin.api.controller.systemstatus.SystemStatusResponse.SchedulerItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class SchedulerStatusClient {
    private final String schedulerUrl;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;

    @Autowired
    public SchedulerStatusClient(
            @Value("${scheduler.url:}") String schedulerUrl,
            JsonMapper jsonMapper
    ) {
        this(
                schedulerUrl,
                jsonMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(2))
                        .build()
        );
    }

    SchedulerStatusClient(String schedulerUrl, JsonMapper jsonMapper, HttpClient httpClient) {
        this.schedulerUrl = schedulerUrl;
        this.jsonMapper = jsonMapper;
        this.httpClient = httpClient;
    }

    public List<SchedulerItem> getSchedulers() throws Exception {
        if (schedulerUrl == null || schedulerUrl.isBlank()) {
            throw new IllegalStateException("Scheduler URL is not configured");
        }

        var request = HttpRequest.newBuilder()
                .uri(URI.create(schedulerUrl.replaceAll("/+$", "") + "/schedulers"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Scheduler status request failed");
        }

        return Arrays.stream(jsonMapper.readValue(response.body(), SchedulerPayload[].class))
                .map(payload -> SchedulerItem.builder()
                        .schedulerCode(payload.schedulerCode())
                        .status(payload.status())
                        .name(payload.name())
                        .description(payload.description())
                        .build())
                .toList();
    }

    private record SchedulerPayload(
            String schedulerCode,
            String status,
            String name,
            String description
    ) {
    }
}
