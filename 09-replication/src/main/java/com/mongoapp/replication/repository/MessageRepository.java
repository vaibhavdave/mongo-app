package com.mongoapp.replication.repository;

import com.mongoapp.replication.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {
}
