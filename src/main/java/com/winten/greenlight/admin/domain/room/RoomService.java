package com.winten.greenlight.admin.domain.room;

import com.winten.greenlight.admin.db.repository.mapper.room.RoomEntity;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomMapper;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomRuleEntity;
import com.winten.greenlight.admin.db.repository.redis.room.RoomCacheRepository;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.AuthUtil;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomConverter roomConverter;
    private final RoomMapper roomMapper;
    private final RoomCacheRepository roomCacheRepository;
    private final SiteCacheRepository siteCacheRepository;

    @Transactional(readOnly = true)
    public List<Room> getAllRoom() {
        var rooms = roomMapper.findAllRoom();
        return roomConverter.toDto(rooms);
    }

    @Transactional(readOnly = true)
    public Room getRoomById(String roomId) {
        var roomParam = RoomEntity.builder().roomId(roomId).build();
        var room = roomMapper.findRoomById(roomParam)
                .orElseThrow(() -> CoreException.of(ErrorType.ROOM_NOT_FOUND, "대기열을 찾을 수 없습니다. ID: " + roomId));

        var roomRuleParam = RoomRuleEntity.builder().roomId(roomId).build();
        var roomRuleEntities = roomMapper.findAllRoomRuleByRoomId(roomRuleParam);

        if (roomRuleEntities == null) {
            roomRuleEntities = List.of();
        }

        room.setRoomRules(roomRuleEntities);

        return roomConverter.toDto(room);
    }

    @Transactional
    public Room createRoom(Room room) {
        var roomParam = roomConverter.toEntity(room);
        var newRoomId = TSID.fast().toString();
        roomParam.setRoomId(newRoomId);

        // Room 저장
        RoomEntity result = roomMapper.saveRoom(roomParam);

        if (room.getDefaultRuleType() != DefaultRuleType.ALL && room.getRoomRules() != null) {
            // RoomRule 저장
            for (RoomRule roomRule : room.getRoomRules()) {
                roomRule.setRoomId(newRoomId);
                roomMapper.saveRoomRule(roomConverter.toEntity(roomRule));
            }
        }

        // 저장된 roomRule 조회
        var roomRules = roomMapper.findAllRoomRuleByRoomId(RoomRuleEntity.builder().roomId(newRoomId).build());
        result.setRoomRules(roomRules);

        // Redis put
        roomCacheRepository.updateRoomMetaCache(result);

        var roomList = this.getAllRoom();
        siteCacheRepository.updateEnabledRoomList(roomList);

        return roomConverter.toDto(result);
    }

    @Transactional
    public Room updateRoom(Room room) {
        var currentRoom = this.getRoomById(room.getRoomId()); // action group 존재여부 확인

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        AuthUtil.ensureCanUpdate(currentRoom.getUserSiteId());

        roomMapper.updateRoomById(roomConverter.toEntity(room));

        // Rule 업데이트
        if (room.isUpdateRule()) {
            // Rule 일괄 삭제
            roomMapper.deleteAllRoomRuleByRoomId(RoomRuleEntity.builder().roomId(room.getRoomId()).build());
            // DefaultRuleType 이 ALL 이 아닌 경우에 추가
            if (room.getDefaultRuleType() != DefaultRuleType.ALL && room.getRoomRules() != null) {
                // Rule 일괄 insert
                for (RoomRule roomRule : room.getRoomRules()) {
                    roomRule.setRoomId(room.getRoomId());
                    roomMapper.saveRoomRule(roomConverter.toEntity(roomRule));
                }
            }
        }

        var updatedRoom = this.getRoomById(room.getRoomId());

        // Redis put
        var updatedRoomEntity = roomConverter.toEntity(updatedRoom);
        roomCacheRepository.updateRoomMetaCache(updatedRoomEntity);

        var roomList = this.getAllRoom();
        siteCacheRepository.updateEnabledRoomList(roomList);

        return updatedRoom;
    }

    @Transactional
    public Room deleteRoom(String roomId) {
        Room currentRoom = this.getRoomById(roomId); // action group 존재여부 확인

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        AuthUtil.ensureCanDelete(currentRoom.getUserSiteId());

        if (currentRoom.getEnabled()) {
            throw CoreException.of(ErrorType.ENABLED_ROOM_CANNOT_BE_DELETED, "활성화 상태의 대기열은 삭제할 수 없습니다.");
        }

        roomMapper.deleteRoomById(RoomEntity.builder().roomId(roomId).build());

        // Redis delete
        roomCacheRepository.deleteRoomMetaCache(roomId);

        var roomList = this.getAllRoom();
        siteCacheRepository.updateEnabledRoomList(roomList);

        return Room.builder()
                .roomId(roomId)
                .build();
    }

    public void reloadRoomMetaCache() {
        // 본인 Site만 조회됨
        List<Room> roomList = this.getAllRoom();
        for (Room room : roomList) {
            var roomDetail = this.getRoomById(room.getRoomId());
            roomCacheRepository.deleteRoomMetaCache(room.getRoomId());
            roomCacheRepository.updateRoomMetaCache(roomConverter.toEntity(roomDetail));
        }

        siteCacheRepository.updateEnabledRoomList(roomList);
    }
}