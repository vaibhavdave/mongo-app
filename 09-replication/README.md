# 09 - Replication

Everything so far has run against a single MongoDB node (module 06's
single-node "replica set" was only a technical requirement for
transactions, not real replication). This module runs an actual 3-node
replica set and explores what that buys you: durability, read scaling, and
automatic failover.

## What's running

`docker compose up -d` starts `mongo1`/`mongo2`/`mongo3` (each `--replSet
rs0`, on host ports 27017/27018/27019) plus a one-shot `mongo-rs-init`
container that calls `rs.initiate()` once, listing all three members with
`mongo1` given a higher `priority` (making it the preferred, but not
permanent, primary). The app's connection string lists all three hosts -
the driver figures out on its own which one is currently primary, and
keeps working if that changes.

## Concepts covered

### How replication works, briefly

The primary is the only member that accepts writes. Every write is
recorded in the primary's **oplog** (a capped collection of operations),
which secondaries continuously read and replay to stay in sync. If the
primary becomes unreachable, the remaining members hold an election and
promote one of themselves to primary - typically within a few seconds.

### Read preference

Spring Data repositories always read from the primary - there's no
per-repository-query way to change that. To actually route a read
elsewhere, `ReplicaSetService.findMessages()` drops to the driver's
`MongoDatabase`/`MongoCollection` API and sets a `ReadPreference`
explicitly:

| Read preference | Behavior |
|---|---|
| `primary` (default) | Always the primary; strongest consistency, no read scaling |
| `secondaryPreferred` | Prefer a secondary, fall back to primary if none are available |
| `secondary` | Always a secondary - fails if none are reachable |
| `nearest` | Whichever member has the lowest network latency, primary or not |

Reading from secondaries can reduce load on the primary and improve read
throughput, at the cost of **replication lag**: a secondary might be a
few milliseconds (or, under load, more) behind the primary, so a read
immediately after a write can miss that write.

### Write concern

`ReplicaSetService.insertMessage()` lets you pick the write concern per
request:

| Write concern | Behavior |
|---|---|
| `w=1` (default) | Acknowledged once the primary has it - fastest, but a primary crash before replication can lose it |
| `w=2` | Acknowledged once the primary + one secondary have it |
| `w=majority` | Acknowledged once a majority of the set has it - survives any single node failure without data loss |

Time the "Insert" button at different write concerns - `majority` should
consistently take a little longer than `w=1`, because it's actually
waiting for network round trips to other members, not just the primary's
local disk write.

## Running it

```bash
cd 09-replication
docker compose up -d
docker compose logs mongo-rs-init   # confirm it says "Initiating..." then exits
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Click "Check status" - all three members should show up, one `PRIMARY`
   and two `SECONDARY`.
2. Insert a few messages at different write concerns and compare timing.
3. Read with `primary` vs. `secondaryPreferred` - functionally the same
   result set here since nothing else is writing concurrently, but note
   the different code path (driver-level `ReadPreference`) each takes.
4. **Simulate failover** (see the in-app instructions): `docker stop
   replication-mongo1` while reading/writing, watch a new primary get
   elected, then `docker start replication-mongo1` to bring it back.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/messages` | Plain Spring Data write (primary, default concern) |
| `GET` | `/api/messages` | Plain Spring Data read (always primary) |
| `POST` | `/api/messages/with-write-concern?author=&text=&writeConcern=1\|2\|majority` | Configurable write concern |
| `GET` | `/api/messages/with-read-preference?readPreference=primary\|secondary\|secondaryPreferred\|nearest` | Configurable read preference |
| `GET` | `/api/replica-set/status` | Simplified `replSetGetStatus` |

## Things to try

1. While `mongo1` is stopped (mid-failover simulation), call
   `/api/replica-set/status` repeatedly and watch a member transition
   through `(not reachable)`/`SECONDARY`/`PRIMARY` states.
2. Try `readPreference=secondary` while only one secondary is up (stop
   two of the three nodes) - you should eventually see a read failure
   once none are reachable, unlike `secondaryPreferred` which would fall
   back to the primary.
3. Read the mongo1 container logs (`docker compose logs mongo1`) right
   around when you stop/start it - you'll see replica set heartbeat and
   election messages.

## What's next

Module 10 covers sharding: how MongoDB scales writes horizontally across
multiple replica sets, and how to choose a shard key.
