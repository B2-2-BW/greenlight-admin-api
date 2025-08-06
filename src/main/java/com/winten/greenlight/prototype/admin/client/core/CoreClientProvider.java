package com.winten.greenlight.prototype.admin.client.core;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CoreClientProvider {
    @Value("${greenlight.core-api.url}")
    private String coreApiUrl;

    @Getter
    private final RestClient restClient;

    public CoreClientProvider() {
        this.restClient = RestClient.builder()
                .baseUrl(coreApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}