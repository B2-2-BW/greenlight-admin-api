package com.winten.greenlight.admin.domain.room;

import lombok.Value;

import java.util.List;

@Value
public class RoomPage {
    List<Room> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
}
