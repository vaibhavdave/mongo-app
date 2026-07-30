document.getElementById("create-btn").addEventListener("click", async () => {
  const payload = {
    deviceId: document.getElementById("e-device").value,
    eventType: document.getElementById("e-type").value,
    payload: document.getElementById("e-payload").value,
  };
  await fetch("/api/events", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  loadEvents();
});

document.getElementById("targeted-btn").addEventListener("click", async () => {
  const deviceId = document.getElementById("q-device").value;
  const res = await fetch(`/api/events/explain/targeted?deviceId=${encodeURIComponent(deviceId)}`);
  const data = await res.json();
  document.getElementById("explain-output").textContent = JSON.stringify(data, null, 2);
});

document.getElementById("scatter-btn").addEventListener("click", async () => {
  const eventType = document.getElementById("q-type").value;
  const res = await fetch(`/api/events/explain/scatter-gather?eventType=${encodeURIComponent(eventType)}`);
  const data = await res.json();
  document.getElementById("explain-output").textContent = JSON.stringify(data, null, 2);
});

async function loadEvents() {
  const res = await fetch("/api/events");
  const data = await res.json();
  document.getElementById("events-output").textContent = JSON.stringify(data, null, 2);
}
document.getElementById("refresh-btn").addEventListener("click", loadEvents);

loadEvents();
