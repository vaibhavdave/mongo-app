package com.mongoapp.replication.controller;

import com.mongoapp.replication.model.Message;
import com.mongoapp.replication.repository.MessageRepository;
import com.mongoapp.replication.service.ReplicaSetService;
import jakarta.validation.Valid;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final MessageRepository messageRepository;
    private final ReplicaSetService replicaSetService;

    public MessageController(MessageRepository messageRepository, ReplicaSetService replicaSetService) {
        this.messageRepository = messageRepository;
        this.replicaSetService = replicaSetService;
    }

    /** Ordinary Spring Data write - always goes to the current primary, default write concern. */
    @PostMapping("/messages")
    public ResponseEntity<Message> create(@Valid @RequestBody Message message) {
        message.setId(null);
        message.setCreatedAt(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(messageRepository.save(message));
    }

    /** Ordinary Spring Data read - always goes to the current primary. */
    @GetMapping("/messages")
    public List<Message> findAll() {
        return messageRepository.findAll();
    }

    @PostMapping("/messages/with-write-concern")
    public ResponseEntity<Document> createWithWriteConcern(@RequestParam String author, @RequestParam String text,
                                                             @RequestParam(defaultValue = "1") String writeConcern) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(replicaSetService.insertMessage(author, text, writeConcern));
    }

    @GetMapping("/messages/with-read-preference")
    public List<Document> findWithReadPreference(@RequestParam(defaultValue = "primary") String readPreference) {
        return replicaSetService.findMessages(readPreference);
    }

    @GetMapping("/replica-set/status")
    public List<Map<String, Object>> replicaSetStatus() {
        return replicaSetService.replicaSetStatus();
    }
}
