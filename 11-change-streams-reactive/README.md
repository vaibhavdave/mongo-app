# 11 - Change Streams & Reactive

Everything so far has been request/response: the app asks MongoDB for
data, MongoDB answers. **Change streams** flip that around - MongoDB
pushes a notification to your app the moment something changes, with no
polling. This module wires a change stream up to the browser over
Server-Sent Events, and uses a fully reactive stack (WebFlux +
`ReactiveMongoRepository`) throughout.

## Concepts covered

### Change streams

```java
reactiveMongoTemplate.changeStream(Product.class)
    .watchCollection("products")
    .listen()   // -> Flux<ChangeStreamEvent<Product>>
```

This opens a stream (built on the same oplog replication module 09's
secondaries read from) that emits an event for every insert, update,
replace, or delete on the `products` collection - from any client, not
just this app. `ChangeStreamController` maps each event to a small DTO and
republishes it as a Server-Sent Event; the browser's built-in
`EventSource` API consumes that stream directly, no client library or
polling loop needed. Try editing a product straight in Mongo Express - the
browser log updates immediately, even though nothing in this app's own
code triggered the write.

Like transactions (module 06), change streams require a replica set (even
a single-node one) - see `docker-compose.yml`'s `mongo-rs-init` service.

### Why WebFlux + `ReactiveMongoRepository` here specifically

A change stream is inherently a long-lived, unbounded flow of events -
exactly what Reactor's `Flux` is built to represent, and exactly what a
traditional one-thread-per-request servlet stack (`spring-boot-starter-web`,
used in every other module) handles awkwardly: keeping a thread blocked
open for every connected SSE client doesn't scale. WebFlux instead uses a
small, fixed pool of event-loop threads and never blocks one waiting on
I/O. `ProductRepository` matches that style: every method returns
`Mono<T>` (0-or-1 result) or `Flux<T>` (0-to-many) instead of a plain
value, and nothing actually queries the database until something
subscribes - Spring WebFlux does that subscribing for you when a
controller method returns a `Mono`/`Flux` directly.

## Running it

```bash
cd 11-change-streams-reactive
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) in **two** browser
tabs:

1. In tab A, create a product - watch it appear in tab B's live log
   instantly, with no manual refresh.
2. Update its stock from tab A, then delete it - watch both operations
   show up live in tab B (note the delete event has no product name/price,
   just an id - see below for why).
3. Open Mongo Express, edit a product's fields directly there - watch it
   show up in both tabs' live logs, proving the change stream sees writes
   from any source, not just this app's own REST endpoints.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/products` | Create (reactive) |
| `GET` | `/api/products` | List (reactive) |
| `PUT` | `/api/products/{id}` | Update |
| `DELETE` | `/api/products/{id}` | Delete |
| `GET` | `/api/products/stream` | Server-Sent Events feed of change stream events |

## Things to try

1. Look at `ChangeStreamController.extractIdFromDocumentKey()` - a delete
   event's `getBody()` is `null` (the document is already gone by the
   time the event arrives), so the id has to come from the change event's
   `documentKey` instead. Confirm this by watching a delete event's JSON
   in the browser's dev tools network tab.
2. Add a `$match` stage to the change stream (via
   `ChangeStreamOptions`/an aggregation pipeline argument to
   `watchCollection`) so it only emits events where `operationType` is
   `"delete"` - a common pattern for building targeted notifications
   instead of processing every change.
3. Compare this controller's style to `ProductController` in module 01
   (blocking `MongoRepository`) - notice there's no `.block()` anywhere in
   this reactive version; blocking a reactive chain to get a plain value
   defeats the point and is generally avoided outside of tests.

## What's next

Module 12 covers security: enabling authentication on MongoDB itself,
creating roles with least-privilege access, and connecting from Spring
Boot with credentials.
