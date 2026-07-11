"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { crearTenant } from "@/lib/api";

export default function NuevoTenantPage() {
  const listo = useRequireAuth();
  const router = useRouter();
  const [businessName, setBusinessName] = useState("");
  const [whatsappNumber, setWhatsappNumber] = useState("");
  const [businessContext, setBusinessContext] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  if (!listo) return null;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setGuardando(true);
    try {
      await crearTenant({ businessName, whatsappNumber, businessContext });
      router.push("/tenants");
    } catch {
      setError("No se pudo crear el negocio. Revisá los datos.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="max-w-xl">
      <h1 className="mb-6 text-xl font-semibold">Nuevo negocio</h1>

      <form onSubmit={onSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium">Nombre del negocio</label>
          <input
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium">Número de WhatsApp</label>
          <input
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            placeholder="whatsapp:+56912345678"
            value={whatsappNumber}
            onChange={(e) => setWhatsappNumber(e.target.value)}
            required
          />
          <p className="mt-1 text-xs text-gray-500">
            Formato igual al que usa Twilio, ej: whatsapp:+56912345678
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium">
            Catálogo, precios, horarios y tono
          </label>
          <textarea
            className="mt-1 h-48 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            placeholder="Rubro, horario de atención, tono, catálogo con precios, política de envíos, etc."
            value={businessContext}
            onChange={(e) => setBusinessContext(e.target.value)}
            required
          />
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={guardando}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {guardando ? "Creando..." : "Crear negocio"}
          </button>
          <button
            type="button"
            onClick={() => router.push("/tenants")}
            className="rounded-md px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-100"
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  );
}
