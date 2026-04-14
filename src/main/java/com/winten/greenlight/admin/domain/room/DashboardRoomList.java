package com.winten.greenlight.admin.domain.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class DashboardRoomList {
    private String version;
    private List<Room> roomList;
}