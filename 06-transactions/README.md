# 06 - Transactions & Consistency

Every single write to a single document in MongoDB is always atomic - that
much is true regardless of this module. What multi-document **transactions**
add is atomicity *across* several writes (possibly to several documents, or
even several collections): either all of them apply, or none do. This
module also covers **optimistic locking**, a different (and often better)
tool for a related but distinct problem: detecting *conflicting concurrent*
writes to the *same* document.

## Why this module needs a replica set

MongoDB only supports multi-document transactions against a replica set (or
sharded cluster) - never a standalone `mongod`. `docker-compose.yml` starts
Mongo with `--replSet rs0` and a one-shot `mongo-rs-init` container that
runs `rs.initiate()` once, turning a single node into a (single-member)
replica set. The app's connection string then needs `?replicaSet=rs0` -
without it, the driver won't even attempt a transaction.

## Concepts covered

### `@Transactional` + `MongoTransactionManager`

`@Transactional` on a Spring method does nothing for MongoDB unless a
`MongoTransactionManager` bean exists (`MongoTransactionConfig`) - that's
what actually knows how to start/commit/abort a Mongo session transaction
around the method. `TransferService.transferSafe()` debits one account and
credits another inside one transaction:

```java
@Transactional
public void transferSafe(...) {
    from.setBalance(from.getBalance() - amount);
    accountRepository.save(from);
    // ... if anything below throws, the debit above is rolled back too
    to.setBalance(to.getBalance() + amount);
    accountRepository.save(to);
}
```

`transferUnsafe()` does the exact same two writes with no `@Transactional`.
Trigger a failure between the debit and the credit in both, and compare:
the transactional version leaves balances untouched; the non-transactional
version permanently loses the debited amount. That's the whole point of a
transaction - it's not about performance, it's about not being able to
observe (or persist) a halfway-done multi-step write.

### Read/write concern

`MongoTransactionConfig` sets every transaction in this app to
`WriteConcern.MAJORITY` (don't consider a transaction committed until a
majority of replica set members have durably applied it) and
`ReadConcern.SNAPSHOT` (the transaction sees one consistent point-in-time
snapshot of the data for its whole duration, unaffected by concurrent
writes happening elsewhere). On this module's single-node replica set,
"majority" is trivially satisfied by that one node - the real durability
tradeoff becomes visible in module 09, once there are multiple nodes that
can lag or go down independently.

### Optimistic locking with `@Version`

Transactions solve "these writes must succeed or fail together." A
different, common problem: two requests read the *same* document, both
compute a new value from what they read, and both write back - the second
write silently overwrites the first one's change (a classic lost update).
`@Version` on `BankAccount`/`InventoryItem` fixes this without any locking:
Spring Data folds the current version into the update's filter and bumps
it on write, so a write based on stale data matches nothing and fails
loudly (`OptimisticLockingFailureException`) instead of clobbering the
other write.

`InventoryService.simulateConflict()` reproduces this deterministically -
loads the same document twice (simulating two requests that each read
before either wrote), saves the first successfully, then tries to save the
second and shows it getting rejected.

`InventoryService.reserveWithRetry()` shows the standard recovery pattern:
catch the conflict, re-read the now-current document, retry - up to a
bounded number of attempts.

## Running it

```bash
cd 06-transactions
docker compose up -d
# wait a few seconds for mongo-rs-init to finish - check:
docker compose logs mongo-rs-init
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Create two accounts, note their ids.
2. Check "Simulate failure after debit", click **Transfer (no
   transaction)** - refresh accounts: the source account lost the amount,
   the destination didn't gain it. Money just vanished.
3. Reset the accounts (create fresh ones, or manually fix balances in
   Mongo Express), check the same box, click **Transfer (transactional)**
   instead - refresh: both balances are exactly what they were before.
   The transaction aborted and rolled back the debit too.
4. Create an inventory item, note its id, click "Simulate concurrent write
   conflict" - read the response; only one of the two writes succeeded.
5. Click "Reserve 1 with retry" a few times and watch `version` climb by
   one each time.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/accounts` | Create |
| `GET` | `/api/accounts` | List |
| `POST` | `/api/accounts/transfer?fromId=&toId=&amount=&simulateFailure=` | Transactional transfer |
| `POST` | `/api/accounts/transfer-unsafe?fromId=&toId=&amount=&simulateFailure=` | Same transfer, no transaction |
| `POST` | `/api/inventory` | Create |
| `GET` | `/api/inventory` | List |
| `POST` | `/api/inventory/{id}/simulate-conflict` | Deterministic optimistic-lock conflict demo |
| `POST` | `/api/inventory/{id}/reserve-with-retry?quantity=&maxAttempts=` | Retry-on-conflict pattern |

## Things to try

1. Open the app's console log (`org.springframework.transaction` DEBUG is
   on) and watch a transactional transfer - you'll see the session begin,
   both writes happen inside it, then either commit or abort.
2. Remove the `MongoTransactionManager` bean and re-run the safe transfer
   with a simulated failure - `@Transactional` becomes a no-op and it
   behaves exactly like the unsafe version.
3. Open two browser tabs on the same inventory item and rapidly click two
   different "reserve" actions at nearly the same time (without the retry
   endpoint) to try to trigger a real, not simulated, conflict.

## What's next

Module 07 covers schema validation: enforcing structure at the collection
level with JSON Schema, on top of (or instead of) validating only in
application code.
