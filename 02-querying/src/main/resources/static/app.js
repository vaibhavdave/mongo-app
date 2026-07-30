const API_BASE = "/api/products";
let currentPage = 0;
const pageSize = 5;

const form = document.getElementById("product-form");
const formError = document.getElementById("form-error");
const rowsBody = document.getElementById("rows");
const pageInfo = document.getElementById("page-info");

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  formError.textContent = "";
  const tags = document.getElementById("tags").value
    .split(",").map((t) => t.trim()).filter(Boolean);

  const payload = {
    name: document.getElementById("name").value,
    category: document.getElementById("category").value,
    price: parseFloat(document.getElementById("price").value || "0"),
    stock: parseInt(document.getElementById("stock").value || "0", 10),
    tags,
  };

  const res = await fetch(API_BASE, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    formError.textContent = Object.values(body).join(", ") || `Failed (${res.status})`;
    return;
  }
  form.reset();
  runSearch();
});

document.getElementById("search-btn").addEventListener("click", () => { currentPage = 0; runSearch(); });
document.getElementById("search-clear").addEventListener("click", () => {
  ["s-category", "s-tag"].forEach((id) => (document.getElementById(id).value = ""));
  ["s-minprice", "s-maxprice"].forEach((id) => (document.getElementById(id).value = ""));
  document.getElementById("s-instock").value = "";
  currentPage = 0;
  runSearch();
});
document.getElementById("prev-page").addEventListener("click", () => { if (currentPage > 0) { currentPage--; runSearch(); } });
document.getElementById("next-page").addEventListener("click", () => { currentPage++; runSearch(); });

document.getElementById("projection-btn").addEventListener("click", async () => {
  const res = await fetch(`${API_BASE}/projection`);
  const data = await res.json();
  alert(JSON.stringify(data, null, 2));
});

document.getElementById("bulk-btn").addEventListener("click", async () => {
  const category = document.getElementById("bulk-category").value;
  const res = await fetch(`${API_BASE}/bulk/out-of-stock?category=${encodeURIComponent(category)}`, { method: "POST" });
  const data = await res.json();
  document.getElementById("bulk-result").textContent = `Modified ${data.modifiedCount} product(s) in "${data.category}"`;
  runSearch();
});

async function adjustStock(id, delta) {
  await fetch(`${API_BASE}/${id}/stock?delta=${delta}`, { method: "PATCH" });
  runSearch();
}

async function purchase(id) {
  const res = await fetch(`${API_BASE}/${id}/purchase?quantity=1`, { method: "POST" });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    alert(body.error || "Purchase failed");
  }
  runSearch();
}

async function deleteProduct(id) {
  if (!confirm("Delete this product?")) return;
  await fetch(`${API_BASE}/${id}`, { method: "DELETE" });
  runSearch();
}

function escapeHtml(value) {
  const div = document.createElement("div");
  div.textContent = value;
  return div.innerHTML;
}

function renderRows(products) {
  rowsBody.innerHTML = "";
  for (const p of products) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${escapeHtml(p.name)}</td>
      <td>${escapeHtml(p.category || "")}</td>
      <td>${Number(p.price).toFixed(2)}</td>
      <td>${p.stock}</td>
      <td>${(p.tags || []).map(escapeHtml).join(", ")}</td>
      <td>${p.inStock ? "yes" : "no"}</td>
      <td class="row-actions"></td>
    `;
    const actions = tr.querySelector(".row-actions");
    const mkBtn = (label, fn) => { const b = document.createElement("button"); b.textContent = label; b.className = "secondary"; b.addEventListener("click", fn); return b; };
    actions.appendChild(mkBtn("+1", () => adjustStock(p.id, 1)));
    actions.appendChild(mkBtn("-1", () => adjustStock(p.id, -1)));
    actions.appendChild(mkBtn("Buy", () => purchase(p.id)));
    actions.appendChild(mkBtn("Delete", () => deleteProduct(p.id)));
    rowsBody.appendChild(tr);
  }
}

async function runSearch() {
  const params = new URLSearchParams();
  const category = document.getElementById("s-category").value;
  const minPrice = document.getElementById("s-minprice").value;
  const maxPrice = document.getElementById("s-maxprice").value;
  const tag = document.getElementById("s-tag").value;
  const inStock = document.getElementById("s-instock").value;
  const sortBy = document.getElementById("s-sortby").value;
  const sortDir = document.getElementById("s-sortdir").value;

  if (category) params.set("category", category);
  if (minPrice) params.set("minPrice", minPrice);
  if (maxPrice) params.set("maxPrice", maxPrice);
  if (tag) params.set("tag", tag);
  if (inStock) params.set("inStock", inStock);
  params.set("sortBy", sortBy);
  params.set("sortDir", sortDir);
  params.set("page", currentPage);
  params.set("size", pageSize);

  const res = await fetch(`${API_BASE}/query?${params.toString()}`);
  const data = await res.json();
  renderRows(data.content);
  pageInfo.textContent = `Page ${data.page + 1} of ${Math.max(data.totalPages, 1)} (${data.totalElements} total)`;
}

runSearch();
