"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { useAuth } from "@/lib/auth-context";
import {
  actualizarProducto,
  ApiError,
  buscarTenant,
  crearProducto,
  eliminarProducto,
  fijarCredencialesWooCommerce,
  listarProductos,
  Product,
  sincronizarCatalogo,
  Tenant,
} from "@/lib/api";
import TenantSubNav from "@/components/TenantSubNav";

const PRODUCTO_VACIO = { name: "", price: "", category: "", subcategory: "", stockQuantity: "" };

export default function CatalogoPage() {
  const listo = useRequireAuth();
  const params = useParams<{ id: string }>();
  const tenantId = Number(params.id);
  const { esAdmin } = useAuth();

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

  const [nuevoProducto, setNuevoProducto] = useState(PRODUCTO_VACIO);
  const [guardandoProducto, setGuardandoProducto] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [edicion, setEdicion] = useState(PRODUCTO_VACIO);

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

  async function onCrearProducto(e: FormEvent) {
    e.preventDefault();
    setGuardandoProducto(true);
    setError(null);
    try {
      await crearProducto(tenantId, {
        name: nuevoProducto.name,
        price: Number(nuevoProducto.price),
        category: nuevoProducto.category || undefined,
        subcategory: nuevoProducto.subcategory || undefined,
        stockQuantity: nuevoProducto.stockQuantity ? Number(nuevoProducto.stockQuantity) : undefined,
      });
      setNuevoProducto(PRODUCTO_VACIO);
      listarProductos(tenantId).then(setProductos);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo agregar el producto.");
    } finally {
      setGuardandoProducto(false);
    }
  }

  function empezarEdicion(p: Product) {
    setEditandoId(p.id);
    setEdicion({
      name: p.name,
      price: String(p.price),
      category: p.category ?? "",
      subcategory: p.subcategory ?? "",
      stockQuantity: p.stockQuantity != null ? String(p.stockQuantity) : "",
    });
  }

  async function onGuardarEdicion(p: Product) {
    setError(null);
    try {
      const actualizado = await actualizarProducto(tenantId, p.id, {
        name: edicion.name,
        price: Number(edicion.price),
        category: edicion.category || undefined,
        subcategory: edicion.subcategory || undefined,
        stockQuantity: edicion.stockQuantity ? Number(edicion.stockQuantity) : undefined,
        active: p.active,
      });
      setProductos((prev) => prev?.map((x) => (x.id === actualizado.id ? actualizado : x)) ?? prev);
      setEditandoId(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo actualizar el producto.");
    }
  }

  async function onToggleActivo(p: Product) {
    setError(null);
    try {
      const actualizado = await actualizarProducto(tenantId, p.id, {
        name: p.name,
        price: p.price,
        category: p.category ?? undefined,
        subcategory: p.subcategory ?? undefined,
        stockQuantity: p.stockQuantity ?? undefined,
        active: !p.active,
      });
      setProductos((prev) => prev?.map((x) => (x.id === actualizado.id ? actualizado : x)) ?? prev);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo cambiar el estado del producto.");
    }
  }

  async function onEliminarProducto(p: Product) {
    if (!window.confirm(`¿Eliminar "${p.name}" del catálogo? Esta acción no se puede deshacer.`)) return;
    setError(null);
    try {
      await eliminarProducto(tenantId, p.id);
      setProductos((prev) => prev?.filter((x) => x.id !== p.id) ?? prev);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo eliminar el producto.");
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

      <section className="mt-6 rounded-md border border-gray-200 bg-white p-4">
        <h2 className="mb-1 text-sm font-medium text-gray-700">Agregar producto manualmente</h2>
        <p className="mb-3 text-xs text-gray-500">
          Para negocios sin tienda online, o para completar/corregir productos puntuales entre sincronizaciones.
        </p>
        <form onSubmit={onCrearProducto} className="grid grid-cols-2 gap-2 sm:grid-cols-5">
          <input
            value={nuevoProducto.name}
            onChange={(e) => setNuevoProducto({ ...nuevoProducto, name: e.target.value })}
            placeholder="Nombre"
            required
            className="col-span-2 rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none sm:col-span-1"
          />
          <input
            value={nuevoProducto.price}
            onChange={(e) => setNuevoProducto({ ...nuevoProducto, price: e.target.value })}
            placeholder="Precio CLP"
            type="number"
            min="0"
            required
            className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />
          <input
            value={nuevoProducto.category}
            onChange={(e) => setNuevoProducto({ ...nuevoProducto, category: e.target.value })}
            placeholder="Categoría (ej. Ropa)"
            className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />
          <input
            value={nuevoProducto.subcategory}
            onChange={(e) => setNuevoProducto({ ...nuevoProducto, subcategory: e.target.value })}
            placeholder="Subcategoría (ej. Poleras)"
            className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />
          <input
            value={nuevoProducto.stockQuantity}
            onChange={(e) => setNuevoProducto({ ...nuevoProducto, stockQuantity: e.target.value })}
            placeholder="Stock"
            type="number"
            min="0"
            className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={guardandoProducto}
            className="col-span-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50 sm:col-span-5"
          >
            {guardandoProducto ? "Agregando..." : "Agregar producto"}
          </button>
        </form>
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
                <th className="px-4 py-2 font-medium">Categoría</th>
                <th className="px-4 py-2 font-medium">Precio</th>
                <th className="px-4 py-2 font-medium">Stock</th>
                <th className="px-4 py-2 font-medium">Estado</th>
                <th className="px-4 py-2 font-medium">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {productos?.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-4 text-gray-500">
                    Todavía no hay productos. Sincronizá tu tienda o agregá uno manualmente arriba.
                  </td>
                </tr>
              )}
              {productos?.map((p) =>
                editandoId === p.id ? (
                  <tr key={p.id} className="border-t border-gray-100 bg-blue-50/40">
                    <td className="px-2 py-2">
                      <input
                        value={edicion.name}
                        onChange={(e) => setEdicion({ ...edicion, name: e.target.value })}
                        className="w-full rounded-md border border-gray-300 px-2 py-1 text-sm"
                      />
                    </td>
                    <td className="px-2 py-2">
                      <input
                        value={edicion.category}
                        onChange={(e) => setEdicion({ ...edicion, category: e.target.value })}
                        placeholder="Categoría"
                        className="mb-1 w-full rounded-md border border-gray-300 px-2 py-1 text-sm"
                      />
                      <input
                        value={edicion.subcategory}
                        onChange={(e) => setEdicion({ ...edicion, subcategory: e.target.value })}
                        placeholder="Subcategoría"
                        className="w-full rounded-md border border-gray-300 px-2 py-1 text-sm"
                      />
                    </td>
                    <td className="px-2 py-2">
                      <input
                        value={edicion.price}
                        onChange={(e) => setEdicion({ ...edicion, price: e.target.value })}
                        type="number"
                        min="0"
                        className="w-24 rounded-md border border-gray-300 px-2 py-1 text-sm"
                      />
                    </td>
                    <td className="px-2 py-2">
                      <input
                        value={edicion.stockQuantity}
                        onChange={(e) => setEdicion({ ...edicion, stockQuantity: e.target.value })}
                        type="number"
                        min="0"
                        className="w-20 rounded-md border border-gray-300 px-2 py-1 text-sm"
                      />
                    </td>
                    <td className="px-2 py-2 text-gray-500">{p.active ? "Activo" : "Inactivo"}</td>
                    <td className="px-2 py-2">
                      <div className="flex gap-2">
                        <button
                          onClick={() => onGuardarEdicion(p)}
                          className="rounded-md bg-green-600 px-2 py-1 text-xs font-medium text-white hover:bg-green-700"
                        >
                          Guardar
                        </button>
                        <button
                          onClick={() => setEditandoId(null)}
                          className="rounded-md bg-gray-200 px-2 py-1 text-xs font-medium text-gray-700 hover:bg-gray-300"
                        >
                          Cancelar
                        </button>
                      </div>
                    </td>
                  </tr>
                ) : (
                  <tr key={p.id} className="border-t border-gray-100">
                    <td className="px-4 py-2">{p.name}</td>
                    <td className="px-4 py-2 text-gray-500">
                      {p.category ?? "-"}
                      {p.subcategory ? ` > ${p.subcategory}` : ""}
                    </td>
                    <td className="px-4 py-2 text-gray-500">${p.price.toLocaleString("es-CL")}</td>
                    <td className="px-4 py-2 text-gray-500">{p.stockQuantity ?? "-"}</td>
                    <td className="px-4 py-2 text-gray-500">{p.active ? "Activo" : "Inactivo"}</td>
                    <td className="px-4 py-2">
                      <div className="flex gap-2">
                        <button onClick={() => empezarEdicion(p)} className="text-xs font-medium text-blue-600 hover:underline">
                          Editar
                        </button>
                        <button onClick={() => onToggleActivo(p)} className="text-xs font-medium text-gray-600 hover:underline">
                          {p.active ? "Desactivar" : "Activar"}
                        </button>
                        <button onClick={() => onEliminarProducto(p)} className="text-xs font-medium text-red-600 hover:underline">
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
