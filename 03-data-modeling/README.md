# 03 - Data Modeling

MongoDB doesn't force a schema, but it very much rewards *thinking about
your schema anyway*. This module walks through the two core modeling
decisions - embed or reference - and polymorphic documents, using a
`Customer` / `Order` domain.

## Concepts covered

### Embedding: `Address` inside `Customer`, `OrderItem`/`Payment` inside `Order`

An embedded field lives inside its parent document - there's no separate
collection, no join, no second query. Good candidates share three traits:

- **Bounded size** - a customer has a handful of addresses, not millions.
- **Read/written together** - you basically never want a customer without
  its addresses, or an order without its line items.
- **No independent lifecycle** - an address doesn't outlive its customer,
  an order line item doesn't outlive its order.

`OrderItem` goes a step further and deliberately **duplicates** data
(`productName`, `unitPrice`) that also exists in a `Product` document
elsewhere (see modules 01/02). That's not an oversight - it's a snapshot,
so a historical order still reflects what was actually charged even if the
product's price changes next week. Denormalizing for correctness/read
performance is a normal, deliberate MongoDB modeling choice.

### Referencing: `Order` &rarr; `Customer`, two ways

An order references its customer rather than embedding it, because
customers have their own independent lifecycle and an unbounded, growing
list of orders (embedding "all my orders" inside a customer document would
make that document grow forever). This module shows **two** ways to do
that reference, side by side, so you can see why one is generally
preferred:

**1. Manual reference** (`Order.customerId`, a plain string):
```java
private String customerId;
```
You resolve it yourself, explicitly, when you actually need the customer:
```java
customerRepository.findById(order.getCustomerId())
```
See `OrderController.getCustomerViaManualReference` - exactly one query,
triggered exactly when this code runs.

**2. `@DBRef`** (`Order.customerRef`):
```java
@DBRef
private Customer customerRef;
```
This stores a structured reference (`{"$ref": "customers", "$id": ...}`)
and, by default, Spring Data resolves it **eagerly** - every time you load
an `Order`, it silently fires an extra query to fetch the referenced
`Customer`. Call `GET /api/orders` with a few orders in the database and
watch the console (MongoTemplate DEBUG logging is on): you'll see one
`customers` query *per order*, an N+1 query pattern you didn't explicitly
ask for. `@DBRef(lazy = true)` defers it to first access instead, but
either way you've given up control over when that query happens - which is
the main reason MongoDB's own documentation, and most of the Spring Data
community, recommend manual references over `@DBRef` for anything beyond
trivial single-document lookups.

### Polymorphic documents: `Payment`

`Order.payment` is typed as the abstract `Payment`, backed by two concrete
subclasses, `CreditCardPayment` and `PaypalPayment`. Two independent
mechanisms make this work:

- **Spring Data MongoDB** writes a `_class` field into the stored document
  recording the concrete subtype, and uses it to pick the right subclass
  back out on read. `@TypeAlias("credit_card")` / `@TypeAlias("paypal")` on
  the subclasses shorten that stored value from a full Java class name to
  something readable - look at a saved order in Mongo Express and you'll
  see `"_class": "credit_card"`, not
  `"_class": "com.mongoapp.datamodeling.model.CreditCardPayment"`.
- **Jackson** (`@JsonTypeInfo`/`@JsonSubTypes` on `Payment`) does the same
  job independently for the REST layer, picking a subclass to deserialize
  an incoming request body into based on its `"type"` field.

These two mechanisms don't know about each other - they just happen to
both look at type information to solve the same "which subclass is this"
problem in their respective layers (HTTP JSON vs. BSON).

## Running it

```bash
cd 03-data-modeling
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Create a customer (with an address).
2. Place an order for that customer - add a couple of line items, pick a
   payment type, submit.
3. Click "Show all orders" and look at the raw JSON: each order has both
   `customerId` (a plain string) and `customerRef` (a full, already-
   resolved `Customer` object, courtesy of eager `@DBRef`).
4. Open Mongo Express, find the order in the `orders` collection, and look
   at the *stored* shape - `customerRef` is `{"$ref": "customers", "$id":
   {...}}`, not the full object; Spring Data only inflates it into a full
   object when reading the document back into Java. Also note
   `payment._class` is the short alias, not a full class name.
5. Use "Show orders for customer" (the manual-reference query) and compare
   the console log to loading all orders - one query vs. one-plus-N.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/customers` | Create (with embedded addresses) |
| `GET` | `/api/customers` | List |
| `GET` | `/api/customers/{id}` | Get one |
| `POST` | `/api/orders` | Create (`{customerId, items[], payment}`, `payment.type` is `credit_card` or `paypal`) |
| `GET` | `/api/orders` | List all - triggers eager `@DBRef` resolution per order |
| `GET` | `/api/orders/{id}` | Get one |
| `GET` | `/api/orders/by-customer/{customerId}` | Manual-reference query |
| `GET` | `/api/orders/{id}/customer` | Resolves the customer explicitly via manual reference |

## Things to try

1. Change `@DBRef` to `@DBRef(lazy = true)` on `Order.customerRef`, restart,
   and compare the console log on `GET /api/orders` - the per-order query
   disappears until you actually access `customerRef` (e.g. by returning
   it, since Jackson will still serialize it and trigger the lazy proxy).
2. Add a third `Payment` subtype (e.g. `BankTransferPayment`) - you'll need
   a `@JsonSubTypes.Type` entry, a `@TypeAlias`, and a constructor. Once
   done, both the REST layer and Mongo storage handle it automatically.
3. In Mongo Express, manually edit a stored order's `items` array to add
   an extra item with a `productId` that doesn't exist in any real product
   catalog. Reload the order via the API - it still works, because the
   embedded item is self-contained data, not a live reference.

## What's next

Module 04 covers indexing: how MongoDB actually finds documents fast,
what `.explain()` tells you, and when a query needs a compound, text, or
TTL index.
