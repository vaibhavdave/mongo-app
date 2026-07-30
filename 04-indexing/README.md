# 04 - Indexing & Performance

Without an index, MongoDB answers a query by scanning every document in
the collection (a **COLLSCAN**) and checking each one against the filter.
An index lets it jump straight to matching documents instead (an
**IXSCAN**). This module builds several index types and uses
`.explain()` to see the difference directly, plus a TTL index for
auto-expiring documents.

## Important setup detail

Spring Boot does **not** create indexes from `@Indexed`/`@CompoundIndex`/
`@TextIndexed` annotations by default - they're inert unless you turn on:

```yaml
spring:
  data:
    mongodb:
      auto-index-creation: true
```

(already set in this module's `application.yml`). Without it, every
annotation below is just documentation.

## Indexes in this module

| Index | Declared on | Type |
|---|---|---|
| `sku` | `Product.sku` (`@Indexed(unique = true)`) | Single-field, unique |
| `category_price_idx` | `Product` class (`@CompoundIndex`) | Compound: `{category: 1, price: -1}` |
| text index on `name` (weight 2) + `description` | `Product` (`@TextIndexed`) | Text |
| `expiresAt_ttl` | `CartSession.expiresAt` (`@Indexed(expireAfterSeconds = 0)`) | TTL |

### Unique index

Rejects a second document with the same `sku` at the database level - try
it in the UI, it comes back as a 409. This is a real constraint MongoDB
enforces, not just application-level validation.

### Compound index

`{category: 1, price: -1}` supports queries that filter by `category` and
sort by `price` in one index - see "explain indexed query" below.
Compound index field **order matters**: this index can efficiently serve
a query on `category` alone, or `category` + `price`, but not on `price`
alone (that would need `price` as the compound index's leading field, or
its own separate index).

### Text index

A special index type for basic full-text search. `TextCriteria` +
`TextQuery.sortByScore()` runs a `$text` query and ranks results by
relevance (`name` matches count double `description` matches, because of
`weight = 2`). Note: only **one** text index is allowed per collection
(spanning multiple fields, as done here), unlike other index types.

### TTL index

`expireAfterSeconds = 0` on `CartSession.expiresAt` means: delete this
document once the *stored timestamp itself* is in the past. Each document
can carry its own expiry. MongoDB's background TTL monitor sweeps roughly
once a minute, so deletion isn't instant - budget up to ~60s of lag after
the expiry time.

## Reading `.explain()`

`ProductIndexService.explain()` runs the query with
`ExplainVerbosity.EXECUTION_STATS` and pulls out the fields that matter
most day to day:

- **`nReturned`** - documents that matched and were returned.
- **`totalDocsExamined`** - documents MongoDB actually had to look at.
- **`totalKeysExamined`** - index entries scanned (0 for a COLLSCAN).
- **`stages`** - the plan's stage chain; look for `IXSCAN` (index used,
  with an `indexName`) vs. `COLLSCAN` (full scan, no index used).

The healthy case: `totalDocsExamined` close to `nReturned`. A red flag:
`totalDocsExamined` far larger than `nReturned` - the database is doing
much more work than the result size justifies, usually meaning a missing
or badly-shaped index. The full raw explain output is also returned
(`rawExplain`) if you want to dig further than the summary.

## Running it

```bash
cd 04-indexing
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Seed 20-30 products across 2-3 categories (vary price).
2. Run "Explain indexed query" for one of those categories - check
   `stages` shows `IXSCAN` with `indexName: "category_price_idx"`, and
   `totalDocsExamined` is close to `nReturned`.
3. Run "Explain unindexed query" with any description snippet - check
   `stages` shows `COLLSCAN`, and `totalDocsExamined` equals your entire
   product count regardless of how few actually matched.
4. Try the text search with a couple of words from your product
   names/descriptions - results are ranked by relevance.
5. Create a cart session with a short TTL (e.g. 20s), then hit "Refresh
   list" every 15-20 seconds and watch it eventually disappear.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/products` | Create (409 on duplicate `sku`) |
| `GET` | `/api/products` | List all |
| `GET` | `/api/products/explain/indexed?category=` | Explain the compound-index query |
| `GET` | `/api/products/explain/unindexed?snippet=` | Explain the unindexed regex query |
| `GET` | `/api/products/search/text?q=` | `$text` search, sorted by relevance |
| `POST` | `/api/cart-sessions?owner=&ttlSeconds=` | Create a TTL-expiring session |
| `GET` | `/api/cart-sessions` | List current (non-expired) sessions |

## Things to try

1. In Mongo Express, open `products` &rarr; Indexes tab and confirm all
   four indexes exist (`_id_`, `sku_1`, `category_price_idx`, the text
   index, plus `expiresAt_ttl` on `cart_sessions`).
2. Add a query that filters on `price` alone and explain it - notice it's
   still a COLLSCAN, because `category_price_idx` needs `category` as the
   leading field to be usable.
3. Drop the compound index in Mongo Express, re-run the "indexed" explain,
   and watch the plan fall back to COLLSCAN - then restart the app (it
   recreates the index on boot) and confirm it's back to IXSCAN.

## What's next

Module 05 moves from single-query performance to the aggregation
framework: multi-stage pipelines for building reports (totals by category,
top sellers, price buckets) directly in the database.
