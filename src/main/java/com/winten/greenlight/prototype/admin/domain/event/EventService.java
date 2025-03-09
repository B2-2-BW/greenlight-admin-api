package com.winten.greenlight.prototype.admin.domain.event;

import com.winten.greenlight.prototype.admin.db.repository.mapper.EventMapper;
import com.winten.greenlight.prototype.admin.db.repository.redis.EventRedisRepository;
import com.winten.greenlight.prototype.admin.support.error.CoreException;
import com.winten.greenlight.prototype.admin.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventMapper eventMapper;
    private final EventRedisRepository eventRedisRepository;

    public Event getEventByName(String eventName) {
        return eventMapper.findOneByName(eventName)
                .orElseThrow(() -> CoreException.of(ErrorType.EVENT_NOT_FOUND, "이벤트명 `" + eventName +"` 을 찾을 수 없습니다."));
    }

    private void checkDuplicatedEventName(String eventName) {
        if (eventName != null && !eventName.isBlank()) {
            eventMapper.findOneByName(eventName)
                    .ifPresent(s -> {
                        throw CoreException.of(ErrorType.EVENT_NAME_ALREADY_EXISTS, "이미 존재하는 이벤트명입니다.");
                    });
        }
    }

    public List<Event> getAllEvents() {
        return eventMapper.findAll();
    }

    @Transactional
    public Event insertEvent(Event event) {
        checkDuplicatedEventName(event.getEventName());
        eventMapper.save(event);
        eventRedisRepository.put(event);
        return getEventByName(event.getEventName());
    }

    @Transactional
    public Event updateEvent(Event event) {
        if (event.getEventName() == null) {
            throw new CoreException(ErrorType.DEFAULT_ERROR, "Event명은 null일 수 없습니다");
        }
        event.setEventName(event.getEventName().trim());

        getEventByName(event.getEventName()); // 기존 이벤트 없으면 오류

        eventMapper.updateByName(event);
        Event updatedEvent = getEventByName(event.getEventName());
        eventRedisRepository.put(updatedEvent);
        return updatedEvent;
    }

    public Event deleteEvent(Event event) {
        getEventByName(event.getEventName()); // 존재여부 체크
        eventMapper.deleteByName(event.getEventName());
        eventRedisRepository.pop(event.getEventName());
        return event;
    }

    public Integer reloadAllEventCache() {
        List<Event> events = getAllEvents();
        for (Event event : events) {
            eventRedisRepository.put(event);
        }
        return events.size();
    }
}