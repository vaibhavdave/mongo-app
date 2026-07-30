try {
  rs.status();
  print("Replica set already initiated.");
} catch (e) {
  print("Initiating single-node replica set rs0...");
  rs.initiate({
    _id: "rs0",
    members: [{ _id: 0, host: "mongodb:27017" }],
  });
}
