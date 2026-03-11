package com.winten.greenlight.admin.domain.room;

import com.winten.greenlight.admin.api.controller.room.*;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomEntity;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomRuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomConverter {
    List<Room> toDto(List<RoomEntity> roomEntityList);
    Room toDto(RoomEntity roomEntity);
    RoomRule toDto(RoomRuleEntity roomEntity);

    Room toDto(RoomSearchRequest request);
    Room toDto(RoomCreateRequest request);
    Room toDto(RoomUpdateRequest request);

    RoomEntity toEntity(Room room);
    RoomRuleEntity toEntity(RoomRule room);

    RoomResponse toResponse(Room room);

    RoomRule toDto(RoomRuleCreateRequest request);
    RoomRuleResponse toResponse(RoomRule roomRule);
}