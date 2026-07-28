"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { useAuth } from "@/lib/auth-context";
import { eliminarTenant, listarTenants, Tenant } from "@/lib/api";
import TenantCard from "@/components/TenantCard";

export default function TenantsPage() {
  const listo = useRequireAuth();
  const { esAdmin } = useAuth();
  const [tenants, setTenants] = useState<Tenant[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [eliminando, setEliminando] = useState<number | null>(null);

  function cargar() {
    listarTenants()
      .then(setTenants)
      .catch(() => setError("No se pudieron cargar los negocios."));
  }

  useEffect(() => {
    if (listo) cargar();
  }, [listo]);

  if (!listo) return null;

  async function onEliminar(tenant: Tenant) {
    const confirmado = window.confirm(
      `¿Eliminar "${tenant.businessName}"? Esto borra también sus citas, profesionales y conversaciones. No se puede deshacer.`
    );
    if (!confirmado) return;

    setEliminando(tenant.id);
    setError(null);
    try {
      await eliminarTenant(tenant.id);
      cargar();
    } catch {
      setError("No se pudo eliminar el negocio.");
    } finally {
      setEliminando(null);
    }
  }

  return (
    <div>
      <div className="mb-7 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-[22px] font-bold tracking-[-0.01em] text-ink">Negocios</h1>
          <p className="mt-0.5 text-sm text-ink/50">Tiendas y locales con el agente activado.</p>
        </div>
        {esAdmin && (
          <Link
            href="/tenants/new"
            className="rounded-[10px] px-4 py-2.5 text-sm font-semibold text-white shadow-[0_8px_20px_rgba(185,134,47,0.25)] transition hover:brightness-105"
            style={{ backgroundImage: "linear-gradient(90deg,#b9862f,#8a5f22)" }}
          >
            + Nuevo negocio
          </Link>
        )}
      </div>

      {error && (
        <p className="mb-4 rounded-lg border border-error/20 bg-error-bg px-3.5 py-2.5 text-sm text-error">
          {error}
        </p>
      )}

      {tenants?.length === 0 && (
        <p className="rounded-xl border border-ink/10 bg-card px-4 py-6 text-center text-sm text-ink/50">
          Todavía no hay negocios cargados.
        </p>
      )}

      <div className="grid gap-4" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(300px,1fr))" }}>
        {tenants?.map((tenant) => (
          <TenantCard
            key={tenant.id}
            tenant={tenant}
            eliminando={eliminando === tenant.id}
            onEliminar={onEliminar}
          />
        ))}
      </div>
    </div>
  );
}
