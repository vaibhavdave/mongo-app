package com.mongoapp.replication.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "messages")
@Data
@NoArgsConstructor
public class Message {

    @Id
    private String id;

    @NotBlank
    private String author;

    @NotBlank
    private String text;

    private Instant createdAt;
}
