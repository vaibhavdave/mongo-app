const status = sh.status();
print("Adding shards to cluster (safe to re-run - addShard is idempotent)...");

sh.addShard("shard1rs/shard1:27018");
sh.addShard("shard2rs/shard2:27020");

sh.enableSharding("shardeddb");

// Ranged sharding on deviceId: documents are distributed across shards
// by deviceId value range. See the module README for why this field was
// chosen as the shard key, and the hashed-sharding alternative.
sh.shardCollection("shardeddb.events", { deviceId: 1 });

print("Sharding setup complete.");
