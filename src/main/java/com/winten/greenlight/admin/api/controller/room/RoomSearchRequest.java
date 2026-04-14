package com.winten.greenlight.admin.api.controller.room;

import com.winten.greenlight.admin.domain.room.RoomEnvironment;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomSearchRequest {
    @Parameter
    private String version;

    @Parameter
    private RoomEnvironment roomEnvironment;

    @Parameter
    private Boolean enabled;
}