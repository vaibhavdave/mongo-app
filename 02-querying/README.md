# 02 - Querying Deep Dive

Module 01 used Spring Data's derived query methods (`findByCategory`, etc.).
Those are great until a query needs several *optional* filters combined at
runtime, a projection, pagination/sorting, or an update operator with no
derived-method equivalent. This module covers the tool you reach for then:
**`MongoTemplate`**.

## Concepts covered

### `MongoTemplate` vs. derived query methods

`MongoRepository` derived methods are convenient but static: one method =
one fixed query shape. `MongoTemplate` (via `Query`/`Criteria`) lets you
build a query's filter *dynamically*, in code, which is exactly what
`ProductQueryService.search()` does in this module - it adds a `Criteria`
for each filter parameter that's actually present, then combines them:

```java
Criteria criteria = new Criteria().andOperator(
    where("category").is("electronics"),
    where("price").gte(10).lte(100)
);
```

This becomes a single Mongo filter:
`{ $and: [ { category: "electronics" }, { price: { $gte: 10, $lte: 100 } } ] }`

### Pagination and sorting

`Query.with(PageRequest.of(page, size, sort))` maps directly to Mongo's
`.skip()`, `.limit()`, and `.sort()`. The service also runs a second
`count()` query (same filter, no pagination) to compute `totalElements` -
this is the standard "two queries" pattern for paginated APIs, since a
single `find()` can't also tell you how many documents *would* have
matched without the limit.

### Projections

`Query.fields().include("name").include("price")` tells MongoDB to return
only those fields (`_id` always comes back too, exclude it explicitly with
`.exclude("_id")` if you don't want it). This reduces the data sent over
the wire - worth doing once documents are large and a view only needs a
few fields.

### Update operators

| Operator | Method | What it does |
|---|---|---|
| `$inc` | `update.inc("stock", delta)` | Atomically add/subtract a number |
| `$addToSet` | `update.addToSet("tags", tag)` | Add to an array only if not already present |
| `$set` | `update.set("inStock", false)` | Overwrite a field |

All of these happen **server-side, atomically** - there's no
read-modify-write race, even with concurrent requests.

### `findAndModify` for atomic conditional updates

`purchase()` is the interesting one: it combines a *filter* (`stock $gte
quantity`) with an *update* (`$inc stock by -quantity`) in one atomic
operation. If two requests try to buy the last item at the same time, only
one will match the filter (since the other's decrement already dropped
stock below the requested quantity) - Mongo guarantees this without any
locking in your Java code. Compare this to doing `find` then `if (stock >=
qty) save(stock - qty)` in application code, which has a race condition
under concurrency.

### `updateMulti` for bulk updates

`markCategoryOutOfStock()` uses `updateMulti` (Mongo's `updateMany`) to
apply one update to every matching document, not just the first.

## Running it

```bash
cd 02-querying
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Seed a few products,
then:

1. Use the search panel - combine category + price range + tag + in-stock,
   change sort field/direction, and page through results.
2. Click "Show name+price projection" and compare the payload to a normal
   `GET /api/products` - notice the projection response has no
   `description`, `stock`, etc.
3. Use **+1 / -1** to `$inc` stock directly, and **Buy** to trigger the
   atomic `findAndModify` purchase (try buying down to 0, then buy again -
   watch it fail with 409 instead of going negative).
4. Use "Mark whole category out of stock" and watch every product in that
   category flip in one request.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/products` | Create (also used to seed data) |
| `GET` | `/api/products` | List all (no pagination) |
| `DELETE` | `/api/products/{id}` | Delete |
| `GET` | `/api/products/query?category=&minPrice=&maxPrice=&tag=&inStock=&page=&size=&sortBy=&sortDir=` | Dynamic Criteria search, paginated |
| `GET` | `/api/products/projection` | Name+price only |
| `PATCH` | `/api/products/{id}/stock?delta=` | `$inc` stock |
| `POST` | `/api/products/{id}/tags?tag=` | `$addToSet` |
| `POST` | `/api/products/{id}/purchase?quantity=` | Atomic conditional decrement, 409 if insufficient stock |
| `POST` | `/api/products/bulk/out-of-stock?category=` | `updateMulti` |

## Things to try

1. Watch the console (`MongoTemplate` DEBUG logging is on) while you search
   - see the actual filter document Spring Data sends.
2. Open two browser tabs, buy the last unit of the same product from both
   at nearly the same time - only one should succeed.
3. Add a new optional filter to `search()`, e.g. `hasTag` vs. `tag` using
   `Criteria.where("tags").all(list)` (all tags must be present, vs. `in`
   which matches any one of them).

## What's next

Module 03 covers data modeling: when to embed data in a document vs.
reference another collection, and how to model one-to-many/many-to-many
relationships and polymorphic documents.
