document.getElementById("status-btn").addEventListener("click", async () => {
  const res = await fetch("/api/replica-set/status");
  const data = await res.json().catch(async () => ({ error: await res.text() }));
  document.getElementById("status-output").textContent = JSON.stringify(data, null, 2);
});

document.getElementById("write-btn").addEventListener("click", async () => {
  const params = new URLSearchParams({
    author: document.getElementById("w-author").value,
    text: document.getElementById("w-text").value,
    writeConcern: document.getElementById("w-concern").value,
  });
  const start = performance.now();
  const res = await fetch(`/api/messages/with-write-concern?${params.toString()}`, { method: "POST" });
  const elapsedMs = Math.round(performance.now() - start);
  const body = await res.json().catch(async () => ({ error: await res.text() }));
  document.getElementById("write-output").textContent = `HTTP ${res.status} in ${elapsedMs}ms\n${JSON.stringify(body, null, 2)}`;
});

document.getElementById("read-btn").addEventListener("click", async () => {
  const pref = document.getElementById("r-pref").value;
  const res = await fetch(`/api/messages/with-read-preference?readPreference=${pref}`);
  const data = await res.json().catch(async () => ({ error: await res.text() }));
  document.getElementById("read-output").textContent = JSON.stringify(data, null, 2);
});

document.getElementById("read-normal-btn").addEventListener("click", async () => {
  const res = await fetch("/api/messages");
  const data = await res.json().catch(async () => ({ error: await res.text() }));
  document.getElementById("read-output").textContent = JSON.stringify(data, null, 2);
});
