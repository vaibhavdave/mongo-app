const liveLog = document.getElementById("live-log");
const connStatus = document.getElementById("conn-status");

const source = new EventSource("/api/products/stream");
source.onopen = () => (connStatus.textContent = "connected");
source.onerror = () => (connStatus.textContent = "disconnected (retrying...)");
source.onmessage = (event) => {
  const change = JSON.parse(event.data);
  const row = document.createElement("div");
  row.className = `event-row op-${change.operationType}`;
  const time = new Date().toLocaleTimeString();
  const name = change.product ? change.product.name : "(deleted)";
  row.textContent = `[${time}] ${change.operationType.toUpperCase()} - id=${change.productId} - ${name}`;
  liveLog.prepend(row);
  loadProducts();
};

document.getElementById("create-btn").addEventListener("click", async () => {
  const payload = {
    name: document.getElementById("p-name").value,
    price: parseFloat(document.getElementById("p-price").value || "0"),
    stock: parseInt(document.getElementById("p-stock").value || "0", 10),
  };
  await fetch("/api/products", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
});

document.getElementById("update-btn").addEventListener("click", async () => {
  const id = document.getElementById("p-id").value;
  const res = await fetch(`/api/products`).then((r) => r.json());
  const existing = res.find((p) => p.id === id);
  if (!existing) {
    alert("Product not found - refresh the list first");
    return;
  }
  await fetch(`/api/products/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: existing.name,
      price: existing.price,
      stock: parseInt(document.getElementById("p-new-stock").value || "0", 10),
    }),
  });
});

document.getElementById("delete-btn").addEventListener("click", async () => {
  const id = document.getElementById("p-id").value;
  await fetch(`/api/products/${id}`, { method: "DELETE" });
});

async function loadProducts() {
  const res = await fetch("/api/products");
  const data = await res.json();
  document.getElementById("products-output").textContent = JSON.stringify(data, null, 2);
}
document.getElementById("refresh-btn").addEventListener("click", loadProducts);

loadProducts();
