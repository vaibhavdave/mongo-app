// Runs automatically on first container start (mongo's official image
// executes every *.js file in /docker-entrypoint-initdb.d, authenticated
// as the just-created root user, once - only when the data directory is
// empty).
//
// Deliberately creating app_user scoped to ONLY security_db with ONLY
// readWrite - not the root user, not a role with any admin/cluster
// privileges. This is the principle of least privilege: the application
// gets exactly the access it needs to do its job, and nothing more. If
// this app were compromised, the blast radius is "read/write one
// database", not "administer the entire deployment".
db = db.getSiblingDB("security_db");

db.createUser({
  user: "app_user",
  pwd: "app_pass_change_me",
  roles: [{ role: "readWrite", db: "security_db" }],
});

print("Created app_user with readWrite on security_db only.");
