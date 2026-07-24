package com.winten.greenlight.admin.api.controller.room;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class RoomPageResponse {
    List<RoomResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
