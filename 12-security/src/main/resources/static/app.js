document.getElementById("whoami-btn").addEventListener("click", async () => {
  const res = await fetch("/api/security-demo/whoami");
  const data = await res.json();
  document.getElementById("whoami-output").textContent = JSON.stringify(data, null, 2);
});

document.getElementById("attempt-btn").addEventListener("click", async () => {
  const res = await fetch("/api/security-demo/attempt-cross-database-access");
  const data = await res.json();
  document.getElementById("attempt-output").textContent = JSON.stringify(data, null, 2);
});

document.getElementById("create-btn").addEventListener("click", async () => {
  const payload = {
    name: document.getElementById("p-name").value,
    price: parseFloat(document.getElementById("p-price").value || "0"),
  };
  await fetch("/api/products", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  loadProducts();
});

async function loadProducts() {
  const res = await fetch("/api/products");
  const data = await res.json();
  document.getElementById("products-output").textContent = JSON.stringify(data, null, 2);
}
document.getElementById("refresh-btn").addEventListener("click", loadProducts);

loadProducts();
