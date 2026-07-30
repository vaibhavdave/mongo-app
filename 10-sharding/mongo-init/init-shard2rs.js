try {
  rs.status();
  print("shard2rs already initiated.");
} catch (e) {
  print("Initiating shard2rs...");
  rs.initiate({ _id: "shard2rs", members: [{ _id: 0, host: "shard2:27020" }] });
}
