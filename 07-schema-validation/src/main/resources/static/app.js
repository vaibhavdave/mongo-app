document.getElementById("create-btn").addEventListener("click", async () => {
  const errorEl = document.getElementById("create-error");
  errorEl.textContent = "";
  const payload = {
    sku: document.getElementById("p-sku").value,
    name: document.getElementById("p-name").value,
    price: {
      amount: document.getElementById("p-amount").value,
      currency: document.getElementById("p-currency").value,
    },
    category: document.getElementById("p-category").value,
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
  loadProducts();
});

document.getElementById("raw-invalid-btn").addEventListener("click", async () => {
  const res = await fetch("/api/products/raw-insert-invalid", { method: "POST" });
  const body = await res.json().catch(async () => ({ error: await res.text() }));
  document.getElementById("raw-output").textContent = `HTTP ${res.status}\n${JSON.stringify(body, null, 2)}`;
});

async function loadProducts() {
  const res = await fetch("/api/products");
  const data = await res.json();
  document.getElementById("products-output").textContent = JSON.stringify(data, null, 2);
}
document.getElementById("refresh-btn").addEventListener("click", loadProducts);

loadProducts();
