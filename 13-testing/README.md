# 13 - Testing

Everything so far has been tested by hand, through the browser. This
module is about `src/test/java` instead: three tiers of automated test,
from fastest/most-isolated to slowest/most-realistic, using
**Testcontainers** to run tests against a real, throwaway MongoDB rather
than mocking the database away.

## Why not just mock MongoTemplate/the repository everywhere?

You can, and the fastest tier below does exactly that - but a mock only
verifies your code calls the mock the way you told the mock to expect.
It can't tell you whether a derived query method's generated filter is
actually correct, whether a `Criteria` chain produces the Mongo query you
think it does, or whether your data survives a real BSON round trip.
Testcontainers closes that gap: it starts an actual `mongo:7.0` container
for the test run (no manual `docker compose up`, no shared test database
state between runs) and tears it down afterward.

## The three tiers

### 1. `@WebMvcTest` - web layer only, no database (`ProductControllerWebMvcTest`)

```java
@WebMvcTest(ProductController.class)
class ProductControllerWebMvcTest {
    @MockBean private ProductRepository productRepository;
    @MockBean private ProductSearchService productSearchService;
    ...
}
```

Only the web layer boots - no Spring Data, no MongoDB, no Testcontainers.
`@MockBean` replaces the repository/service with Mockito mocks. This tier
is for verifying HTTP concerns: status codes, JSON shape, validation
wiring (`create_withBlankName_returns400`) - things that have nothing to
do with what's actually in the database. It starts almost instantly.

### 2. `@DataMongoTest` - persistence layer, real Mongo (`ProductRepositoryIT`)

```java
@DataMongoTest
class ProductRepositoryIT extends AbstractIntegrationTest {
    @Autowired private ProductRepository productRepository;
    ...
}
```

Boots only the Mongo-related slice of the context (repositories,
`MongoTemplate`) against a real database - this is the tier for testing
what a derived query method (`findByCategory`, `findByPriceLessThan`)
actually does against real BSON, which mocking the repository can't
verify (you'd just be testing that you called your own mock correctly).

### 3. `@SpringBootTest` - full integration (`ProductSearchServiceIT`)

```java
@SpringBootTest
class ProductSearchServiceIT extends AbstractIntegrationTest {
    @Autowired private ProductSearchService productSearchService;
    ...
}
```

The whole application context, still against the same throwaway Mongo -
service layer, `MongoTemplate` queries, the works, wired together exactly
as they would be at runtime. Slowest tier (most to start up), most
realistic.

A reasonable default: write most tests at tier 1 (cheap, fast feedback on
HTTP/validation concerns), reach for tier 2 when a query's correctness is
the actual thing under test, and use tier 3 sparingly for the handful of
flows worth verifying end-to-end.

## How the throwaway MongoDB works

`AbstractIntegrationTest`, extended by both integration test classes,
does the actual Testcontainers wiring:

```java
@Testcontainers
public abstract class AbstractIntegrationTest {
    @Container
    static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO_DB_CONTAINER.getReplicaSetUrl("testing_db"));
    }
}
```

`@Container` (static) means the container is shared across all test
methods in the class - started once, not once per test.
`@DynamicPropertySource` overrides `spring.data.mongodb.uri` *before* the
Spring context starts, pointing it at whatever host/port Testcontainers
actually assigned (never a fixed port, so tests can run in parallel or on
a CI box already running its own MongoDB without colliding).
`MongoDBContainer` sets up a single-node replica set automatically -
useful groundwork if you wanted to test transaction (module 06) or change
stream (module 11) code the same way.

Each test class also cleans up in `@BeforeEach` (`productRepository.
deleteAll()`) so tests don't leak state into each other within a class.

## Running it

```bash
cd 13-testing
./mvnw test
```

This alone runs all three tiers - no `docker compose up` needed for the
tests themselves (Testcontainers manages its own container via the Docker
daemon directly). You do need Docker installed and reachable.

To run the app itself and poke at it manually:

```bash
docker compose up -d
./mvnw spring-boot:run
```

## Things to try

1. Add a test to `ProductRepositoryIT` for a new derived query method you
   add to `ProductRepository` - watch it fail if the method name doesn't
   produce the filter you expect, before you ever wire it into a
   controller.
2. Temporarily break `ProductSearchService.findInStockByCategorySortedByPrice`
   (e.g. remove the `.gt(0)` stock filter) and watch `ProductSearchServiceIT`
   catch it - that's the value of testing against real data rather than a
   mock that would happily return whatever you told it to.
3. Time the three test classes individually (`./mvnw test
   -Dtest=ProductControllerWebMvcTest`, etc.) and compare - the gap
   between the mocked tier and the Testcontainers tiers is the real cost
   of "the fast test doesn't touch a database."

## What's next

Module 14 is the capstone: one cohesive application combining CRUD,
modeling, indexing, aggregation, transactions, validation, and
change-stream-driven events from every module before it.
