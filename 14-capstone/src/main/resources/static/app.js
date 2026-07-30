let customers = [];
let products = [];

async function loadCustomers() {
  const res = await fetch("/api/customers");
  customers = await res.json();
  document.getElementById("customer-rows").innerHTML = customers.map((c) => `<tr><td>${c.id}</td><td>${c.name}</td></tr>`).join("");
  document.getElementById("o-customer").innerHTML = customers.map((c) => `<option value="${c.id}">${c.name} (${c.id})</option>`).join("");
}

document.getElementById("create-customer").addEventListener("click", async () => {
  await fetch("/api/customers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: document.getElementById("c-name").value,
      email: document.getElementById("c-email").value,
      addresses: [],
    }),
  });
  loadCustomers();
});

function renderProducts(list) {
  products = list;
  document.getElementById("product-rows").innerHTML = list.map((p) => `
    <tr><td>${p.id}</td><td>${p.sku}</td><td>${p.name}</td><td>${p.category}</td><td>${p.price.toFixed(2)}</td><td>${p.stock}</td></tr>
  `).join("");
}

async function loadProducts() {
  const res = await fetch("/api/products");
  renderProducts(await res.json());
}

document.getElementById("create-product").addEventListener("click", async () => {
  const payload = {
    sku: document.getElementById("p-sku").value,
    name: document.getElementById("p-name").value,
    description: document.getElementById("p-description").value,
    category: document.getElementById("p-category").value,
    price: parseFloat(document.getElementById("p-price").value || "0"),
    stock: parseInt(document.getElementById("p-stock").value || "0", 10),
  };
  const res = await fetch("/api/products", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    alert(body.error || Object.values(body).join(", ") || "Failed to create product");
  }
  loadProducts();
});

document.getElementById("search-btn").addEventListener("click", async () => {
  const params = new URLSearchParams();
  const category = document.getElementById("s-category").value;
  const maxPrice = document.getElementById("s-maxprice").value;
  if (category) params.set("category", category);
  if (maxPrice) params.set("maxPrice", maxPrice);
  const res = await fetch(`/api/products/search?${params.toString()}`);
  renderProducts(await res.json());
});
document.getElementById("refresh-products").addEventListener("click", loadProducts);

function addItemRow() {
  const container = document.getElementById("item-rows");
  const row = document.createElement("div");
  row.className = "item-row";
  row.innerHTML = `
    <input type="text" placeholder="Product id" class="item-productId">
    <input type="number" placeholder="Qty" class="item-quantity" value="1" min="1">
  `;
  container.appendChild(row);
}
document.getElementById("add-item").addEventListener("click", addItemRow);

document.getElementById("place-order").addEventListener("click", async () => {
  const errorEl = document.getElementById("order-error");
  errorEl.textContent = "";

  const items = Array.from(document.querySelectorAll(".item-row")).map((row) => ({
    productId: row.querySelector(".item-productId").value,
    quantity: parseInt(row.querySelector(".item-quantity").value || "1", 10),
  })).filter((i) => i.productId);

  if (items.length === 0) {
    errorEl.textContent = "Add at least one item with a product id.";
    return;
  }

  const payload = { customerId: document.getElementById("o-customer").value, items };
  const res = await fetch("/api/orders", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    errorEl.textContent = body.error || Object.values(body).join(", ") || `Failed (${res.status})`;
    return;
  }

  document.getElementById("item-rows").innerHTML = "";
  addItemRow();
  loadProducts();
  loadOrders();
});

async function loadOrders() {
  const res = await fetch("/api/orders");
  document.getElementById("orders-output").textContent = JSON.stringify(await res.json(), null, 2);
}
document.getElementById("refresh-orders").addEventListener("click", loadOrders);

document.getElementById("btn-top-products").addEventListener("click", async () => {
  const res = await fetch("/api/reports/top-products?limit=5");
  document.getElementById("report-output").textContent = JSON.stringify(await res.json(), null, 2);
});
document.getElementById("btn-revenue-status").addEventListener("click", async () => {
  const res = await fetch("/api/reports/revenue-by-status");
  document.getElementById("report-output").textContent = JSON.stringify(await res.json(), null, 2);
});

const liveLog = document.getElementById("live-log");
const connStatus = document.getElementById("conn-status");
const source = new EventSource("/api/orders/stream");
source.onopen = () => (connStatus.textContent = "connected");
source.onerror = () => (connStatus.textContent = "disconnected (retrying...)");
source.onmessage = (event) => {
  const change = JSON.parse(event.data);
  const row = document.createElement("div");
  row.className = "event-row";
  const time = new Date().toLocaleTimeString();
  const total = change.order ? `$${change.order.totalAmount?.toFixed(2)}` : "";
  row.textContent = `[${time}] ${change.operationType.toUpperCase()} order ${change.order ? change.order.id : ""} ${total}`;
  liveLog.prepend(row);
  loadOrders();
  loadProducts();
};

addItemRow();
loadCustomers();
loadProducts();
loadOrders();
