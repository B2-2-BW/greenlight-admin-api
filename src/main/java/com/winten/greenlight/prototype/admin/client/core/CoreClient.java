package com.winten.greenlight.prototype.admin.client.core;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoreClient {

    @Value("${greenlight.core-api.url}")
    private String coreApiUrl;

    private final CoreClientProvider coreClientProvider;

    public void invalidateActionCacheById(Long actionId) {
        try {
            coreClientProvider.getRestClient()
                    .delete()
                    .uri(coreApiUrl + "/actions/{actionId}/cache", actionId)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void invalidateActionGroupCacheById(Long actionGroupId) {
        try {
            coreClientProvider.getRestClient()
                    .delete()
                    .uri(coreApiUrl + "/action-groups/{actionGroupId}/cache", actionGroupId)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void invalidateActionCacheByLandingId(String landingId) {
        try {
            coreClientProvider.getRestClient()
                    .delete()
                    .uri(coreApiUrl + "/actions/landing/{landingId}/cache", landingId)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}