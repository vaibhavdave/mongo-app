let customers = [];

async function loadCustomers() {
  const res = await fetch("/api/customers");
  customers = await res.json();
  const select = document.getElementById("o-customer");
  select.innerHTML = customers.map((c) => `<option value="${c.id}">${c.name} (${c.id})</option>`).join("");
}

document.getElementById("create-customer").addEventListener("click", async () => {
  const payload = {
    name: document.getElementById("c-name").value,
    email: document.getElementById("c-email").value,
  };
  await fetch("/api/customers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  loadCustomers();
});

function addItemRow() {
  const container = document.getElementById("item-rows");
  const row = document.createElement("div");
  row.className = "item-row";
  const categories = ["electronics", "books", "home", "toys"];
  const category = categories[Math.floor(Math.random() * categories.length)];
  row.innerHTML = `
    <input type="text" placeholder="Product id" class="item-productId" value="sku-${Math.floor(Math.random() * 1000)}">
    <input type="text" placeholder="Product name" class="item-productName" value="Widget">
    <input type="text" placeholder="Category" class="item-category" value="${category}">
    <input type="number" placeholder="Unit price" class="item-unitPrice" value="${(Math.random() * 200).toFixed(2)}" step="0.01">
    <input type="number" placeholder="Qty" class="item-quantity" value="${1 + Math.floor(Math.random() * 3)}" min="1">
  `;
  container.appendChild(row);
}
document.getElementById("add-item").addEventListener("click", addItemRow);

document.getElementById("place-order").addEventListener("click", async () => {
  const errorEl = document.getElementById("order-error");
  errorEl.textContent = "";

  const items = Array.from(document.querySelectorAll(".item-row")).map((row) => ({
    productId: row.querySelector(".item-productId").value,
    productName: row.querySelector(".item-productName").value,
    category: row.querySelector(".item-category").value,
    unitPrice: parseFloat(row.querySelector(".item-unitPrice").value || "0"),
    quantity: parseInt(row.querySelector(".item-quantity").value || "1", 10),
  }));

  if (items.length === 0) {
    errorEl.textContent = "Add at least one item.";
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
    errorEl.textContent = Object.values(body).join(", ") || `Failed (${res.status})`;
    return;
  }
  document.getElementById("item-rows").innerHTML = "";
  addItemRow();
});

async function runReport(path) {
  const res = await fetch(path);
  const data = await res.json();
  document.getElementById("report-output").textContent = JSON.stringify(data, null, 2);
}

document.getElementById("btn-category").addEventListener("click", () => runReport("/api/reports/sales-by-category"));
document.getElementById("btn-top-products").addEventListener("click", () => runReport("/api/reports/top-products?limit=5"));
document.getElementById("btn-lookup").addEventListener("click", () => runReport("/api/reports/orders-with-customer"));
document.getElementById("btn-bucket").addEventListener("click", () => runReport("/api/reports/price-buckets"));
document.getElementById("btn-dashboard").addEventListener("click", () => runReport("/api/reports/dashboard"));

addItemRow();
loadCustomers();
