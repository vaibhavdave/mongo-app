# 01 - Foundations

The first stop in the MongoDB + Spring Boot learning series: get MongoDB
running, connect a Spring Boot app to it, and do basic CRUD through
`MongoRepository`.

## Concepts covered

### What MongoDB actually is

MongoDB is a **document database**. Instead of rows in tables with a fixed
schema, it stores **documents** (JSON-like objects, encoded on disk as
**BSON** - Binary JSON) inside **collections**. A collection is roughly
analogous to a SQL table, but documents in the same collection don't have to
share the same fields.

| SQL term | MongoDB term |
|---|---|
| Database | Database |
| Table | Collection |
| Row | Document |
| Column | Field |
| Primary key | `_id` field |

Every document has an `_id` field that's unique within its collection. If you
don't supply one, MongoDB generates an `ObjectId` - a 12-byte identifier
(4-byte timestamp + 5-byte random value + 3-byte counter) that's roughly
sortable by creation time. In this module, `Product.id` is typed as `String`;
Spring Data converts between the driver's `ObjectId` and that string for you.

### How Spring Data maps a class to a collection

```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    ...
}
```

- `@Document` tells Spring Data which collection this class maps to.
- `@Id` marks the field that maps to `_id`.
- Every other field is written/read verbatim as a BSON field with the same
  name (you can override the name with `@Field("different_name")`, not used
  here).

There's no migration step, no `CREATE TABLE`. The `products` collection and
the `foundations_db` database are created automatically the moment the first
document is inserted.

### `MongoRepository` and derived queries

`ProductRepository extends MongoRepository<Product, String>` gets you
`save()`, `findById()`, `findAll()`, `deleteById()`, `count()`, etc. for
free. On top of that, Spring Data can derive queries from method names:

```java
List<Product> findByCategory(String category);
// becomes: db.products.find({ category: <value> })

List<Product> findByPriceLessThan(double maxPrice);
// becomes: db.products.find({ price: { $lt: <value> } })

List<Product> findByNameContainingIgnoreCase(String namePart);
// becomes: db.products.find({ name: { $regex: <value>, $options: "i" } })
```

No SQL, no query written by hand - the method signature *is* the query.
(Module 02 covers when you'd drop down to `MongoTemplate`/`Criteria`
instead - roughly: once the query has several optional/dynamic parts.)

## Project layout

```
01-foundations/
  docker-compose.yml          MongoDB + Mongo Express
  src/main/java/.../
    FoundationsApplication.java
    model/Product.java        @Document-mapped POJO
    repository/ProductRepository.java   MongoRepository + derived queries
    controller/ProductController.java   REST CRUD + /search
    exception/                  404 + validation error handling
  src/main/resources/
    application.yml            Mongo connection string
    static/                     plain HTML/CSS/JS UI (no build step)
```

## Running it

**1. Start MongoDB (and Mongo Express, a web GUI for browsing the raw
database):**

```bash
cd 01-foundations
docker compose up -d
```

This starts:
- **MongoDB** on `localhost:27017`, no auth (auth is covered in module 12).
- **Mongo Express** on [http://localhost:8081](http://localhost:8081) - a
  generic web UI to browse databases/collections/documents directly. Use it
  to see the raw BSON your app is writing, independent of the app's own UI.

**2. Run the app:**

```bash
./mvnw spring-boot:run
# or, if you don't have the wrapper: mvn spring-boot:run
```

**3. Open the app's UI:** [http://localhost:8080](http://localhost:8080)

You'll see a form to create products, a search panel that exercises the
derived-query endpoints, and a table of everything in the collection. Every
action goes through the REST API below.

## REST API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/products` | Create a product |
| `GET` | `/api/products` | List all products |
| `GET` | `/api/products/{id}` | Get one product |
| `PUT` | `/api/products/{id}` | Update a product |
| `DELETE` | `/api/products/{id}` | Delete a product |
| `GET` | `/api/products/search?category=&maxPrice=&name=&inStockOnly=` | Derived-query demo (one filter applied at a time) |

Example:

```bash
curl -X POST localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Wireless Mouse","description":"Ergonomic","price":19.99,"category":"electronics","tags":["input","wireless"],"inStock":true}'

curl "localhost:8080/api/products/search?category=electronics"
```

## Things to try

1. **Watch the generated queries.** `application.yml` turns on `DEBUG`
   logging for `MongoTemplate` - create/search a few products and watch the
   console print the actual Mongo operations being executed.
2. **Compare app UI vs. Mongo Express.** Create a product through the app,
   then find it in Mongo Express. Note the `_id` is a 24-character hex
   string (an `ObjectId`), and the document has exactly the fields you sent
   - nothing more.
3. **Insert a document with an extra field directly in Mongo Express**, e.g.
   add `"warehouse": "east-1"` to a product. Reload the app - notice it
   doesn't break. MongoDB doesn't enforce a schema; only your Java class
   decides what it reads back. (Module 07 covers adding real schema
   validation.)
4. **Add your own derived query method**, e.g.
   `List<Product> findByTagsContaining(String tag)`, wire it into `/search`,
   and use it from the UI.
5. **Delete `foundations_mongo_data` volume** (`docker compose down -v`) and
   restart - notice the database is gone. That's expected: this module uses
   a plain Docker volume, not a "real" persistent deployment.

## What's next

Module 02 goes deeper on querying: `MongoTemplate`, the `Criteria` API,
projections, sorting, pagination, and update operators - the tools you reach
for once derived query methods aren't expressive enough.
