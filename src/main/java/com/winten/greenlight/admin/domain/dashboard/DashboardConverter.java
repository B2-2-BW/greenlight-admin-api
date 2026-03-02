package com.winten.greenlight.admin.domain.dashboard;

import com.winten.greenlight.admin.api.controller.dashboard.DashboardResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DashboardConverter {
    DashboardResponse toResponse(DashboardDetail dashboardDetail);
}
