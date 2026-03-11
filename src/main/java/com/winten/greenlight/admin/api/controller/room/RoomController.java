package com.winten.greenlight.admin.api.controller.room;

import com.winten.greenlight.admin.domain.room.Room;
import com.winten.greenlight.admin.domain.room.RoomConverter;
import com.winten.greenlight.admin.domain.room.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final RoomConverter roomConverter;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms(
           @ParameterObject RoomSearchRequest request
    ) {
        var result = roomService.getAllRoom(roomConverter.toDto(request));
        var response = result.stream().map(roomConverter::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    // GET /rooms/{roomId}
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(
            @PathVariable final String roomId
    ) {
        var result = roomService.getRoomById(roomId);
        return ResponseEntity.ok(roomConverter.toResponse(result));
    }

    // POST /rooms
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestBody @Valid final RoomCreateRequest request
    ) {
        var result = roomService.createRoom(roomConverter.toDto(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(roomConverter.toResponse(result));
    }

    // PUT /rooms/{roomId}
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable final String roomId,
            @RequestBody final RoomUpdateRequest request
    ) {
        Room room = roomConverter.toDto(request);
        room.setRoomId(roomId);
        // TODO 업데이트 이후
        var result = roomService.updateRoom(room);
        return ResponseEntity.ok(roomConverter.toResponse(result));
    }

    // DELETE /rooms/{roomId}
    @DeleteMapping("/{roomId}")
    public ResponseEntity<RoomResponse> deleteRoom(
            @PathVariable final String roomId
    ) {
        Room result = roomService.deleteRoom(roomId);
        return ResponseEntity.ok(roomConverter.toResponse(result));
    }

    @PostMapping("/cache")
    public ResponseEntity<String> reloadRoomMetaCache() {
        roomService.reloadRoomMetaCache();
        return ResponseEntity.ok("room cache reload successful");
    }
}