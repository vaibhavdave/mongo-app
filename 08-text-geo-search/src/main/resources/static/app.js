let allStores = [];

async function loadStores() {
  const res = await fetch("/api/stores");
  allStores = await res.json();
  renderMap(allStores.map((s) => ({ ...s, highlighted: false })), null, null);
}

document.getElementById("create-btn").addEventListener("click", async () => {
  const payload = {
    name: document.getElementById("s-name").value,
    description: document.getElementById("s-description").value,
    category: document.getElementById("s-category").value,
    lng: parseFloat(document.getElementById("s-lng").value),
    lat: parseFloat(document.getElementById("s-lat").value),
  };
  await fetch("/api/stores", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  loadStores();
});

document.getElementById("near-btn").addEventListener("click", async () => {
  const { lng, lat, km } = readQueryInputs();
  const res = await fetch(`/api/stores/near?lng=${lng}&lat=${lat}&maxDistanceKm=${km}`);
  const data = await res.json();
  document.getElementById("geo-output").textContent = JSON.stringify(data, null, 2);
  const highlightedIds = new Set(data.map((r) => r.store.id));
  renderMap(
    allStores.map((s) => ({ ...s, highlighted: highlightedIds.has(s.id) })),
    { lng, lat },
    km
  );
});

document.getElementById("within-btn").addEventListener("click", async () => {
  const { lng, lat, km } = readQueryInputs();
  const res = await fetch(`/api/stores/within-radius?lng=${lng}&lat=${lat}&radiusKm=${km}`);
  const data = await res.json();
  document.getElementById("geo-output").textContent = JSON.stringify(data, null, 2);
  const highlightedIds = new Set(data.map((s) => s.id));
  renderMap(
    allStores.map((s) => ({ ...s, highlighted: highlightedIds.has(s.id) })),
    { lng, lat },
    km
  );
});

function readQueryInputs() {
  return {
    lng: parseFloat(document.getElementById("q-lng").value),
    lat: parseFloat(document.getElementById("q-lat").value),
    km: parseFloat(document.getElementById("q-km").value),
  };
}

document.getElementById("text-btn").addEventListener("click", async () => {
  const q = document.getElementById("text-query").value;
  const res = await fetch(`/api/stores/search/text?q=${encodeURIComponent(q)}`);
  const data = await res.json();
  document.getElementById("text-output").textContent = JSON.stringify(data, null, 2);
});

/**
 * A rough local equirectangular projection: longitude is scaled by
 * cos(average latitude) so that x/y pixel distances are approximately
 * proportional to real-world km near the plotted area. Good enough for a
 * small-area visual aid - not a substitute for the server's actual
 * spherical geo math, which is exact.
 */
function renderMap(stores, center, radiusKm) {
  const svg = document.getElementById("map");
  svg.innerHTML = "";
  const points = stores.map((s) => ({
    lng: s.location.x ?? s.location.coordinates?.[0],
    lat: s.location.y ?? s.location.coordinates?.[1],
    ...s,
  }));
  const all = center ? [...points, { lng: center.lng, lat: center.lat }] : points;
  if (all.length === 0) return;

  const avgLat = all.reduce((sum, p) => sum + p.lat, 0) / all.length;
  const cos = Math.cos((avgLat * Math.PI) / 180);
  const corrected = all.map((p) => ({ x: p.lng * cos, y: p.lat }));

  const minX = Math.min(...corrected.map((p) => p.x));
  const maxX = Math.max(...corrected.map((p) => p.x));
  const minY = Math.min(...corrected.map((p) => p.y));
  const maxY = Math.max(...corrected.map((p) => p.y));
  const span = Math.max(maxX - minX, maxY - minY, 0.01) * 1.3;
  const midX = (minX + maxX) / 2;
  const midY = (minY + maxY) / 2;
  const scale = 340 / span;

  function toPixel(lng, lat) {
    const x = lng * cos;
    const y = lat;
    return { px: 200 + (x - midX) * scale, py: 200 - (y - midY) * scale };
  }

  if (center && radiusKm) {
    const c = toPixel(center.lng, center.lat);
    const radiusDeg = radiusKm / 111;
    const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
    circle.setAttribute("cx", c.px);
    circle.setAttribute("cy", c.py);
    circle.setAttribute("r", radiusDeg * scale);
    circle.setAttribute("fill", "#2f6feb22");
    circle.setAttribute("stroke", "#2f6feb");
    svg.appendChild(circle);
  }

  for (const s of points) {
    const p = toPixel(s.lng, s.lat);
    const dot = document.createElementNS("http://www.w3.org/2000/svg", "circle");
    dot.setAttribute("cx", p.px);
    dot.setAttribute("cy", p.py);
    dot.setAttribute("r", s.highlighted ? 7 : 5);
    dot.setAttribute("fill", s.highlighted ? "#d1242f" : "#57606a");
    svg.appendChild(dot);

    const label = document.createElementNS("http://www.w3.org/2000/svg", "text");
    label.setAttribute("x", p.px + 8);
    label.setAttribute("y", p.py + 4);
    label.setAttribute("font-size", "10");
    label.textContent = s.name;
    svg.appendChild(label);
  }

  if (center) {
    const c = toPixel(center.lng, center.lat);
    const mark = document.createElementNS("http://www.w3.org/2000/svg", "circle");
    mark.setAttribute("cx", c.px);
    mark.setAttribute("cy", c.py);
    mark.setAttribute("r", 4);
    mark.setAttribute("fill", "#2f6feb");
    svg.appendChild(mark);
  }
}

loadStores();
