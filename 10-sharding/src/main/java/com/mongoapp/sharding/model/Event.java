package com.mongoapp.sharding.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * deviceId is the chosen shard key for this collection (set up by
 * mongo-init/init-add-shards.js when running against the sharded
 * cluster: sh.shardCollection("shardeddb.events", {deviceId: 1})).
 * MongoDB uses this field's value to decide which shard owns each
 * document - see the module README for why deviceId, not eventType or
 * _id, was chosen.
 */
@Document(collection = "events")
@Data
@NoArgsConstructor
public class Event {

    @Id
    private String id;

    @NotBlank
    private String deviceId;

    @NotBlank
    private String eventType;

    private String payload;

    private Instant timestamp;
}
