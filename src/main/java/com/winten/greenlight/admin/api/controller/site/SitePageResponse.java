package com.winten.greenlight.admin.api.controller.site;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class SitePageResponse {
    List<SiteResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
