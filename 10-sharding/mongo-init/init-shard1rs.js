try {
  rs.status();
  print("shard1rs already initiated.");
} catch (e) {
  print("Initiating shard1rs...");
  rs.initiate({ _id: "shard1rs", members: [{ _id: 0, host: "shard1:27018" }] });
}
