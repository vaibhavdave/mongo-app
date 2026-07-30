# 07 - Schema Validation

MongoDB doesn't require a schema, but it can enforce one if you ask it to.
This module attaches a `$jsonSchema` validator to the `products`
collection itself - enforced by the database for every write, from any
client - and contrasts it with Java-level Bean Validation, which only
runs when a request goes through this app's own REST controller.

## Concepts covered

### Collection-level `$jsonSchema` validation

`ProductCollectionInitializer` runs once at startup and creates the
`products` collection (if it doesn't already exist) with a schema
attached:

```java
MongoJsonSchema schema = MongoJsonSchema.builder()
    .required("sku", "name", "price", "category")
    .properties(
        JsonSchemaProperty.string("sku"),
        JsonSchemaProperty.string("name"),
        JsonSchemaProperty.object("price").properties(
            JsonSchemaProperty.required(JsonSchemaProperty.decimal128("amount")),
            JsonSchemaProperty.required(JsonSchemaProperty.string("currency")
                .possibleValues("USD", "EUR", "GBP"))
        ),
        JsonSchemaProperty.string("category")
            .possibleValues("electronics", "books", "home", "toys")
    ).build();
```

with `schemaValidationAction(ERROR)` (reject bad writes outright - the
alternative, `WARN`, only logs a warning server-side and lets the write
through) and `schemaValidationLevel(STRICT)` (apply to updates as well as
inserts, not just inserts).

The key property: **this is enforced by MongoDB itself**, for every write
to the collection, regardless of what wrote it. Try inserting a document
missing `category` directly in Mongo Express (bypassing this app
entirely) - MongoDB refuses it with a validation error. No application
code ran; the database did it on its own.

### vs. Bean Validation

`Product` also has ordinary `@NotBlank`/`@Valid` annotations, checked by
Spring when a request hits `POST /api/products`. That's useful - fast
feedback, nice error messages - but it's an application-layer concern
only. `ProductController.rawInsertInvalid()` proves the point: it builds
an invalid document by hand and inserts it through the MongoDB driver
directly, skipping `Product`/`@Valid` completely. It's still rejected -
by MongoDB's schema, not by any Java code. **Neither layer replaces the
other**: Bean Validation gives good UX for well-behaved clients; the
database schema is the actual backstop that no client can route around.
Notice also that the database schema restricts `category` to a fixed enum
- something the Bean Validation on `Product` doesn't even attempt - so the
two layers aren't even validating identical things.

### Custom converters: `Money` as `BigDecimal` &harr; `Decimal128`

`Product.price` is a `Money` value object (`BigDecimal amount`, `String
currency`), not a raw `double`. Money as a binary float rounds - `19.99`
as a `double`, written and read back, can come back as
`19.990000000000002`. BSON's `Decimal128` is a proper base-10 floating
point type built for exactly this problem, and Java's `BigDecimal` maps to
it precisely (no representable-value loss).

Spring Data doesn't know how to convert a `Money` object to BSON on its
own, so `MoneyWritingConverter`/`MoneyReadingConverter` (registered via
the `MongoCustomConversions` bean in `MongoConfig`) do it explicitly:

```java
@WritingConverter
class MoneyWritingConverter implements Converter<Money, Document> {
    public Document convert(Money source) {
        return new Document()
            .append("amount", new Decimal128(source.getAmount()))
            .append("currency", source.getCurrency());
    }
}
```

Any time a `Money` field is about to be written, Spring Data calls this
instead of its default reflection-based mapping; `MoneyReadingConverter`
does the inverse on the way back out.

## Running it

```bash
cd 07-schema-validation
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Create a valid product - notice it succeeds.
2. Click "Attempt invalid raw insert" - it comes back as a 422 with
   MongoDB's own validation error message, even though it never touched
   `Product` or Bean Validation.
3. Open Mongo Express, go to `products`, and manually try to insert a
   document with `category: "not-a-real-category"` - rejected, because
   that value isn't in the schema's enum.
4. Look at a saved product's raw document in Mongo Express - `price.amount`
   is stored as a proper decimal type (Mongo Express shows it as
   `Decimal128`), not a float.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/products` | Create (validated by both Bean Validation and the DB schema) |
| `GET` | `/api/products` | List |
| `POST` | `/api/products/raw-insert-invalid` | Bypasses the app's validation entirely; the DB schema still rejects it |

## Things to try

1. Change `validationAction` from `ERROR` to `WARN` in
   `ProductCollectionInitializer`, delete the `products` collection in
   Mongo Express so it gets recreated on next boot, restart the app, and
   retry the invalid raw insert - it now succeeds, but check the MongoDB
   server log for a validation warning. `WARN` is useful when rolling out
   a new schema against existing data you're not 100% sure conforms yet.
2. Add a new required field to the schema (e.g. `sku` must match a regex
   via `.matching(pattern)` if your Spring Data version's
   `JsonSchemaProperty` supports it, or add a new enum-restricted field)
   and see it enforced immediately.
3. Remove the `MoneyWritingConverter`/`MoneyReadingConverter` registration
   and see Spring Data fall back to its default object mapping for
   `Money` - compare the stored shape in Mongo Express.

## What's next

Module 08 covers text and geospatial search: full-text queries with
relevance ranking, and location-based queries (`$near`, `$geoWithin`) over
GeoJSON data.
