async function loadAccounts() {
  const res = await fetch("/api/accounts");
  const accounts = await res.json();
  document.getElementById("account-rows").innerHTML = accounts.map((a) => `
    <tr><td>${a.id}</td><td>${a.owner}</td><td>${a.balance.toFixed(2)}</td><td>${a.version}</td></tr>
  `).join("");
}

document.getElementById("create-account").addEventListener("click", async () => {
  await fetch("/api/accounts", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      owner: document.getElementById("a-owner").value,
      balance: parseFloat(document.getElementById("a-balance").value || "0"),
    }),
  });
  loadAccounts();
});
document.getElementById("refresh-accounts").addEventListener("click", loadAccounts);

async function transfer(path) {
  const params = new URLSearchParams({
    fromId: document.getElementById("t-from").value,
    toId: document.getElementById("t-to").value,
    amount: document.getElementById("t-amount").value,
    simulateFailure: document.getElementById("t-simulate").checked,
  });
  const res = await fetch(`${path}?${params.toString()}`, { method: "POST" });
  const body = await res.json().catch(() => ({}));
  document.getElementById("transfer-output").textContent = `HTTP ${res.status}\n${JSON.stringify(body, null, 2)}`;
  loadAccounts();
}
document.getElementById("transfer-safe").addEventListener("click", () => transfer("/api/accounts/transfer"));
document.getElementById("transfer-unsafe").addEventListener("click", () => transfer("/api/accounts/transfer-unsafe"));

async function loadItems() {
  const res = await fetch("/api/inventory");
  const items = await res.json();
  document.getElementById("item-rows").innerHTML = items.map((i) => `
    <tr><td>${i.id}</td><td>${i.sku}</td><td>${i.stock}</td><td>${i.version}</td></tr>
  `).join("");
}

document.getElementById("create-item").addEventListener("click", async () => {
  await fetch("/api/inventory", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      sku: document.getElementById("i-sku").value,
      stock: parseInt(document.getElementById("i-stock").value || "0", 10),
    }),
  });
  loadItems();
});
document.getElementById("refresh-items").addEventListener("click", loadItems);

document.getElementById("simulate-conflict").addEventListener("click", async () => {
  const id = document.getElementById("i-id").value;
  const res = await fetch(`/api/inventory/${id}/simulate-conflict`, { method: "POST" });
  const body = await res.json();
  document.getElementById("inventory-output").textContent = JSON.stringify(body, null, 2);
  loadItems();
});

document.getElementById("reserve-retry").addEventListener("click", async () => {
  const id = document.getElementById("i-id").value;
  const res = await fetch(`/api/inventory/${id}/reserve-with-retry?quantity=1`, { method: "POST" });
  const body = await res.json();
  document.getElementById("inventory-output").textContent = JSON.stringify(body, null, 2);
  loadItems();
});

loadAccounts();
loadItems();
