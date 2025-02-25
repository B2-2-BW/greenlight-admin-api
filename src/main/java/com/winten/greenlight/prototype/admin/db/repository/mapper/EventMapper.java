package com.winten.greenlight.prototype.admin.db.repository.mapper;

import com.winten.greenlight.prototype.admin.domain.event.Event;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EventMapper {
    Optional<Event> findOneByName(String eventName);
    List<Event> findAll();
    void save(Event event);
    void updateByName(Event event);
    void deleteByName(String eventName);
}