package com.winten.greenlight.admin.domain.site;

import com.winten.greenlight.admin.api.controller.site.SiteInfoRequest;
import com.winten.greenlight.admin.api.controller.site.SiteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SiteConverter {
    SiteResponse toResponse(SiteInfo site);
    SiteInfo toDto(SiteInfoRequest request);
}