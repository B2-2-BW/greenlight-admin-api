package com.winten.greenlight.admin.domain.room;

import com.winten.greenlight.admin.db.repository.mapper.room.RoomEntity;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomMapper;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomRuleEntity;
import com.winten.greenlight.admin.db.repository.mapper.site.SiteMapper;
import com.winten.greenlight.admin.db.repository.redis.room.RoomCacheRepository;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import com.winten.greenlight.admin.domain.action.DefaultRuleType;
import com.winten.greenlight.admin.domain.audit.AuditAction;
import com.winten.greenlight.admin.domain.audit.AuditService;
import com.winten.greenlight.admin.domain.site.SiteInfo;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.error.NotModifiedException;
import com.winten.greenlight.admin.support.util.AuthUtil;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomConverter roomConverter;
    private final RoomMapper roomMapper;
    private final RoomCacheRepository roomCacheRepository;
    private final SiteCacheRepository siteCacheRepository;
    private final SiteMapper siteMapper;
    private final AuditService auditService;

    private static final List<String> AUDITED_ROOM_FIELDS = List.of(
            "name",
            "description",
            "maxTrafficPerSecond",
            "capacity",
            "enabled",
            "defaultRuleType",
            "defaultDestinationUrl",
            "roomEnvironment",
            "adImageUrl",
            "roomRules"
    );

    @Transactional(readOnly = true)
    public DashboardRoomList getRoomListFiltered(String version, Room roomParam) {
        var currentVersion = roomCacheRepository.getRoomMetaVersion();
        if (currentVersion.equals(version)) { // 버전 변경이 없을 경우 스킵
            throw new NotModifiedException();
        }

        var roomList = this.getRoomListFiltered(roomParam);
        return DashboardRoomList.builder()
                .version(currentVersion)
                .roomList(roomList)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Room> getRoomListFiltered(Room roomParam) {
        var rooms = roomMapper.findAllRoom(roomConverter.toEntity(roomParam));
        return roomConverter.toDto(rooms);
    }

    @Transactional(readOnly = true)
    public RoomPage getRoomPage(int requestedPage, int size, String query, RoomEnvironment roomEnvironment, Boolean enabled) {
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        long totalElements = roomMapper.countRooms(roomEnvironment, enabled, normalizedQuery);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int page = totalPages == 0 ? 1 : Math.min(requestedPage, totalPages);
        long offset = (long) (page - 1) * size;
        var content = totalElements == 0 ? List.<Room>of() : roomConverter.toDto(
                roomMapper.findRoomsPage(roomEnvironment, enabled, normalizedQuery, size, offset)
        );
        return new RoomPage(content, page, size, totalElements, totalPages);
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
        ensureSiteIsActive(AuthUtil.getCurrentUser().getUserSiteId());
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

        // 활성화된 Room만 업데이트
//        updateRoomListCache();

        updateSiteEnabledRoomListCache(result.getSiteId());

        return roomConverter.toDto(result);
    }

    @Transactional
    public Room updateRoom(Room room, String reason) {
        var currentRoom = this.getRoomById(room.getRoomId()); // action group 존재여부 확인

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        AuthUtil.ensureCanUpdate(currentRoom.getSiteId());
        ensureSiteIsActive(currentRoom.getSiteId());

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

        updateSiteEnabledRoomListCache(currentRoom.getSiteId());

        auditService.recordChanges(
                currentRoom.getSiteId(),
                "ROOM",
                currentRoom.getRoomId(),
                AuditAction.UPDATE,
                reason,
                auditedValues(currentRoom),
                auditedValues(updatedRoom),
                AUDITED_ROOM_FIELDS
        );

        return updatedRoom;
    }

    @Transactional
    public Room deleteRoom(String roomId) {
        Room currentRoom = this.getRoomById(roomId); // action group 존재여부 확인

        // 본인 Site가 아닐 경우 수정하면 안되므로 검증로직 추가 (SUPER 권한 제외)
        AuthUtil.ensureCanDelete(currentRoom.getSiteId());
        ensureSiteIsActive(currentRoom.getSiteId());

        if (currentRoom.getEnabled()) {
            throw CoreException.of(ErrorType.ENABLED_ROOM_CANNOT_BE_DELETED, "활성화 상태의 대기열은 삭제할 수 없습니다.");
        }

        roomMapper.deleteRoomById(RoomEntity.builder().roomId(roomId).build());

        // Redis delete
        roomCacheRepository.deleteRoomMetaCache(roomId);

        // 활성화된 Room만 업데이트
//        updateRoomListCache();

        updateSiteEnabledRoomListCache(currentRoom.getSiteId()); // room list 갱신

        return Room.builder()
                .roomId(roomId)
                .build();
    }

    public List<String> reloadRoomMetaCache() {
        AuthUtil.ensureUserAdmin();
        // 본인 Site만 조회됨
        List<Room> roomList = this.getRoomListFiltered(new Room());

        for (Room room : roomList) {
            var roomDetail = this.getRoomById(room.getRoomId());
            roomCacheRepository.deleteRoomMetaCache(room.getRoomId());
            roomCacheRepository.updateRoomMetaCache(roomConverter.toEntity(roomDetail));
        }

        roomCacheRepository.updateRoomMetaVersionToNow(); // 버전 최신화

        Map<String, List<Room>> enabledRoomsBySite = roomList.stream()
                .collect(Collectors.groupingBy(
                        Room::getSiteId,
                        Collectors.filtering(room -> Boolean.TRUE.equals(room.getEnabled()), Collectors.toList())
                ));
        var currentUser = AuthUtil.getCurrentUser();
        var targetSiteIds = currentUser.getUserRole() == com.winten.greenlight.admin.domain.user.UserRole.SUPER
                ? siteMapper.findAllSite().stream().map(site -> site.getSiteId()).toList()
                : List.of(currentUser.getUserSiteId());
        targetSiteIds.forEach(siteId ->
                siteCacheRepository.updateRoomListCache(siteId, enabledRoomsBySite.getOrDefault(siteId, List.of())));

        return roomList.stream().map(Room::getRoomId).toList();
    }

    private void updateSiteEnabledRoomListCache(String siteId) {
        var roomList = roomConverter.toDto(roomMapper.findEnabledRoomsBySiteId(siteId));
        siteCacheRepository.updateRoomListCache(siteId, roomList);
    }

    private Map<String, Object> auditedValues(Room room) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", room.getName());
        values.put("description", room.getDescription());
        values.put("maxTrafficPerSecond", room.getMaxTrafficPerSecond());
        values.put("capacity", room.getCapacity());
        values.put("enabled", room.getEnabled());
        values.put("defaultRuleType", room.getDefaultRuleType());
        values.put("defaultDestinationUrl", room.getDefaultDestinationUrl());
        values.put("roomEnvironment", room.getRoomEnvironment());
        values.put("adImageUrl", room.getAdImageUrl());
        values.put("roomRules", sanitizedRules(room.getRoomRules()));
        return values;
    }

    private List<Map<String, Object>> sanitizedRules(List<RoomRule> roomRules) {
        if (roomRules == null) return List.of();
        return roomRules.stream().map(rule -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("value", rule.getValue());
            value.put("matchOperator", rule.getMatchOperator());
            value.put("description", rule.getDescription());
            return value;
        }).toList();
    }

    private void ensureSiteIsActive(String siteId) {
        if (siteId == null || siteMapper.findSiteById(SiteInfo.builder().siteId(siteId).build()).isEmpty()) {
            throw CoreException.of(ErrorType.SITE_NOT_FOUND, "사용할 수 없는 사이트입니다. " + siteId);
        }
    }
}
