package com.winten.greenlight.admin.db.repository.mapper.room;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoomMapper {
    Optional<RoomEntity> findRoomById(RoomEntity roomEntity);
    List<RoomEntity> findAllRoom();
    RoomEntity saveRoom(RoomEntity roomEntity);
    RoomEntity updateRoomById(RoomEntity roomEntity);
    Long deleteRoomById(RoomEntity roomEntity);
    List<RoomRuleEntity> findAllRoomRuleByRoomId(RoomRuleEntity roomRuleEntity);
    RoomRuleEntity saveRoomRule(RoomRuleEntity roomRuleEntity);
    Long deleteAllRoomRuleByRoomId(RoomRuleEntity roomRuleEntity);
}