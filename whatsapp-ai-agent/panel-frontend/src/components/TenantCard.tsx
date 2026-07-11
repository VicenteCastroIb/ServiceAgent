"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  obtenerSuscripcion,
  registrarPagoManual,
  Tenant,
  TenantSubscription,
} from "@/lib/api";
import { esAdminSegunToken } from "@/lib/auth";

const NOMBRE_PLAN: Record<Tenant["plan"], string> = {
  BASICO: "Básico",
  PRO: "Pro",
  CATALOGO: "Catálogo",
};

const NOMBRE_METODO_PAGO: Record<TenantSubscription["paymentMethod"], string> = {
  MANUAL: "Transferencia manual",
  FLOW_AUTOMATICO: "Flow (automático)",
};

const NOMBRE_ESTADO: Record<TenantSubscription["status"], string> = {
  PENDIENTE_TARJETA: "pendiente de tarjeta",
  ACTIVA: "al día",
  MOROSA: "morosa",
  CANCELADA: "cancelada",
};

function formatFecha(fecha: string): string {
  return new Date(fecha + "T00:00:00").toLocaleDateString("es-CL");
}

function tiempoSuscrito(desde: string): string {
  const dias = Math.floor((Date.now() - new Date(desde).getTime()) / (1000 * 60 * 60 * 24));
  if (dias < 1) return "hoy";
  if (dias < 31) return `hace ${dias} día${dias === 1 ? "" : "s"}`;
  const meses = Math.floor(dias / 30);
  return `hace ${meses} mes${meses === 1 ? "" : "es"}`;
}

export default function TenantCard({
  tenant,
  eliminando,
  onEliminar,
}: {
  tenant: Tenant;
  eliminando: boolean;
  onEliminar: (tenant: Tenant) => void;
}) {
  const esAdmin = esAdminSegunToken();
  const [suscripcion, setSuscripcion] = useState<TenantSubscription | null>(null);
  const [cargandoSuscripcion, setCargandoSuscripcion] = useState(true);
  const [editandoPago, setEditandoPago] = useState(false);
  const [nuevaFecha, setNuevaFecha] = useState("");
  const [guardandoPago, setGuardandoPago] = useState(false);
  const [errorPago, setErrorPago] = useState<string | null>(null);

  useEffect(() => {
    obtenerSuscripcion(tenant.id)
      .then(setSuscripcion)
      .catch(() => setSuscripcion(null))
      .finally(() => setCargandoSuscripcion(false));
  }, [tenant.id]);

  async function onGuardarPagoManual(e: FormEvent) {
    e.preventDefault();
    if (!nuevaFecha) return;
    setGuardandoPago(true);
    setErrorPago(null);
    try {
      const actualizada = await registrarPagoManual(tenant.id, nuevaFecha);
      setSuscripcion(actualizada);
      setEditandoPago(false);
      setNuevaFecha("");
    } catch (err) {
      setErrorPago(err instanceof ApiError ? err.message : "No se pudo guardar el pago.");
    } finally {
      setGuardandoPago(false);
    }
  }

  return (
    <div className="flex flex-col justify-between rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <div>
        <div className="flex items-start justify-between gap-2">
          <h2 className="font-semibold">{tenant.businessName}</h2>
          <span className="shrink-0 rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
            {NOMBRE_PLAN[tenant.plan]}
          </span>
        </div>
        <p className="mt-1 text-xs text-gray-500">{tenant.whatsappNumber}</p>
        <p className="mt-2 line-clamp-2 text-sm text-gray-600">{tenant.businessContext}</p>

        <div className="mt-3 rounded-md bg-gray-50 p-2 text-xs text-gray-600">
          {cargandoSuscripcion && <p>Cargando suscripción...</p>}
          {!cargandoSuscripcion && !suscripcion && <p>Sin pago registrado todavía.</p>}
          {!cargandoSuscripcion && suscripcion && (
            <p>
              {NOMBRE_METODO_PAGO[suscripcion.paymentMethod]}
              {" · "}
              {suscripcion.paidUntil
                ? `vigente hasta ${formatFecha(suscripcion.paidUntil)}`
                : "sin vigencia registrada"}
              {suscripcion.status !== "ACTIVA" && (
                <span className="font-medium text-red-600"> · {NOMBRE_ESTADO[suscripcion.status]}</span>
              )}
              <br />
              Suscrito al plan {NOMBRE_PLAN[tenant.plan]} desde {tiempoSuscrito(suscripcion.createdAt)}
            </p>
          )}
        </div>

        {!editandoPago && (
          <button
            onClick={() => setEditandoPago(true)}
            className="mt-2 text-xs text-blue-600 hover:underline"
          >
            Registrar pago manual
          </button>
        )}
        {editandoPago && (
          <form onSubmit={onGuardarPagoManual} className="mt-2 flex items-center gap-2">
            <input
              type="date"
              value={nuevaFecha}
              onChange={(e) => setNuevaFecha(e.target.value)}
              required
              className="rounded-md border border-gray-300 px-2 py-1 text-xs"
            />
            <button
              type="submit"
              disabled={guardandoPago}
              className="rounded-md bg-blue-600 px-2 py-1 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {guardandoPago ? "Guardando..." : "Guardar"}
            </button>
            <button
              type="button"
              onClick={() => setEditandoPago(false)}
              className="text-xs text-gray-500 hover:underline"
            >
              Cancelar
            </button>
          </form>
        )}
        {errorPago && <p className="mt-1 text-xs text-red-600">{errorPago}</p>}
      </div>

      <div className="mt-3 flex flex-wrap items-center justify-between gap-x-3 gap-y-1 text-sm">
        <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs">
          <Link href={`/tenants/${tenant.id}/edit`} className="text-blue-600 hover:underline">
            Contexto
          </Link>
          <Link href={`/tenants/${tenant.id}/agendamiento`} className="text-blue-600 hover:underline">
            Agendamiento
          </Link>
          <Link href={`/tenants/${tenant.id}/catalogo`} className="text-blue-600 hover:underline">
            Catálogo
          </Link>
          <Link href={`/tenants/${tenant.id}/pagos`} className="text-blue-600 hover:underline">
            Pagos
          </Link>
        </div>
        {esAdmin && (
          <button
            onClick={() => onEliminar(tenant)}
            disabled={eliminando}
            className="text-red-600 hover:underline disabled:opacity-50"
          >
            {eliminando ? "Eliminando..." : "Eliminar"}
          </button>
        )}
      </div>
    </div>
  );
}
