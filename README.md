# mongo-app - Learning MongoDB with Spring Boot

A hands-on path through MongoDB, from basic CRUD to replication, sharding,
and change streams - each concept as a small, runnable Spring Boot app with
its own REST API, a browser UI to try it live, and a README explaining the
concept, not just the code.

## How this repo is organized

Every module is a **standalone Maven project** under a numbered folder. They
don't depend on each other - clone, `docker compose up`, `mvn spring-boot:run`,
and go. Each one has:

- A REST API demonstrating that module's MongoDB feature(s).
- A plain HTML/JS page (served by the app itself) to exercise the API from a
  browser, no separate frontend build.
- **Mongo Express** included in its `docker-compose.yml` for a no-code view
  into the raw database.
- A `README.md` that explains the *why*, not just the *how*.

Prerequisites: Java 21, Maven (or use each module's `./mvnw`), Docker +
Docker Compose.

## Syllabus

| # | Module | Status | Covers |
|---|--------|--------|--------|
| 01 | [Foundations](01-foundations/) | ✅ available | Docker Mongo setup, `@Document`/`@Id` mapping, `MongoRepository` CRUD, derived queries |
| 02 | [Querying Deep Dive](02-querying/) | ✅ available | `MongoTemplate`, `Criteria` API, projections, sorting, pagination, update operators |
| 03 | [Data Modeling](03-data-modeling/) | ✅ available | Embedding vs. referencing, one-to-many/many-to-many, `DBRef`, polymorphic documents |
| 04 | [Indexing & Performance](04-indexing/) | ✅ available | Single/compound/unique/text/TTL indexes, `.explain()` |
| 05 | [Aggregation Framework](05-aggregation/) | ✅ available | `$match`, `$group`, `$project`, `$lookup`, `$unwind`, `$facet`, `$bucket` |
| 06 | [Transactions & Consistency](06-transactions/) | ✅ available | Multi-document ACID transactions, `@Transactional`, read/write concerns, optimistic locking |
| 07 | Schema Validation | planned | Collection-level JSON Schema validation, Bean Validation, custom converters |
| 08 | Text & Geo Search | planned | `$text` search indexes, geospatial queries |
| 09 | Replication | planned | Local 3-node replica set, read preference/write concern, failover |
| 10 | Sharding | planned | Shard keys, chunk distribution (conceptual + optional local cluster) |
| 11 | Change Streams & Reactive | planned | Change streams, `ReactiveMongoRepository` + WebFlux |
| 12 | Security | planned | Authentication (SCRAM), roles/users, field-level encryption overview |
| 13 | Testing | planned | Testcontainers-based integration tests |
| 14 | Capstone | planned | A realistic app combining everything above |

## Suggested pace

Work through modules in order - later ones build on concepts (and
sometimes the Docker Compose setup, e.g. the replica set from module 09)
introduced earlier. Each module is small enough to finish, including
reading its README and poking at the UI, in a sitting.
