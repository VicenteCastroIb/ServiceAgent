"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { listarTenants, Tenant } from "@/lib/api";

export default function TenantsPage() {
  const listo = useRequireAuth();
  const [tenants, setTenants] = useState<Tenant[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!listo) return;
    listarTenants()
      .then(setTenants)
      .catch(() => setError("No se pudieron cargar los negocios."));
  }, [listo]);

  if (!listo) return null;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Negocios</h1>
        <Link
          href="/tenants/new"
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + Nuevo negocio
        </Link>
      </div>

      {error && <p className="text-sm text-red-600">{error}</p>}

      <div className="overflow-hidden rounded-md border border-gray-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-gray-50 text-gray-500">
            <tr>
              <th className="px-4 py-2 font-medium">Nombre</th>
              <th className="px-4 py-2 font-medium">WhatsApp</th>
              <th className="px-4 py-2 font-medium">Contexto</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody>
            {tenants?.length === 0 && (
              <tr>
                <td colSpan={4} className="px-4 py-4 text-gray-500">
                  Todavía no hay negocios cargados.
                </td>
              </tr>
            )}
            {tenants?.map((tenant) => (
              <tr key={tenant.id} className="border-t border-gray-100">
                <td className="px-4 py-2">{tenant.businessName}</td>
                <td className="px-4 py-2 text-gray-500">{tenant.whatsappNumber}</td>
                <td className="max-w-xs truncate px-4 py-2 text-gray-500">
                  {tenant.businessContext}
                </td>
                <td className="px-4 py-2 text-right">
                  <Link
                    href={`/tenants/${tenant.id}/edit`}
                    className="text-blue-600 hover:underline"
                  >
                    Editar
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
