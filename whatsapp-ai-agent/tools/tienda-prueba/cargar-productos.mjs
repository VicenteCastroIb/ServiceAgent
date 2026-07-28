#!/usr/bin/env node
// Carga productos.json (300 productos de prueba) al catálogo de un tenant vía
// la API manual de CatalogController (POST /admin/tenants/{id}/catalogo/productos).
//
// Uso:
//   API_URL=https://tu-backend.onrender.com \
//   ADMIN_USERNAME=admin \
//   ADMIN_PASSWORD=tu-panel-password \
//   TENANT_ID=1 \
//   node cargar-productos.mjs
//
// (En local: API_URL=http://localhost:8080)
//
// El tenant tiene que existir de antes con plan CATALOGO (creado desde el
// panel: Negocios > Nuevo, o PUT /admin/tenants/{id}/plan) - este script solo
// carga los productos, no crea el tenant.

const API_URL = process.env.API_URL || "http://localhost:8080";
const ADMIN_USERNAME = process.env.ADMIN_USERNAME;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;
const TENANT_ID = process.env.TENANT_ID;

if (!ADMIN_USERNAME || !ADMIN_PASSWORD || !TENANT_ID) {
  console.error("Faltan variables de entorno: ADMIN_USERNAME, ADMIN_PASSWORD y TENANT_ID son obligatorias.");
  process.exit(1);
}

async function main() {
  const { default: productos } = await import("./productos.json", { with: { type: "json" } });

  console.log(`Login en ${API_URL} como ${ADMIN_USERNAME}...`);
  const loginRes = await fetch(`${API_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD }),
  });
  if (!loginRes.ok) {
    console.error(`Login falló: ${loginRes.status} ${await loginRes.text()}`);
    process.exit(1);
  }
  // El JWT viaja en una cookie httpOnly (ver AuthController) - la extraemos
  // del header Set-Cookie para reenviarla en cada request siguiente, ya que
  // fetch en Node no maneja cookie jar solo.
  const setCookie = loginRes.headers.get("set-cookie");
  const cookie = setCookie ? setCookie.split(";")[0] : null;
  if (!cookie) {
    console.error("Login OK pero no se recibió cookie de sesión - revisá ADMIN_USERNAME/ADMIN_PASSWORD.");
    process.exit(1);
  }

  console.log(`Cargando ${productos.length} productos al tenant ${TENANT_ID}...`);
  let ok = 0;
  let fallidos = 0;
  for (const p of productos) {
    const res = await fetch(`${API_URL}/admin/tenants/${TENANT_ID}/catalogo/productos`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Cookie: cookie },
      body: JSON.stringify(p),
    });
    if (res.ok) {
      ok++;
    } else {
      fallidos++;
      console.error(`Falló "${p.name}": ${res.status} ${await res.text()}`);
    }
    if ((ok + fallidos) % 25 === 0) {
      console.log(`  ${ok + fallidos}/${productos.length}...`);
    }
  }

  console.log(`Listo: ${ok} productos cargados, ${fallidos} fallidos.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
