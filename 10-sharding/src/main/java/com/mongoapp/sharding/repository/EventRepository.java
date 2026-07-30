package com.mongoapp.sharding.repository;

import com.mongoapp.sharding.model.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    List<Event> findByDeviceId(String deviceId);
}
