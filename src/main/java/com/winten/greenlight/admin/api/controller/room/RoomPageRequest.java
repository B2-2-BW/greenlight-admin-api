package com.winten.greenlight.admin.api.controller.room;

import com.winten.greenlight.admin.domain.room.RoomEnvironment;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RoomPageRequest {
    @Parameter
    @Min(1)
    private int page = 1;

    @Parameter
    @Min(1)
    @Max(100)
    private int size = 10;

    @Parameter
    private String query;

    @Parameter
    private RoomEnvironment roomEnvironment;

    @Parameter
    private Boolean enabled;
}
