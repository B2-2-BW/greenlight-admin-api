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
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void roomPageNormalizesSearchAppliesFiltersAndCapsOutOfRangePage() {
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, mock(RoomCacheRepository.class), mock(SiteCacheRepository.class),
                mock(SiteMapper.class), mock(AuditService.class)
        );
        var entity = RoomEntity.builder().roomId("room-21").build();
        var room = Room.builder().roomId("room-21").build();
        when(roomMapper.countRooms(RoomEnvironment.LIVE, true, "queue")).thenReturn(21L);
        when(roomMapper.findRoomsPage(RoomEnvironment.LIVE, true, "queue", 10, 20)).thenReturn(List.of(entity));
        when(roomConverter.toDto(List.of(entity))).thenReturn(List.of(room));

        RoomPage result = service.getRoomPage(99, 10, " queue ", RoomEnvironment.LIVE, true);

        assertThat(result.getPage()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(21);
        assertThat(result.getContent()).containsExactly(room);
        verify(roomMapper).countRooms(RoomEnvironment.LIVE, true, "queue");
        verify(roomMapper).findRoomsPage(RoomEnvironment.LIVE, true, "queue", 10, 20);
    }

    @Test
    void emptyRoomPageDoesNotExecutePagedQueryForBlankSearch() {
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, mock(RoomCacheRepository.class), mock(SiteCacheRepository.class),
                mock(SiteMapper.class), mock(AuditService.class)
        );
        when(roomMapper.countRooms(null, false, null)).thenReturn(0L);

        RoomPage result = service.getRoomPage(1, 10, "   ", null, false);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.getContent()).isEmpty();
        verify(roomMapper).countRooms(null, false, null);
    }

    @Test
    void createRoomPreservesDisabledStateAndRefreshesOnlyTheCreatedRoomsSite() {
        authenticate(UserRole.SITE_ADMIN, "site-b");
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        RoomCacheRepository roomCacheRepository = mock(RoomCacheRepository.class);
        SiteCacheRepository siteCacheRepository = mock(SiteCacheRepository.class);
        SiteMapper siteMapper = mock(SiteMapper.class);
        AuditService auditService = mock(AuditService.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, roomCacheRepository, siteCacheRepository,
                siteMapper, auditService
        );
        var request = Room.builder().enabled(false).build();
        var entity = RoomEntity.builder().enabled(false).build();
        var savedEntity = RoomEntity.builder()
                .roomId("room-disabled")
                .siteId("site-b")
                .enabled(false)
                .build();
        var savedRoom = Room.builder()
                .roomId("room-disabled")
                .siteId("site-b")
                .enabled(false)
                .build();
        when(roomConverter.toEntity(request)).thenReturn(entity);
        when(roomMapper.saveRoom(entity)).thenReturn(savedEntity);
        when(roomMapper.findEnabledRoomsBySiteId("site-b")).thenReturn(List.of());
        when(roomConverter.toDto(List.<RoomEntity>of())).thenReturn(List.of());
        when(roomConverter.toDto(savedEntity)).thenReturn(savedRoom);
        when(siteMapper.findSiteById(any())).thenReturn(java.util.Optional.of(
                SiteInfo.builder().siteId("site-b").siteEnabled(true).build()
        ));

        Room result = service.createRoom(request);

        assertThat(result).isEqualTo(savedRoom);
        assertThat(entity.getEnabled()).isFalse();
        verify(roomMapper).findEnabledRoomsBySiteId("site-b");
        verify(siteCacheRepository).updateRoomListCache("site-b", List.of());
        verify(auditService).recordChanges(
                eq("site-b"), eq("ROOM"), eq("room-disabled"), eq(AuditAction.CREATE), eq(null),
                anyMap(), anyMap(), anyList()
        );
    }

    @Test
    void superUpdateRecordsAuditAgainstTheTargetRoomsSite() {
        authenticate(UserRole.SUPER, "super-site");
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        RoomCacheRepository roomCacheRepository = mock(RoomCacheRepository.class);
        SiteCacheRepository siteCacheRepository = mock(SiteCacheRepository.class);
        AuditService auditService = mock(AuditService.class);
        SiteMapper siteMapper = mock(SiteMapper.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, roomCacheRepository, siteCacheRepository,
                siteMapper, auditService
        );
        var previousEntity = RoomEntity.builder().roomId("room-a").siteId("site-a").name("이전").build();
        var updatedEntity = RoomEntity.builder().roomId("room-a").siteId("site-a").name("변경").build();
        var updateEntity = RoomEntity.builder().roomId("room-a").name("변경").build();
        var requestedRule = RoomRule.builder().value("vip").build();
        var requestedRuleEntity = RoomRuleEntity.builder().value("vip").build();
        var previous = Room.builder().roomId("room-a").siteId("site-a").name("이전").roomRules(List.of()).build();
        var updated = Room.builder().roomId("room-a").siteId("site-a").name("변경").roomRules(List.of()).build();
        var update = Room.builder()
                .roomId("room-a")
                .name("변경")
                .defaultRuleType(DefaultRuleType.INCLUDE)
                .roomRules(List.of(requestedRule))
                .updateRule(true)
                .build();
        when(roomMapper.findRoomById(any())).thenReturn(
                java.util.Optional.of(previousEntity),
                java.util.Optional.of(updatedEntity)
        );
        when(roomMapper.findAllRoomRuleByRoomId(any())).thenReturn(List.of());
        when(roomConverter.toDto(previousEntity)).thenReturn(previous);
        when(roomConverter.toDto(updatedEntity)).thenReturn(updated);
        when(roomConverter.toEntity(update)).thenReturn(updateEntity);
        when(roomConverter.toEntity(requestedRule)).thenReturn(requestedRuleEntity);
        when(roomConverter.toEntity(updated)).thenReturn(updatedEntity);
        when(roomMapper.findEnabledRoomsBySiteId("site-a")).thenReturn(List.of());
        when(roomConverter.toDto(anyList())).thenReturn(List.of());
        when(siteMapper.findSiteById(any())).thenReturn(java.util.Optional.of(
                SiteInfo.builder().siteId("site-a").siteEnabled(true).build()
        ));

        service.updateRoom(update, "처리량 조정");

        verify(auditService).recordChanges(
                eq("site-a"), eq("ROOM"), eq("room-a"), eq(AuditAction.UPDATE), eq("처리량 조정"),
                anyMap(), anyMap(), anyList()
        );
        assertThat(requestedRule.getSiteId()).isEqualTo("site-a");
        verify(roomMapper).saveRoomRule(requestedRuleEntity);
    }

    @Test
    void deleteRoomRecordsAuditAgainstTheDeletedRoomsSite() {
        authenticate(UserRole.SUPER, "super-site");
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        RoomCacheRepository roomCacheRepository = mock(RoomCacheRepository.class);
        SiteCacheRepository siteCacheRepository = mock(SiteCacheRepository.class);
        SiteMapper siteMapper = mock(SiteMapper.class);
        AuditService auditService = mock(AuditService.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, roomCacheRepository, siteCacheRepository,
                siteMapper, auditService
        );
        var entity = RoomEntity.builder().roomId("room-a").siteId("site-a").enabled(false).build();
        var room = Room.builder()
                .roomId("room-a")
                .siteId("site-a")
                .enabled(false)
                .roomRules(List.of())
                .build();
        when(roomMapper.findRoomById(any())).thenReturn(java.util.Optional.of(entity));
        when(roomMapper.findAllRoomRuleByRoomId(any())).thenReturn(List.of());
        when(roomConverter.toDto(entity)).thenReturn(room);
        when(siteMapper.findSiteById(any())).thenReturn(java.util.Optional.of(
                SiteInfo.builder().siteId("site-a").siteEnabled(true).build()
        ));
        when(roomMapper.findEnabledRoomsBySiteId("site-a")).thenReturn(List.of());
        when(roomConverter.toDto(List.<RoomEntity>of())).thenReturn(List.of());

        Room result = service.deleteRoom("room-a");

        assertThat(result.getRoomId()).isEqualTo("room-a");
        verify(roomMapper).deleteRoomById(any());
        verify(auditService).recordChanges(
                eq("site-a"), eq("ROOM"), eq("room-a"), eq(AuditAction.DELETE), eq(null),
                anyMap(), anyMap(), anyList()
        );
        verify(roomCacheRepository).deleteRoomMetaCache("room-a");
        verify(siteCacheRepository).updateRoomListCache("site-a", List.of());
    }

    @Test
    void deletedSiteBlocksRoomCreate() {
        authenticate(UserRole.SITE_ADMIN, "site-a");
        RoomMapper roomMapper = mock(RoomMapper.class);
        RoomService service = new RoomService(
                mock(RoomConverter.class), roomMapper, mock(RoomCacheRepository.class),
                mock(SiteCacheRepository.class), mock(SiteMapper.class), mock(AuditService.class)
        );

        assertThatThrownBy(() -> service.createRoom(Room.builder().build()))
                .isInstanceOf(com.winten.greenlight.admin.support.error.CoreException.class)
                .extracting(error -> ((com.winten.greenlight.admin.support.error.CoreException) error).getErrorType())
                .isEqualTo(com.winten.greenlight.admin.support.error.ErrorType.SITE_NOT_FOUND);
        verifyNoInteractions(roomMapper);
    }

    @Test
    void deletedSiteBlocksRoomUpdateAndDelete() {
        authenticate(UserRole.SITE_ADMIN, "site-a");
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        SiteMapper siteMapper = mock(SiteMapper.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, mock(RoomCacheRepository.class),
                mock(SiteCacheRepository.class), siteMapper, mock(AuditService.class)
        );
        var entity = RoomEntity.builder().roomId("room-a").siteId("site-a").enabled(false).build();
        var room = Room.builder().roomId("room-a").siteId("site-a").enabled(false).roomRules(List.of()).build();
        when(roomMapper.findRoomById(any())).thenReturn(java.util.Optional.of(entity));
        when(roomMapper.findAllRoomRuleByRoomId(any())).thenReturn(List.of());
        when(roomConverter.toDto(entity)).thenReturn(room);

        assertThatThrownBy(() -> service.updateRoom(Room.builder().roomId("room-a").build(), "변경"))
                .isInstanceOf(com.winten.greenlight.admin.support.error.CoreException.class)
                .extracting(error -> ((com.winten.greenlight.admin.support.error.CoreException) error).getErrorType())
                .isEqualTo(com.winten.greenlight.admin.support.error.ErrorType.SITE_NOT_FOUND);
        assertThatThrownBy(() -> service.deleteRoom("room-a"))
                .isInstanceOf(com.winten.greenlight.admin.support.error.CoreException.class)
                .extracting(error -> ((com.winten.greenlight.admin.support.error.CoreException) error).getErrorType())
                .isEqualTo(com.winten.greenlight.admin.support.error.ErrorType.SITE_NOT_FOUND);
        verify(roomMapper, never()).updateRoomById(any());
        verify(roomMapper, never()).deleteRoomById(any());
    }

    @Test
    void superReloadClearsRoomListCacheForSitesWithoutAnyRooms() {
        authenticate(UserRole.SUPER, "super-site");
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        SiteCacheRepository siteCacheRepository = mock(SiteCacheRepository.class);
        SiteMapper siteMapper = mock(SiteMapper.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, mock(RoomCacheRepository.class), siteCacheRepository,
                siteMapper, mock(AuditService.class)
        );
        when(roomConverter.toEntity(any(Room.class))).thenReturn(RoomEntity.builder().build());
        when(roomMapper.findAllRoom(any(RoomEntity.class))).thenReturn(List.of());
        when(roomConverter.toDto(List.<RoomEntity>of())).thenReturn(List.of());
        when(siteMapper.findAllSite()).thenReturn(List.of(
                SiteInfo.builder().siteId("site-a").build(),
                SiteInfo.builder().siteId("site-b").build()
        ));

        assertThat(service.reloadRoomMetaCache()).isEmpty();

        verify(siteCacheRepository).updateRoomListCache("site-a", List.of());
        verify(siteCacheRepository).updateRoomListCache("site-b", List.of());
    }

    @Test
    void siteAdminReloadClearsOnlyOwnSitesRoomListCacheWhenItHasNoRooms() {
        authenticate(UserRole.SITE_ADMIN, "site-a");
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        SiteCacheRepository siteCacheRepository = mock(SiteCacheRepository.class);
        SiteMapper siteMapper = mock(SiteMapper.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, mock(RoomCacheRepository.class), siteCacheRepository,
                siteMapper, mock(AuditService.class)
        );
        when(roomConverter.toEntity(any(Room.class))).thenReturn(RoomEntity.builder().build());
        when(roomMapper.findAllRoom(any(RoomEntity.class))).thenReturn(List.of());
        when(roomConverter.toDto(List.<RoomEntity>of())).thenReturn(List.of());

        assertThat(service.reloadRoomMetaCache()).isEmpty();

        verify(siteCacheRepository).updateRoomListCache("site-a", List.of());
        verify(siteMapper, never()).findAllSite();
    }

    private void authenticate(UserRole role, String siteId) {
        var user = CurrentUser.builder()
                .userId("test-user")
                .userRole(role)
                .userSiteId(siteId)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }
}
