document.getElementById("create-btn").addEventListener("click", async () => {
  const payload = {
    name: document.getElementById("p-name").value,
    category: document.getElementById("p-category").value,
    price: parseFloat(document.getElementById("p-price").value || "0"),
    stock: parseInt(document.getElementById("p-stock").value || "0", 10),
  };
  await fetch("/api/products", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  loadAll();
});

document.getElementById("search-btn").addEventListener("click", async () => {
  const category = document.getElementById("q-category").value;
  const res = await fetch(`/api/products/in-stock?category=${encodeURIComponent(category)}`);
  const data = await res.json();
  document.getElementById("output").textContent = JSON.stringify(data, null, 2);
});

async function loadAll() {
  const res = await fetch("/api/products");
  const data = await res.json();
  document.getElementById("output").textContent = JSON.stringify(data, null, 2);
}
document.getElementById("refresh-btn").addEventListener("click", loadAll);

loadAll();
