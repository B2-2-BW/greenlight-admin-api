package com.winten.greenlight.admin.domain.site;

import lombok.Value;

import java.util.List;

@Value
public class SitePage {
    List<SiteInfo> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
