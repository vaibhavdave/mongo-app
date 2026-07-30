# 10 - Sharding

Replication (module 09) copies the *same* data to multiple nodes for
redundancy and read scaling - every node has everything. Sharding does the
opposite: it **splits** a collection's data across multiple nodes (shards)
so no single machine has to hold or serve all of it. This is how MongoDB
scales writes and total data size horizontally, past what one replica set
can hold.

This module is mostly conceptual, with a small app for the one piece
that's cheap to demonstrate concretely (targeted vs. scatter-gather
queries). Standing up a full sharded cluster is heavy - the optional
`docker-compose.sharded.yml` in this module runs six containers.

## How a sharded cluster fits together

```
                    ┌────────────┐
   your app  ─────► │   mongos   │  (query router - stateless, you always talk to this)
                    └─────┬──────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
 ┌─────────────┐   ┌─────────────┐   ┌──────────────┐
 │  shard1rs    │   │  shard2rs    │   │  config      │
 │ (replica set)│   │ (replica set)│   │  servers     │
 │ holds a      │   │ holds a      │   │ (replica set)│
 │ range of the │   │ range of the │   │ - metadata:  │
 │ data)        │   │ data)        │   │ which shard  │
 └─────────────┘   └─────────────┘   │ owns which   │
                                       │ data range   │
                                       └──────────────┘
```

- **Shards** each hold a *portion* of the collection's data (and are
  themselves replica sets, for the durability module 09 covers - sharding
  and replication combine, they're not alternatives).
- **Config servers** store the metadata: which range of shard-key values
  lives on which shard.
- **`mongos`** is what your application actually connects to. It looks
  stateless from the outside - it reads the config servers to figure out
  which shard(s) a query needs, forwards the query there, and merges the
  results. This module's app connects to port 27017 either way; against
  `docker-compose.sharded.yml`, that's `mongos`.

## Choosing a shard key

The shard key is the field MongoDB uses to decide which shard a document
belongs on - **chosen once, at the time you shard a collection, and hard
to change later**. This module's `Event.deviceId` was picked over the
alternatives for reasons worth internalizing:

- **High cardinality**: many distinct values (many devices), so data
  actually spreads across shards instead of piling onto one.
- **Not monotonically increasing**: a key like an auto-incrementing
  counter or `_id`'s embedded timestamp means *all new writes* target
  whichever shard currently owns the highest range - a permanent
  bottleneck, one shard doing all the work while the others sit idle. An
  arbitrary-ish identifier like `deviceId` doesn't have this problem.
- **Matches your query patterns**: queries that filter on the shard key
  can be routed to exactly the shard(s) that could possibly match
  ("targeted"). Queries that don't must be sent to *every* shard
  ("scatter-gather") and have their results merged - more expensive, and
  it gets worse as you add shards.

MongoDB also supports **hashed** sharding (`{deviceId: "hashed"}` instead
of `{deviceId: 1}`) - trades away efficient range queries on that field in
exchange for near-perfectly even data distribution, useful when a
naturally good key still clusters unevenly.

## Targeted vs. scatter-gather, demonstrated

`ShardExplainService` runs the same `.explain()` technique from module 04
against two different filters:

- `explainTargetedQuery(deviceId)` - filters on the shard key. On a real
  sharded cluster, mongos can compute exactly which shard to ask.
- `explainScatterGatherQuery(eventType)` - filters on a non-shard-key
  field. mongos has no way to narrow this down, so it asks every shard.

Against the default `docker-compose.yml` (a single plain `mongod`, not
actually sharded), both look similar - there's only one node to ask
either way. The difference only becomes visible against the real cluster.

## Running it

**Default (light, no real sharding - just to explore the API):**

```bash
cd 10-sharding
docker compose up -d
./mvnw spring-boot:run
```

**Optional: the real sharded cluster (~6 containers, heavier):**

```bash
docker compose -f docker-compose.sharded.yml up -d
docker compose -f docker-compose.sharded.yml logs mongo-cluster-init
# wait for "Sharding setup complete." before using the app
./mvnw spring-boot:run   # same app, same port - now talking to mongos
```

Either way, open [http://localhost:8080](http://localhost:8080), seed a
few events across 3-4 different `deviceId`s, then run both explain
buttons and compare. Against the sharded cluster, `explain/scatter-gather`
should report multiple shards queried (or check `rawExplain` directly if
`shardsQueried` doesn't parse cleanly for your MongoDB version - the shape
of sharded explain output has shifted across versions).

## Things to try

1. Against the sharded cluster, check distribution directly: `docker
   compose -f docker-compose.sharded.yml exec mongos mongosh --eval
   'db.getSiblingDB("shardeddb").events.getShardDistribution()'` (after
   seeding enough events - a handful of documents may all happen to land
   on one shard by chance).
2. Reason through what would happen if `Event` had used `timestamp`
   (monotonically increasing) as the shard key instead of `deviceId` -
   which shard would every new write target?
3. Read `mongo-init/init-add-shards.js` - note `sh.shardCollection` is
   called once, at setup; changing a shard key later requires
   reshard/migration tooling, not a quick edit.

## What's next

Module 11 covers change streams and reactive programming: subscribing to
a live feed of database changes and building a WebFlux endpoint that
pushes them to the browser in real time.
