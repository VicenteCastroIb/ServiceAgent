"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { esAdminSegunToken } from "@/lib/auth";
import { eliminarTenant, listarTenants, Tenant } from "@/lib/api";
import TenantCard from "@/components/TenantCard";

export default function TenantsPage() {
  const listo = useRequireAuth();
  const esAdmin = esAdminSegunToken();
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
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Negocios</h1>
        {esAdmin && (
          <Link
            href="/tenants/new"
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            + Nuevo negocio
          </Link>
        )}
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {tenants?.length === 0 && (
        <p className="text-sm text-gray-500">Todavía no hay negocios cargados.</p>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
