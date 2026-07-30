# 14 - Capstone: Order Management

One cohesive application, pulling together a technique from (almost)
every earlier module in this series. A small e-commerce order flow:
browse an indexed, searchable product catalog, place an order that
atomically and transactionally reserves stock, watch it show up live via
a change stream, and check aggregated sales reports.

## What's here, and which module it's from

| Feature | From module | Where |
|---|---|---|
| Embedded `Address` in `Customer`, embedded `OrderItem` snapshots in `Order`, manual `customerId` reference (not `@DBRef`) | 03 - Data Modeling | `model/` |
| Unique `sku` index, compound `category+price` index, text index on name/description | 04 - Indexing | `model/Product.java` |
| Dynamic `Criteria` search + pagination, `$text` search | 02 / 04 / 08 - Querying, Indexing, Text Search | `service/ProductQueryService.java` |
| `$unwind`/`$group`/`$sort`/`$limit` sales reports | 05 - Aggregation | `service/ReportService.java` |
| Atomic conditional stock decrement (`findAndModify`) + `@Transactional` multi-item rollback | 02 / 06 - Querying, Transactions | `service/OrderService.java` |
| `$jsonSchema` validation on `products` | 07 - Schema Validation | `config/ProductCollectionInitializer.java` |
| Live order feed over SSE, backed by a change stream | 11 - Change Streams | `controller/OrderStreamController.java` |
| Testcontainers integration test proving the rollback behavior | 13 - Testing | `src/test/java/.../OrderServiceIT.java` |

Not carried into the capstone (kept in their own modules to avoid
bloating this one): full authentication setup (12), replication/sharding
topology (09/10) - the transactional/change-stream features here do
require the single-node replica set in `docker-compose.yml`, same
requirement as modules 06/11, just not a multi-node cluster.

## The core flow: placing an order

```java
@Transactional
public Order placeOrder(OrderPlaceRequest request) {
    ...
    for (OrderItemRequest itemRequest : request.getItems()) {
        Product decremented = decrementStockAtomically(itemRequest.getProductId(), itemRequest.getQuantity());
        items.add(new OrderItem(...));
    }
    return orderRepository.save(new Order(...));
}
```

Each item's stock decrement is its own atomic, conditional
`findAndModify` (`{stock: {$gte: quantity}} + {$inc: {stock: -quantity}}`
in one server-side operation) - so two concurrent orders for the last
unit of the same product can't both succeed. But a multi-item order needs
more than that: if item 1 succeeds and item 2 fails (not enough stock),
item 1's decrement must not silently stick around with no order to show
for it. That's what `@Transactional` on the whole method buys - a failure
partway through rolls back every write made earlier in the same method
call, not just the one that failed. `OrderServiceIT` (module 13's
pattern, applied here) proves this against a real database rather than
asserting it in prose.

## Running it

```bash
cd 14-capstone
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Create a customer and a few products (vary category/price/stock -
   give at least one product a small stock count, like 2, to make the
   rollback scenario easy to trigger).
2. Search products by category/price, or by text.
3. Place an order with 2+ items, one of them requesting more than its
   available stock - watch it get rejected, and confirm (refresh
   products) that *no* product's stock changed, even the ones that had
   enough.
4. Place a valid order - watch it appear instantly in the live order feed
   panel (no refresh needed), see the reflected stock changes in the
   product table, and check the reports panel.

## Running the tests

```bash
./mvnw test
```

`OrderServiceIT` seeds a customer and two products (one with plenty of
stock, one nearly out), then asserts three scenarios against a real,
throwaway MongoDB (via Testcontainers, same pattern as module 13):
a fully-satisfiable order succeeds and decrements exactly as expected; an
order where one item can't be fulfilled rolls back *all* of it, including
the item that would have succeeded on its own; and an order for an
unknown customer fails before touching any stock at all.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` / `GET` | `/api/customers` | Create / list |
| `POST` / `GET` | `/api/products` | Create / list |
| `GET` | `/api/products/search?category=&maxPrice=&page=&size=` | Dynamic paginated search |
| `GET` | `/api/products/search/text?q=` | Text search |
| `POST` | `/api/orders` | Place an order (`{customerId, items: [{productId, quantity}]}`) |
| `GET` | `/api/orders`, `/api/orders/{id}`, `/api/orders/by-customer/{id}` | Read orders |
| `GET` | `/api/orders/stream` | SSE live order feed |
| `GET` | `/api/reports/top-products?limit=` | Aggregation |
| `GET` | `/api/reports/revenue-by-status` | Aggregation |

## Where to go from here

This series covered MongoDB from basic CRUD through replication,
sharding, security, and testing, each as an isolated, focused module. The
natural next step is picking one thread from this capstone and pulling it
further than any single module did - layer module 12's authentication
setup onto this app, add module 09's real multi-node replica set here
instead of a single node, or extend the change stream to drive an actual
notification system rather than just a live UI log.
