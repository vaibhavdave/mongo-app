try {
  rs.status();
  print("Config server replica set already initiated.");
} catch (e) {
  print("Initiating config server replica set configrs...");
  rs.initiate({
    _id: "configrs",
    configsvr: true,
    members: [{ _id: 0, host: "configsvr:27019" }],
  });
}
