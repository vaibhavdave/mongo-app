document.getElementById("create-btn").addEventListener("click", async () => {
  const errorEl = document.getElementById("create-error");
  errorEl.textContent = "";
  const payload = {
    sku: document.getElementById("p-sku").value,
    name: document.getElementById("p-name").value,
    description: document.getElementById("p-description").value,
    price: parseFloat(document.getElementById("p-price").value || "0"),
    category: document.getElementById("p-category").value,
    tags: [],
  };
  const res = await fetch("/api/products", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    errorEl.textContent = body.error || Object.values(body).join(", ") || `Failed (${res.status})`;
  }
});

document.getElementById("explain-indexed-btn").addEventListener("click", async () => {
  const category = document.getElementById("e-category").value;
  const res = await fetch(`/api/products/explain/indexed?category=${encodeURIComponent(category)}`);
  const data = await res.json();
  document.getElementById("explain-output").textContent = JSON.stringify(data, null, 2);
});

document.getElementById("explain-unindexed-btn").addEventListener("click", async () => {
  const snippet = document.getElementById("e-snippet").value;
  const res = await fetch(`/api/products/explain/unindexed?snippet=${encodeURIComponent(snippet)}`);
  const data = await res.json();
  document.getElementById("explain-output").textContent = JSON.stringify(data, null, 2);
});

document.getElementById("text-search-btn").addEventListener("click", async () => {
  const q = document.getElementById("text-query").value;
  const res = await fetch(`/api/products/search/text?q=${encodeURIComponent(q)}`);
  const data = await res.json();
  document.getElementById("text-output").textContent = JSON.stringify(data, null, 2);
});

document.getElementById("ttl-create-btn").addEventListener("click", async () => {
  const owner = document.getElementById("ttl-owner").value;
  const ttlSeconds = document.getElementById("ttl-seconds").value;
  await fetch(`/api/cart-sessions?owner=${encodeURIComponent(owner)}&ttlSeconds=${ttlSeconds}`, { method: "POST" });
  loadCartSessions();
});
document.getElementById("ttl-refresh-btn").addEventListener("click", loadCartSessions);

async function loadCartSessions() {
  const res = await fetch("/api/cart-sessions");
  const sessions = await res.json();
  document.getElementById("ttl-rows").innerHTML = sessions.map((s) => `
    <tr>
      <td>${s.owner}</td>
      <td>${new Date(s.createdAt).toLocaleTimeString()}</td>
      <td>${new Date(s.expiresAt).toLocaleTimeString()}</td>
    </tr>
  `).join("");
}

loadCartSessions();
