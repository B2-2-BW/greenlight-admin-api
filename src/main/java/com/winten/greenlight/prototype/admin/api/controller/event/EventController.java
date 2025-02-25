package com.winten.greenlight.prototype.admin.api.controller.event;

import com.winten.greenlight.prototype.admin.domain.event.Event;
import com.winten.greenlight.prototype.admin.domain.event.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping("")
    public ResponseEntity<EventListResponseDto> getAllEvents() {
        return ResponseEntity.ok(new EventListResponseDto(eventService.getAllEvents()));
    }

    @GetMapping("/{eventName}")
    public ResponseEntity<EventResponseDto> getEventByName(@PathVariable String eventName) {
        return ResponseEntity.ok(new EventResponseDto(eventService.getEventByName(eventName)));
    }

    @PostMapping("")
    public ResponseEntity<EventResponseDto> createEvent(@RequestBody EventRequestDto requestDto) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new EventResponseDto(eventService.insertEvent(requestDto.toEvent())));
    }

    @PutMapping("/{eventName}")
    public ResponseEntity<EventResponseDto> updateEventByName(@RequestBody EventUpdateRequestDto requestDto,
                                                              @PathVariable String eventName) {
        requestDto.setEventName(eventName);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new EventResponseDto(eventService.updateEvent(requestDto.toEvent())));
    }

    @DeleteMapping("/{eventName}")
    public ResponseEntity<EventResponseDto> deleteEventByName(@PathVariable String eventName) {
        Event event = new Event();
        event.setEventName(eventName);
        return ResponseEntity.ok(new EventResponseDto(eventService.deleteEvent(event)));
    }

}