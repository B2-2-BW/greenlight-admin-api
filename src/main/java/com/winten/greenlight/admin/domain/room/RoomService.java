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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashSet;
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
        String targetSiteId = AuthUtil.getCurrentUser().getUserSiteId();
        ensureSiteIsActive(targetSiteId);
        var roomParam = roomConverter.toEntity(room);
        var newRoomId = TSID.fast().toString();
        roomParam.setRoomId(newRoomId);

        // Room 저장
        RoomEntity result = roomMapper.saveRoom(roomParam);

        if (room.getDefaultRuleType() != DefaultRuleType.ALL && room.getRoomRules() != null) {
            // RoomRule 저장
            for (RoomRule roomRule : room.getRoomRules()) {
                roomRule.setRoomId(newRoomId);
                roomRule.setSiteId(targetSiteId);
                roomMapper.saveRoomRule(roomConverter.toEntity(roomRule));
            }
        }

        // 저장된 roomRule 조회
        var roomRules = roomMapper.findAllRoomRuleByRoomId(RoomRuleEntity.builder().roomId(newRoomId).build());
        result.setRoomRules(roomRules);

        Room createdRoom = roomConverter.toDto(result);
        auditService.recordChanges(
                result.getSiteId(),
                "ROOM",
                result.getRoomId(),
                AuditAction.CREATE,
                null,
                emptyAuditedValues(),
                auditedValues(createdRoom),
                AUDITED_ROOM_FIELDS
        );

        // Redis put
        roomCacheRepository.updateRoomMetaCache(result);

        // 활성화된 Room만 업데이트
//        updateRoomListCache();

        updateSiteEnabledRoomListCache(result.getSiteId());
        roomCacheRepository.updateRoomMetaVersionToNow();

        return createdRoom;
    }

    @Transactional
    public Room updateRoom(Room room, String reason) {
        var currentRoom = this.getRoomById(room.getRoomId());
        AuthUtil.ensureCanUpdate(currentRoom.getSiteId());
        ensureSiteIsActive(currentRoom.getSiteId());

        var updatedRoom = updateRoomAndAudit(room, currentRoom, reason);
        roomCacheRepository.updateRoomMetaCache(roomConverter.toEntity(updatedRoom));
        updateSiteEnabledRoomListCache(currentRoom.getSiteId());
        roomCacheRepository.updateRoomMetaVersionToNow();
        return updatedRoom;
    }

    @Transactional
    public void bulkUpdateRooms(List<String> roomIds, Integer capacity, Integer maxTrafficPerSecond, Boolean enabled) {
        if (capacity == null && maxTrafficPerSecond == null && enabled == null) {
            throw CoreException.of(ErrorType.INVALID_DATA, "변경할 대기열을 하나 이상 지정해 주세요.");
        }
        if (new HashSet<>(roomIds).size() != roomIds.size()) {
            throw CoreException.of(ErrorType.INVALID_DATA, "중복된 대기열입니다.");
        }

        String siteId = AuthUtil.getCurrentUser().getUserSiteId();
        AuthUtil.ensureCanUpdate(siteId);
        ensureSiteIsActive(siteId);
        var currentRooms = roomIds.stream().sorted().map(this::getRoomById).toList();
        for (Room currentRoom : currentRooms) {
            if (!siteId.equals(currentRoom.getSiteId())) {
                throw CoreException.of(ErrorType.FORBIDDEN, "해당 사이트에 대한 수정 권한이 없습니다.");
            }
        }

        var updatedRooms = currentRooms.stream().map(currentRoom -> updateRoomAndAudit(
                Room.builder().roomId(currentRoom.getRoomId()).capacity(capacity)
                        .maxTrafficPerSecond(maxTrafficPerSecond).enabled(enabled).build(),
                currentRoom,
                null
        )).toList();
        var activeRooms = roomConverter.toDto(roomMapper.findEnabledRoomsBySiteId(siteId));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    for (Room updatedRoom : updatedRooms) {
                        roomCacheRepository.updateRoomMetaCache(roomConverter.toEntity(updatedRoom));
                    }
                    siteCacheRepository.updateRoomListCache(siteId, activeRooms);
                    roomCacheRepository.updateRoomMetaVersionToNow();
                } catch (RuntimeException exception) {
                    // OperationalCacheSync retries from the committed database state.
                    log.error("Redis sync failed after bulk room update. siteId={} roomIds={}", siteId, roomIds, exception);
                }
            }
        });
    }

    private Room updateRoomAndAudit(Room room, Room currentRoom, String reason) {
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
                    roomRule.setSiteId(currentRoom.getSiteId());
                    roomMapper.saveRoomRule(roomConverter.toEntity(roomRule));
                }
            }
        }

        var updatedRoom = this.getRoomById(room.getRoomId());

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

        auditService.recordChanges(
                currentRoom.getSiteId(),
                "ROOM",
                currentRoom.getRoomId(),
                AuditAction.DELETE,
                null,
                auditedValues(currentRoom),
                emptyAuditedValues(),
                AUDITED_ROOM_FIELDS
        );

        // Redis delete
        roomCacheRepository.deleteRoomMetaCache(roomId);

        // 활성화된 Room만 업데이트
//        updateRoomListCache();

        updateSiteEnabledRoomListCache(currentRoom.getSiteId()); // room list 갱신
        roomCacheRepository.updateRoomMetaVersionToNow();

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
        String siteId = currentUser.getUserSiteId();
        siteCacheRepository.updateRoomListCache(siteId, enabledRoomsBySite.getOrDefault(siteId, List.of()));

        return roomList.stream().map(Room::getRoomId).toList();
    }

    public void reloadAllRoomsForSystem() {
        List<SiteInfo> sites = siteMapper.findAllSite();
        if (sites == null) {
            return;
        }
        for (SiteInfo site : sites) {
            List<RoomEntity> entities = roomMapper.findRoomsBySiteId(site.getSiteId());
            if (entities == null) {
                entities = List.of();
            }
            List<Room> rooms = new ArrayList<>();
            for (RoomEntity entity : entities) {
                var rules = roomMapper.findRoomRulesByRoomId(entity.getRoomId());
                entity.setRoomRules(rules == null ? List.of() : rules);
                Room room = roomConverter.toDto(entity);
                roomCacheRepository.deleteRoomMetaCache(room.getRoomId());
                roomCacheRepository.updateRoomMetaCache(roomConverter.toEntity(room));
                rooms.add(room);
            }
            siteCacheRepository.updateRoomListCache(site.getSiteId(), rooms);
        }
        roomCacheRepository.updateRoomMetaVersionToNow();
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

    private Map<String, Object> emptyAuditedValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        AUDITED_ROOM_FIELDS.forEach(field -> values.put(field, null));
        return values;
    }

    private void ensureSiteIsActive(String siteId) {
        if (siteId == null || siteMapper.findSiteById(SiteInfo.builder().siteId(siteId).build()).isEmpty()) {
            throw CoreException.of(ErrorType.SITE_NOT_FOUND, "사용할 수 없는 사이트입니다. " + siteId);
        }
    }
}
