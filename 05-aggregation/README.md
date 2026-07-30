# 05 - Aggregation Framework

The aggregation framework runs a pipeline of stages - each one
transforming the documents flowing through it - entirely inside MongoDB.
This module builds five reports over an `Order`/`Customer` domain, each
demonstrating a different stage.

## Concepts covered

### Pipelines are just a list of stages

`ReportService.salesByCategory()` reads, top to bottom, exactly like the
Mongo shell version would:

```java
Aggregation agg = newAggregation(
    unwind("items"),                          // one document per line item
    group("items.category")                   // group those by category
        .sum("items.quantity").as("totalQuantity")
        .sum(lineRevenue()).as("totalRevenue")
        .count().as("lineItemCount"),
    project(...).and("_id").as("category"),   // rename the group key
    sort(DESC, "totalRevenue")
);
```

Each stage's output is the next stage's input. `$unwind` turns one order
with 3 items into 3 separate documents (one per item) so `$group` can
aggregate at the line-item level instead of the order level.

### `$group`, `$project`, `$sort`, `$limit` - top products report

`topProducts()` is the same shape as the category report, but groups by
`(productId, productName)` instead, then `$sort`s by revenue descending
and `$limit`s to the top N - a classic "top sellers" query, computed
entirely server-side.

### `$lookup` - joining collections, and its most common gotcha

`ordersWithCustomer()` joins `orders` to `customers` - MongoDB's
equivalent of a SQL join. The catch: `Order.customerId` is stored as a
plain **string**, but `Customer._id` is stored as an **ObjectId** (Spring
Data auto-converts `@Id String` fields to ObjectId on save). `$lookup`
needs matching types on both sides of the join, so the pipeline converts
first:

```java
addFields().addField("customerObjId")
    .withValue(ConvertOperators.ToObjectId.toObjectId("$customerId"))
    .build(),
lookup("customers", "customerObjId", "_id", "customerInfo"),
```

This trips up almost everyone the first time they write a raw aggregation
against Spring Data-managed collections - worth internalizing early.
`unwind("customerInfo", true)` then flattens the (single-element) array
`$lookup` always produces, with `true` meaning "keep the order even if no
matching customer was found" (an orphaned reference doesn't just vanish
from the report).

### `$bucket` - histograms

`priceBuckets()` groups every line item into price ranges (`0-25, 25-50,
50-100, 100-250, 250+`) and counts/sums revenue per bucket - a histogram
computed in the database instead of pulling every item into the JVM to
bucket manually.

### `$facet` - multiple reports, one round trip

`dashboard()` runs three independent sub-pipelines (`byCategory`,
`summary`, `mostExpensiveLineItems`) against the same input collection
inside a **single** `aggregate()` call. Compare this to calling three
separate report endpoints - `$facet` gets you all three in one round trip
to the database.

## Running it

```bash
cd 05-aggregation
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Create 2-3 customers.
2. Place several orders per customer, each with 1-3 line items (the "+ Add
   item" button randomizes category/price/qty for quick seeding - edit
   values if you want a specific scenario).
3. Run each report button and read the JSON output. Try the dashboard last
   and see all three sub-reports in one response.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/customers` | Create |
| `GET` | `/api/customers` | List |
| `POST` | `/api/orders` | Create (`{customerId, items[]}`, item has `category`) |
| `GET` | `/api/orders` | List (raw, no aggregation) |
| `GET` | `/api/reports/sales-by-category` | `$unwind` + `$group` + `$sort` |
| `GET` | `/api/reports/top-products?limit=` | `$unwind` + `$group` + `$sort` + `$limit` |
| `GET` | `/api/reports/orders-with-customer` | `$addFields` + `$lookup` + `$unwind` + `$project` |
| `GET` | `/api/reports/price-buckets` | `$unwind` + `$bucket` |
| `GET` | `/api/reports/dashboard` | `$facet` combining three sub-pipelines |

## Things to try

1. Watch the console (MongoTemplate DEBUG logging is on) while calling
   `/api/reports/dashboard` - notice it's a single aggregate command, even
   though it returns three different report shapes.
2. Delete a customer that still has orders, then run
   `orders-with-customer` again - `customerName`/`customerEmail` come back
   `null` for that order instead of the order disappearing (that's what
   `preserveNullAndEmptyArrays: true` on the `$unwind` buys you).
3. Add a new bucket boundary (e.g. split `250+` into `250-500` and
   `500+`) in `priceBuckets()` and compare the output.
4. Add a `$match` stage as the *first* stage of `salesByCategory()` to
   filter to a single customer's orders before aggregating - this is also
   where you'd want an index on the matched field, same as module 04.

## What's next

Module 06 covers transactions: how to make several writes succeed or fail
together (a real "transfer" style operation), and what read/write concerns
control about consistency.
