const API_BASE = "/api/products";

const form = document.getElementById("product-form");
const formTitle = document.getElementById("form-title");
const idField = document.getElementById("product-id");
const nameField = document.getElementById("name");
const descriptionField = document.getElementById("description");
const priceField = document.getElementById("price");
const categoryField = document.getElementById("category");
const tagsField = document.getElementById("tags");
const inStockField = document.getElementById("inStock");
const submitBtn = document.getElementById("submit-btn");
const cancelEditBtn = document.getElementById("cancel-edit");
const formError = document.getElementById("form-error");

const rowsBody = document.getElementById("product-rows");
const emptyMsg = document.getElementById("empty-msg");

const searchCategory = document.getElementById("search-category");
const searchMaxPrice = document.getElementById("search-maxprice");
const searchName = document.getElementById("search-name");
const searchInStock = document.getElementById("search-instock");

document.getElementById("search-btn").addEventListener("click", runSearch);
document.getElementById("search-clear").addEventListener("click", () => {
  searchCategory.value = "";
  searchMaxPrice.value = "";
  searchName.value = "";
  searchInStock.checked = false;
  loadProducts();
});

form.addEventListener("submit", onSubmit);
cancelEditBtn.addEventListener("click", resetForm);

function readForm() {
  const tags = tagsField.value
    .split(",")
    .map((t) => t.trim())
    .filter((t) => t.length > 0);

  return {
    name: nameField.value,
    description: descriptionField.value,
    price: parseFloat(priceField.value || "0"),
    category: categoryField.value,
    tags,
    inStock: inStockField.checked,
  };
}

async function onSubmit(event) {
  event.preventDefault();
  formError.textContent = "";

  const payload = readForm();
  const id = idField.value;
  const isEdit = Boolean(id);

  const response = await fetch(isEdit ? `${API_BASE}/${id}` : API_BASE, {
    method: isEdit ? "PUT" : "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    formError.textContent = Object.values(body).join(", ") || `Request failed (${response.status})`;
    return;
  }

  resetForm();
  loadProducts();
}

function resetForm() {
  form.reset();
  idField.value = "";
  formTitle.textContent = "Add a product";
  submitBtn.textContent = "Create";
  cancelEditBtn.hidden = true;
  formError.textContent = "";
}

function startEdit(product) {
  idField.value = product.id;
  nameField.value = product.name || "";
  descriptionField.value = product.description || "";
  priceField.value = product.price;
  categoryField.value = product.category || "";
  tagsField.value = (product.tags || []).join(", ");
  inStockField.checked = Boolean(product.inStock);

  formTitle.textContent = `Edit "${product.name}"`;
  submitBtn.textContent = "Save changes";
  cancelEditBtn.hidden = false;
  window.scrollTo({ top: 0, behavior: "smooth" });
}

async function deleteProduct(id) {
  if (!confirm("Delete this product?")) return;
  await fetch(`${API_BASE}/${id}`, { method: "DELETE" });
  loadProducts();
}

function renderProducts(products) {
  rowsBody.innerHTML = "";
  emptyMsg.hidden = products.length > 0;

  for (const product of products) {
    const tr = document.createElement("tr");

    const created = product.createdAt ? new Date(product.createdAt).toLocaleString() : "";

    tr.innerHTML = `
      <td>${escapeHtml(product.name)}</td>
      <td>${escapeHtml(product.category || "")}</td>
      <td>${Number(product.price).toFixed(2)}</td>
      <td>${(product.tags || []).map(escapeHtml).join(", ")}</td>
      <td>${product.inStock ? "yes" : "no"}</td>
      <td>${created}</td>
      <td class="row-actions"></td>
    `;

    const actionsCell = tr.querySelector(".row-actions");

    const editBtn = document.createElement("button");
    editBtn.textContent = "Edit";
    editBtn.addEventListener("click", () => startEdit(product));

    const deleteBtn = document.createElement("button");
    deleteBtn.textContent = "Delete";
    deleteBtn.addEventListener("click", () => deleteProduct(product.id));

    actionsCell.appendChild(editBtn);
    actionsCell.appendChild(deleteBtn);
    rowsBody.appendChild(tr);
  }
}

function escapeHtml(value) {
  const div = document.createElement("div");
  div.textContent = value;
  return div.innerHTML;
}

async function loadProducts() {
  const response = await fetch(API_BASE);
  const products = await response.json();
  renderProducts(products);
}

async function runSearch() {
  const params = new URLSearchParams();
  if (searchCategory.value) params.set("category", searchCategory.value);
  if (searchMaxPrice.value) params.set("maxPrice", searchMaxPrice.value);
  if (searchName.value) params.set("name", searchName.value);
  if (searchInStock.checked) params.set("inStockOnly", "true");

  const response = await fetch(`${API_BASE}/search?${params.toString()}`);
  const products = await response.json();
  renderProducts(products);
}

loadProducts();
