"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { actualizarContextoTenant, buscarTenant, Tenant } from "@/lib/api";

export default function EditarTenantPage() {
  const listo = useRequireAuth();
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [businessContext, setBusinessContext] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState<string | null>(null);

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

  if (error && !tenant) {
    return <p className="text-sm text-red-600">{error}</p>;
  }

  if (!tenant) {
    return <p className="text-sm text-gray-500">Cargando...</p>;
  }

  return (
    <div className="max-w-xl">
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
    </div>
  );
}
