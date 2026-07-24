package com.winten.greenlight.admin.domain.room;

import com.winten.greenlight.admin.db.repository.mapper.room.RoomEntity;
import com.winten.greenlight.admin.db.repository.mapper.room.RoomMapper;
import com.winten.greenlight.admin.db.repository.redis.room.RoomCacheRepository;
import com.winten.greenlight.admin.db.repository.redis.site.SiteCacheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
    @Test
    void roomPageNormalizesSearchAppliesFiltersAndCapsOutOfRangePage() {
        RoomConverter roomConverter = mock(RoomConverter.class);
        RoomMapper roomMapper = mock(RoomMapper.class);
        RoomService service = new RoomService(
                roomConverter, roomMapper, mock(RoomCacheRepository.class), mock(SiteCacheRepository.class)
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
                roomConverter, roomMapper, mock(RoomCacheRepository.class), mock(SiteCacheRepository.class)
        );
        when(roomMapper.countRooms(null, false, null)).thenReturn(0L);

        RoomPage result = service.getRoomPage(1, 10, "   ", null, false);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.getContent()).isEmpty();
        verify(roomMapper).countRooms(null, false, null);
    }
}
