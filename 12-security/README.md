# 12 - Security

Every earlier module ran MongoDB with **no authentication at all** -
fine for local learning, never fine for anything real. This module turns
auth on, creates an application user scoped to exactly what it needs
(not root), and proves that scoping is actually enforced by asking
MongoDB to do something outside it.

## Concepts covered

### SCRAM authentication

MongoDB's default auth mechanism is SCRAM (Salted Challenge Response
Authentication Mechanism) - the client and server prove they both know
the password without the password itself crossing the wire. `docker-
compose.yml` sets `MONGO_INITDB_ROOT_USERNAME`/`MONGO_INITDB_ROOT_PASSWORD`,
which does two things on first startup: creates that root user, and turns
authentication on for the whole deployment (an unauthenticated
connection, from any client, now gets rejected outright).

### A least-privilege application user

`mongo-init/init-app-user.js` runs automatically on first container start
(the official Mongo image executes every `*.js` file in
`/docker-entrypoint-initdb.d`, authenticated as root, once) and creates:

```javascript
db.createUser({
  user: "app_user",
  pwd: "app_pass_change_me",
  roles: [{ role: "readWrite", db: "security_db" }],
});
```

`app_user` can read and write `security_db` - nothing else. Not other
databases, not user/role administration, not server configuration. This
app connects as `app_user`, never as root - **the application should
never hold more access than it needs.** If this app were compromised (a
dependency vulnerability, a leaked credential, whatever), the blast
radius is "read/write one database's worth of data," not "administer the
entire MongoDB deployment."

### Credentials via environment variables

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://${MONGO_APP_USERNAME:app_user}:${MONGO_APP_PASSWORD:app_pass_change_me}@localhost:27017/security_db?authSource=security_db
```

The defaults here exist purely so this module runs out of the box - in
any real deployment you would not commit even placeholder credentials
like this to version control; they'd be injected at deploy time from a
secrets manager or the environment, with the connection string template
(not the values) being the only thing in source control.

`authSource=security_db` matters: it tells the driver *which database's*
user store to authenticate against. `app_user` was created inside
`security_db` (via `db.getSiblingDB("security_db")`), so that's where its
credentials live - get this wrong (e.g. default to `authSource=admin`,
where the root user lives) and authentication fails even with the correct
password.

### Seeing the boundary enforced, not just asserted

`SecurityDemoService.attemptCrossDatabaseAccess()` deliberately tries to
list collections in the `admin` database using the app's normal
connection (`app_user`) - something its role doesn't grant. MongoDB
rejects it with a real authorization error, caught and surfaced by the
"Attempt cross-database access" button. This isn't a contrived Java
check; it's the database itself refusing the operation.

`whoAmI()` runs MongoDB's `connectionStatus` command, which reports back
exactly what the current connection is authenticated as and authorized to
do - useful for confirming a role grants what you think it grants,
straight from the source of truth.

### A note on encryption (not implemented here)

MongoDB also supports **client-side field level encryption (CSFLE)** -
specific fields are encrypted by the driver *before* they ever leave your
application, using keys MongoDB itself never sees, so even a database
admin (or an attacker with full database access) can't read them without
the separate key management service. It requires a KMS (AWS KMS, Azure
Key Vault, GCP KMS, or a local key file for dev) and meaningfully more
setup than fits a learning module - worth knowing it exists for genuinely
sensitive fields (SSNs, payment details), but out of scope here.

## Running it

```bash
cd 12-security
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Click "Who am I?" - confirms the app is connected as `app_user`, and
   shows the roles MongoDB has on file for it.
2. Click "Attempt cross-database access" - read the denial message
   MongoDB itself returns.
3. Create a product, refresh the list - ordinary readWrite operations on
   `security_db` work fine, because that's exactly what the role grants.
4. Open Mongo Express (logs in as root, since it needs to browse
   everything) and look at `security_db`'s users - the "Database Users"
   view (if your Mongo Express version exposes it) or a shell query
   confirms `app_user` exists with only the one role.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/products` | Create (normal readWrite operation) |
| `GET` | `/api/products` | List |
| `GET` | `/api/security-demo/whoami` | `connectionStatus` - what MongoDB thinks this connection can do |
| `GET` | `/api/security-demo/attempt-cross-database-access` | Deliberately denied operation |

## Things to try

1. Add a second user in `mongo-init/init-app-user.js` with only the
   `read` role (not `readWrite`) on `security_db`, connect a throwaway
   `MongoClient` with those credentials, and confirm a write attempt gets
   rejected the same way the admin-database attempt does here.
2. Change `app_user`'s role in `mongo-init/init-app-user.js` to `dbAdmin`
   instead of `readWrite`, recreate the container (`docker compose down
   -v && docker compose up -d` - the init script only runs on an empty
   data directory), and see ordinary product creation start failing
   instead.
3. Try connecting with the wrong `authSource` (e.g. `admin` instead of
   `security_db`) and see authentication fail even with a correct
   username/password - confirming `authSource` isn't optional decoration.

## What's next

Module 13 covers testing: Testcontainers-based integration tests that
spin up a real, throwaway MongoDB for each test run instead of mocking
the database away.
