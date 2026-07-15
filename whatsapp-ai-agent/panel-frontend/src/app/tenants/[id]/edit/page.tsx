"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { esAdminSegunToken } from "@/lib/auth";
import {
  actualizarContextoTenant,
  ApiError,
  buscarTenant,
  fijarCredencialesInstagram,
  Tenant,
} from "@/lib/api";
import TenantSubNav from "@/components/TenantSubNav";

export default function EditarTenantPage() {
  const listo = useRequireAuth();
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const esAdmin = esAdminSegunToken();

  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [businessContext, setBusinessContext] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);

  const [instagramAccountId, setInstagramAccountId] = useState("");
  const [instagramAccessToken, setInstagramAccessToken] = useState("");
  const [guardandoInstagram, setGuardandoInstagram] = useState(false);
  const [mensajeInstagram, setMensajeInstagram] = useState<string | null>(null);
  const [errorInstagram, setErrorInstagram] = useState<string | null>(null);

  useEffect(() => {
    if (!listo) return;
    buscarTenant(id)
      .then((t) => {
        setTenant(t);
        setBusinessContext(t.businessContext);
      })
      .catch(() => setError("No se pudo cargar el negocio."));
  }, [listo, id]);

  if (!listo) return null;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setMensaje(null);
    setGuardando(true);
    try {
      const actualizado = await actualizarContextoTenant(id, businessContext);
      setTenant(actualizado);
      setMensaje("Catálogo/horarios actualizados.");
    } catch {
      setError("No se pudo guardar el cambio.");
    } finally {
      setGuardando(false);
    }
  }

  async function onGuardarInstagram(e: FormEvent) {
    e.preventDefault();
    setErrorInstagram(null);
    setMensajeInstagram(null);
    setGuardandoInstagram(true);
    try {
      const actualizado = await fijarCredencialesInstagram(id, {
        instagramAccountId,
        accessToken: instagramAccessToken,
      });
      setTenant(actualizado);
      setInstagramAccountId("");
      setInstagramAccessToken("");
      setMensajeInstagram("Credenciales de Instagram guardadas.");
    } catch (err) {
      setErrorInstagram(err instanceof ApiError ? err.message : "No se pudieron guardar las credenciales.");
    } finally {
      setGuardandoInstagram(false);
    }
  }

  if (error && !tenant) {
    return <p className="text-sm text-red-600">{error}</p>;
  }

  if (!tenant) {
    return <p className="text-sm text-gray-500">Cargando...</p>;
  }

  return (
    <div className="max-w-xl">
      <TenantSubNav tenantId={id} />

      <h1 className="text-xl font-semibold">{tenant.businessName}</h1>
      <p className="mb-6 text-sm text-gray-500">WhatsApp: {tenant.whatsappNumber}</p>

      <form onSubmit={onSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium">
            Catálogo, precios, horarios y tono
          </label>
          <textarea
            className="mt-1 h-64 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            value={businessContext}
            onChange={(e) => setBusinessContext(e.target.value)}
            required
          />
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}
        {mensaje && <p className="text-sm text-green-600">{mensaje}</p>}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={guardando}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {guardando ? "Guardando..." : "Guardar cambios"}
          </button>
          <button
            type="button"
            onClick={() => router.push("/tenants")}
            className="rounded-md px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
          >
            Volver
          </button>
        </div>
      </form>

      <section className="mt-8 rounded-md border border-gray-200 bg-white p-4">
        <h2 className="mb-1 text-sm font-medium text-gray-700">Instagram</h2>
        <p className="mb-3 text-xs text-gray-500">
          {tenant.instagramConfigurado
            ? `Conectado (cuenta ${tenant.instagramAccountId}). El bot responde los DM dentro de las 24 horas desde el último mensaje del cliente - Instagram no admite recordatorios proactivos.`
            : "Todavía no está conectado. El bot no responde DM de Instagram hasta cargar la cuenta y el token acá."}
          {!esAdmin && " Solo el admin puede cargar o cambiar estas credenciales."}
        </p>
        {esAdmin && (
          <form onSubmit={onGuardarInstagram} className="space-y-2">
            <input
              value={instagramAccountId}
              onChange={(e) => setInstagramAccountId(e.target.value)}
              placeholder="Instagram account id (cuenta profesional)"
              required
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
            <input
              value={instagramAccessToken}
              onChange={(e) => setInstagramAccessToken(e.target.value)}
              placeholder="Access token de larga duración"
              required
              type="password"
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
            <button
              type="submit"
              disabled={guardandoInstagram}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {guardandoInstagram ? "Guardando..." : "Guardar credenciales"}
            </button>
            {mensajeInstagram && <p className="text-sm text-green-600">{mensajeInstagram}</p>}
            {errorInstagram && <p className="text-sm text-red-600">{errorInstagram}</p>}
          </form>
        )}
      </section>
    </div>
  );
}
