package com.winten.greenlight.admin.db.repository.mapper.room;

import com.winten.greenlight.admin.domain.room.RoomEnvironment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoomMapper {
    Optional<RoomEntity> findRoomById(RoomEntity roomEntity);
    List<RoomEntity> findAllRoom(RoomEntity roomEntity);
    List<RoomEntity> findEnabledRoomsBySiteId(@Param("siteId") String siteId);
    List<RoomEntity> findRoomsPage(@Param("roomEnvironment") RoomEnvironment roomEnvironment,
                                   @Param("enabled") Boolean enabled,
                                   @Param("query") String query,
                                   @Param("limit") int limit,
                                   @Param("offset") long offset);
    long countRooms(@Param("roomEnvironment") RoomEnvironment roomEnvironment,
                    @Param("enabled") Boolean enabled,
                    @Param("query") String query);
    RoomEntity saveRoom(RoomEntity roomEntity);
    RoomEntity updateRoomById(RoomEntity roomEntity);
    Long deleteRoomById(RoomEntity roomEntity);
    List<RoomRuleEntity> findAllRoomRuleByRoomId(RoomRuleEntity roomRuleEntity);
    RoomRuleEntity saveRoomRule(RoomRuleEntity roomRuleEntity);
    Long deleteAllRoomRuleByRoomId(RoomRuleEntity roomRuleEntity);
    int disableRoomsBySiteId(@Param("siteId") String siteId);
}
