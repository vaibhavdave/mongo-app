package com.mongoapp.sharding.controller;

import com.mongoapp.sharding.model.Event;
import com.mongoapp.sharding.repository.EventRepository;
import com.mongoapp.sharding.service.ShardExplainService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository eventRepository;
    private final ShardExplainService explainService;

    public EventController(EventRepository eventRepository, ShardExplainService explainService) {
        this.eventRepository = eventRepository;
        this.explainService = explainService;
    }

    @PostMapping
    public ResponseEntity<Event> create(@Valid @RequestBody Event event) {
        event.setId(null);
        event.setTimestamp(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(eventRepository.save(event));
    }

    @GetMapping
    public List<Event> findAll() {
        return eventRepository.findAll();
    }

    @GetMapping("/by-device/{deviceId}")
    public List<Event> findByDevice(@PathVariable String deviceId) {
        return eventRepository.findByDeviceId(deviceId);
    }

    @GetMapping("/explain/targeted")
    public Map<String, Object> explainTargeted(@RequestParam String deviceId) {
        return explainService.explainTargetedQuery(deviceId);
    }

    @GetMapping("/explain/scatter-gather")
    public Map<String, Object> explainScatterGather(@RequestParam String eventType) {
        return explainService.explainScatterGatherQuery(eventType);
    }
}
