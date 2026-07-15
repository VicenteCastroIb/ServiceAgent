"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { esAdminSegunToken } from "@/lib/auth";
import {
  ApiError,
  buscarTenant,
  fijarCredencialesWooCommerce,
  listarProductos,
  Product,
  sincronizarCatalogo,
  Tenant,
} from "@/lib/api";
import TenantSubNav from "@/components/TenantSubNav";

export default function CatalogoPage() {
  const listo = useRequireAuth();
  const params = useParams<{ id: string }>();
  const tenantId = Number(params.id);
  const esAdmin = esAdminSegunToken();

  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [productos, setProductos] = useState<Product[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [url, setUrl] = useState("");
  const [consumerKey, setConsumerKey] = useState("");
  const [consumerSecret, setConsumerSecret] = useState("");
  const [guardandoCredenciales, setGuardandoCredenciales] = useState(false);
  const [mensajeCredenciales, setMensajeCredenciales] = useState<string | null>(null);

  const [sincronizando, setSincronizando] = useState(false);
  const [mensajeSync, setMensajeSync] = useState<string | null>(null);

  function cargar() {
    buscarTenant(tenantId).then(setTenant).catch(() => setError("No se pudo cargar el negocio."));
    listarProductos(tenantId)
      .then(setProductos)
      .catch(() => setError("No se pudieron cargar los productos."));
  }

  useEffect(() => {
    if (listo) cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [listo, tenantId]);

  if (!listo) return null;

  async function onGuardarCredenciales(e: FormEvent) {
    e.preventDefault();
    setGuardandoCredenciales(true);
    setError(null);
    setMensajeCredenciales(null);
    try {
      const actualizado = await fijarCredencialesWooCommerce(tenantId, { url, consumerKey, consumerSecret });
      setTenant(actualizado);
      setUrl("");
      setConsumerKey("");
      setConsumerSecret("");
      setMensajeCredenciales("Credenciales guardadas.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudieron guardar las credenciales.");
    } finally {
      setGuardandoCredenciales(false);
    }
  }

  async function onSincronizar() {
    setSincronizando(true);
    setError(null);
    setMensajeSync(null);
    try {
      const resultado = await sincronizarCatalogo(tenantId);
      setMensajeSync(`Se sincronizaron ${resultado.productosSincronizados} productos.`);
      listarProductos(tenantId).then(setProductos);
    } catch {
      setError("No se pudo sincronizar con la tienda. Revisá la URL y las credenciales.");
    } finally {
      setSincronizando(false);
    }
  }

  return (
    <div className="max-w-3xl">
      <TenantSubNav tenantId={tenantId} />

      <h1 className="text-xl font-semibold">{tenant?.businessName ?? "Catálogo"}</h1>
      {tenant && tenant.plan !== "CATALOGO" && (
        <p className="mt-1 text-sm text-amber-600">
          Este negocio no está en plan Catálogo: el bot todavía no va a recomendar productos ni generar
          links de pago aunque sincronices el catálogo acá.
        </p>
      )}

      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

      <section className="mt-6 rounded-md border border-gray-200 bg-white p-4">
        <h2 className="mb-1 text-sm font-medium text-gray-700">Tienda WooCommerce</h2>
        <p className="mb-3 text-xs text-gray-500">
          {tenant?.wooCommerceConfigurado
            ? "Ya hay credenciales cargadas."
            : "Todavía no hay credenciales cargadas."}
          {!esAdmin && " Solo el admin puede cargarlas o cambiarlas."}
        </p>
        {esAdmin && (
          <form onSubmit={onGuardarCredenciales} className="space-y-2">
            <input
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="URL de la tienda (https://mitienda.cl)"
              required
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
            <input
              value={consumerKey}
              onChange={(e) => setConsumerKey(e.target.value)}
              placeholder="Consumer key"
              required
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
            <input
              value={consumerSecret}
              onChange={(e) => setConsumerSecret(e.target.value)}
              placeholder="Consumer secret"
              required
              type="password"
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
            <button
              type="submit"
              disabled={guardandoCredenciales}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {guardandoCredenciales ? "Guardando..." : "Guardar credenciales"}
            </button>
            {mensajeCredenciales && <p className="text-sm text-green-600">{mensajeCredenciales}</p>}
          </form>
        )}
      </section>

      <section className="mt-6">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-medium text-gray-700">Productos</h2>
          <button
            onClick={onSincronizar}
            disabled={sincronizando}
            className="rounded-md bg-gray-800 px-3 py-1.5 text-xs font-medium text-white hover:bg-gray-900 disabled:opacity-50"
          >
            {sincronizando ? "Sincronizando..." : "Sincronizar ahora"}
          </button>
        </div>
        {mensajeSync && <p className="mb-2 text-sm text-green-600">{mensajeSync}</p>}

        <div className="overflow-hidden rounded-md border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-500">
              <tr>
                <th className="px-4 py-2 font-medium">Nombre</th>
                <th className="px-4 py-2 font-medium">Precio</th>
                <th className="px-4 py-2 font-medium">Stock</th>
                <th className="px-4 py-2 font-medium">Estado</th>
              </tr>
            </thead>
            <tbody>
              {productos?.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-4 text-gray-500">
                    Todavía no hay productos sincronizados.
                  </td>
                </tr>
              )}
              {productos?.map((p) => (
                <tr key={p.id} className="border-t border-gray-100">
                  <td className="px-4 py-2">{p.name}</td>
                  <td className="px-4 py-2 text-gray-500">${p.price.toLocaleString("es-CL")}</td>
                  <td className="px-4 py-2 text-gray-500">{p.stockQuantity ?? "-"}</td>
                  <td className="px-4 py-2 text-gray-500">{p.active ? "Activo" : "Inactivo"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
