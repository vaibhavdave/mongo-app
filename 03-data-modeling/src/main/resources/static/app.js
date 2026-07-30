let customers = [];

function escapeHtml(value) {
  const div = document.createElement("div");
  div.textContent = value == null ? "" : value;
  return div.innerHTML;
}

async function loadCustomers() {
  const res = await fetch("/api/customers");
  customers = await res.json();

  const rows = document.getElementById("customer-rows");
  rows.innerHTML = customers.map((c) => `
    <tr>
      <td>${c.id}</td>
      <td>${escapeHtml(c.name)}</td>
      <td>${escapeHtml(c.email)}</td>
      <td>${(c.addresses || []).map((a) => `${a.type}: ${a.street}, ${a.city}`).join("; ")}</td>
    </tr>
  `).join("");

  const select = document.getElementById("o-customer");
  select.innerHTML = customers.map((c) => `<option value="${c.id}">${escapeHtml(c.name)} (${c.id})</option>`).join("");
}

document.getElementById("create-customer").addEventListener("click", async () => {
  const payload = {
    name: document.getElementById("c-name").value,
    email: document.getElementById("c-email").value,
    addresses: [{
      type: document.getElementById("c-addr-type").value,
      street: document.getElementById("c-street").value,
      city: document.getElementById("c-city").value,
      state: document.getElementById("c-state").value,
      zip: document.getElementById("c-zip").value,
    }],
  };
  await fetch("/api/customers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  loadCustomers();
});

document.getElementById("add-item").addEventListener("click", () => addItemRow());

function addItemRow() {
  const container = document.getElementById("item-rows");
  const row = document.createElement("div");
  row.className = "item-row";
  row.innerHTML = `
    <input type="text" placeholder="Product id" class="item-productId" value="sku-${Math.floor(Math.random() * 1000)}">
    <input type="text" placeholder="Product name" class="item-productName" value="Widget">
    <input type="number" placeholder="Unit price" class="item-unitPrice" value="9.99" step="0.01">
    <input type="number" placeholder="Qty" class="item-quantity" value="1" min="1">
  `;
  container.appendChild(row);
}

document.getElementById("o-paytype").addEventListener("change", (e) => {
  document.getElementById("pay-fields-credit_card").hidden = e.target.value !== "credit_card";
  document.getElementById("pay-fields-paypal").hidden = e.target.value !== "paypal";
});

document.getElementById("place-order").addEventListener("click", async () => {
  const errorEl = document.getElementById("order-error");
  errorEl.textContent = "";

  const items = Array.from(document.querySelectorAll(".item-row")).map((row) => ({
    productId: row.querySelector(".item-productId").value,
    productName: row.querySelector(".item-productName").value,
    unitPrice: parseFloat(row.querySelector(".item-unitPrice").value || "0"),
    quantity: parseInt(row.querySelector(".item-quantity").value || "1", 10),
  }));

  if (items.length === 0) {
    errorEl.textContent = "Add at least one item.";
    return;
  }

  const payType = document.getElementById("o-paytype").value;
  const amount = items.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0);
  const payment = payType === "credit_card"
    ? { type: "credit_card", amount, brand: document.getElementById("pay-brand").value, cardLast4: document.getElementById("pay-last4").value }
    : { type: "paypal", amount, payerEmail: document.getElementById("pay-email").value };

  const payload = { customerId: document.getElementById("o-customer").value, items, payment };

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
  showAllOrders();
});

async function showAllOrders() {
  const res = await fetch("/api/orders");
  const data = await res.json();
  document.getElementById("orders-output").textContent = JSON.stringify(data, null, 2);
}

document.getElementById("show-all").addEventListener("click", showAllOrders);

document.getElementById("filter-btn").addEventListener("click", async () => {
  const customerId = document.getElementById("filter-customer").value;
  const res = await fetch(`/api/orders/by-customer/${encodeURIComponent(customerId)}`);
  const data = await res.json();
  document.getElementById("orders-output").textContent = JSON.stringify(data, null, 2);
});

addItemRow();
loadCustomers();
showAllOrders();
