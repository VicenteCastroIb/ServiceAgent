"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { useAuth } from "@/lib/auth-context";
import {
  ApiError,
  buscarTenant,
  fijarCredencialesFlow,
  iniciarSuscripcion,
  listarOrdenesPago,
  marcarSuscripcionMorosa,
  obtenerSuscripcion,
  PaymentOrder,
  registrarPagoManual,
  Tenant,
  TenantSubscription,
} from "@/lib/api";
import TenantSubNav from "@/components/TenantSubNav";

const NOMBRE_ESTADO_ORDEN: Record<PaymentOrder["status"], string> = {
  PENDIENTE: "Pendiente",
  PAGADA: "Pagada",
  RECHAZADA: "Rechazada",
  ANULADA: "Anulada",
};

const NOMBRE_METODO_PAGO: Record<TenantSubscription["paymentMethod"], string> = {
  MANUAL: "Transferencia manual",
  FLOW_AUTOMATICO: "Flow (automático)",
};

const NOMBRE_ESTADO_SUSCRIPCION: Record<TenantSubscription["status"], string> = {
  PENDIENTE_TARJETA: "Pendiente de tarjeta",
  ACTIVA: "Al día",
  MOROSA: "Morosa",
  CANCELADA: "Cancelada",
};

export default function PagosPage() {
  const listo = useRequireAuth();
  const params = useParams<{ id: string }>();
  const tenantId = Number(params.id);
  const { esAdmin } = useAuth();

  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [ordenes, setOrdenes] = useState<PaymentOrder[] | null>(null);
  const [suscripcion, setSuscripcion] = useState<TenantSubscription | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [apiKey, setApiKey] = useState("");
  const [secretKey, setSecretKey] = useState("");
  const [guardandoFlow, setGuardandoFlow] = useState(false);
  const [mensajeFlow, setMensajeFlow] = useState<string | null>(null);

  const [emailSuscripcion, setEmailSuscripcion] = useState("");
  const [iniciando, setIniciando] = useState(false);
  const [urlTarjeta, setUrlTarjeta] = useState<string | null>(null);

  const [fechaPagoManual, setFechaPagoManual] = useState("");
  const [guardandoPagoManual, setGuardandoPagoManual] = useState(false);

  function cargar() {
    buscarTenant(tenantId).then(setTenant).catch(() => setError("No se pudo cargar el negocio."));
    listarOrdenesPago(tenantId)
      .then(setOrdenes)
      .catch(() => setError("No se pudieron cargar las órdenes de pago."));
    obtenerSuscripcion(tenantId).then(setSuscripcion);
  }

  useEffect(() => {
    if (listo) cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [listo, tenantId]);

  if (!listo) return null;

  async function onGuardarFlow(e: FormEvent) {
    e.preventDefault();
    setGuardandoFlow(true);
    setError(null);
    setMensajeFlow(null);
    try {
      const actualizado = await fijarCredencialesFlow(tenantId, { apiKey, secretKey });
      setTenant(actualizado);
      setApiKey("");
      setSecretKey("");
      setMensajeFlow("Credenciales guardadas.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudieron guardar las credenciales.");
    } finally {
      setGuardandoFlow(false);
    }
  }

  async function onIniciarSuscripcion(e: FormEvent) {
    e.preventDefault();
    setIniciando(true);
    setError(null);
    setUrlTarjeta(null);
    try {
      const { urlRegistroTarjeta } = await iniciarSuscripcion(tenantId, emailSuscripcion);
      setUrlTarjeta(urlRegistroTarjeta);
      obtenerSuscripcion(tenantId).then(setSuscripcion);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? "No se pudo iniciar la suscripción (¿ya cargaste tu propia cuenta Flow para cobrar?)."
          : "No se pudo iniciar la suscripción."
      );
    } finally {
      setIniciando(false);
    }
  }

  async function onMarcarMorosa() {
    await marcarSuscripcionMorosa(tenantId);
    obtenerSuscripcion(tenantId).then(setSuscripcion);
  }

  async function onGuardarPagoManual(e: FormEvent) {
    e.preventDefault();
    if (!fechaPagoManual) return;
    setGuardandoPagoManual(true);
    try {
      const actualizada = await registrarPagoManual(tenantId, fechaPagoManual);
      setSuscripcion(actualizada);
      setFechaPagoManual("");
    } catch {
      setError("No se pudo registrar el pago manual.");
    } finally {
      setGuardandoPagoManual(false);
    }
  }

  return (
    <div className="max-w-3xl">
      <TenantSubNav tenantId={tenantId} />

      <h1 className="text-xl font-semibold">{tenant?.businessName ?? "Pagos"}</h1>

      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

      <section className="mt-6 rounded-md border border-gray-200 bg-white p-4">
        <h2 className="mb-1 text-sm font-medium text-gray-700">
          Suscripción de este negocio a la plataforma
        </h2>
        <p className="mb-3 text-xs text-gray-500">Lo que este negocio te paga a vos cada mes.</p>

        {suscripcion ? (
          <div className="mb-3 text-sm text-gray-600">
            <p>
              {NOMBRE_METODO_PAGO[suscripcion.paymentMethod]}
              {" · "}
              {suscripcion.paidUntil ? `vigente hasta ${suscripcion.paidUntil}` : "sin vigencia registrada"}
              {" · "}
              <span className={suscripcion.status === "ACTIVA" ? "text-green-600" : "font-medium text-red-600"}>
                {NOMBRE_ESTADO_SUSCRIPCION[suscripcion.status]}
              </span>
            </p>
          </div>
        ) : (
          <p className="mb-3 text-sm text-gray-500">Sin pago registrado todavía.</p>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <p className="mb-1 text-xs font-medium text-gray-500">Cobro automático por Flow</p>
            <form onSubmit={onIniciarSuscripcion} className="flex gap-2">
              <input
                type="email"
                value={emailSuscripcion}
                onChange={(e) => setEmailSuscripcion(e.target.value)}
                placeholder="Email del dueño"
                required
                className="flex-1 rounded-md border border-gray-300 px-2 py-1 text-xs"
              />
              <button
                type="submit"
                disabled={iniciando}
                className="rounded-md bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {iniciando ? "..." : "Iniciar"}
              </button>
            </form>
            {urlTarjeta && (
              <p className="mt-2 text-xs text-gray-600">
                Mandale este link al dueño para que registre su tarjeta:{" "}
                <a href={urlTarjeta} target="_blank" rel="noreferrer" className="text-blue-600 underline">
                  {urlTarjeta}
                </a>
              </p>
            )}
          </div>

          <div>
            <p className="mb-1 text-xs font-medium text-gray-500">Pago manual (transferencia)</p>
            <form onSubmit={onGuardarPagoManual} className="flex gap-2">
              <input
                type="date"
                value={fechaPagoManual}
                onChange={(e) => setFechaPagoManual(e.target.value)}
                required
                className="rounded-md border border-gray-300 px-2 py-1 text-xs"
              />
              <button
                type="submit"
                disabled={guardandoPagoManual}
                className="rounded-md bg-gray-800 px-3 py-1 text-xs font-medium text-white hover:bg-gray-900 disabled:opacity-50"
              >
                {guardandoPagoManual ? "..." : "Registrar"}
              </button>
            </form>
          </div>
        </div>

        {esAdmin && suscripcion && suscripcion.status === "ACTIVA" && (
          <button onClick={onMarcarMorosa} className="mt-3 text-xs text-red-600 hover:underline">
            Marcar como morosa
          </button>
        )}
      </section>

      <section className="mt-6 rounded-md border border-gray-200 bg-white p-4">
        <h2 className="mb-1 text-sm font-medium text-gray-700">Pasarela de pago de este negocio</h2>
        <p className="mb-3 text-xs text-gray-500">
          La cuenta Flow propia de este negocio, para que SUS clientes le paguen por WhatsApp.{" "}
          {tenant?.flowConfigurado ? "Ya está configurada." : "Todavía no está configurada."}
          {!esAdmin && " Solo el admin puede cargarla o cambiarla."}
        </p>
        {esAdmin && (
          <form onSubmit={onGuardarFlow} className="space-y-2">
            <input
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="apiKey de Flow del negocio"
              required
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
            <input
              value={secretKey}
              onChange={(e) => setSecretKey(e.target.value)}
              placeholder="secretKey de Flow del negocio"
              required
              type="password"
              className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            />
            <button
              type="submit"
              disabled={guardandoFlow}
              className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {guardandoFlow ? "Guardando..." : "Guardar credenciales"}
            </button>
            {mensajeFlow && <p className="text-sm text-green-600">{mensajeFlow}</p>}
          </form>
        )}
      </section>

      <section className="mt-6">
        <h2 className="mb-2 text-sm font-medium text-gray-700">Órdenes de pago (clientes del negocio)</h2>
        <div className="overflow-hidden rounded-md border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-500">
              <tr>
                <th className="px-4 py-2 font-medium">Fecha</th>
                <th className="px-4 py-2 font-medium">Cliente</th>
                <th className="px-4 py-2 font-medium">Monto</th>
                <th className="px-4 py-2 font-medium">Estado</th>
              </tr>
            </thead>
            <tbody>
              {ordenes?.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-4 text-gray-500">
                    Todavía no hay órdenes de pago.
                  </td>
                </tr>
              )}
              {ordenes?.map((o) => (
                <tr key={o.id} className="border-t border-gray-100">
                  <td className="px-4 py-2 text-gray-500">{o.createdAt.replace("T", " ").slice(0, 16)}</td>
                  <td className="px-4 py-2 text-gray-500">{o.clientPhoneNumber}</td>
                  <td className="px-4 py-2">${o.amount.toLocaleString("es-CL")}</td>
                  <td className="px-4 py-2 text-gray-500">{NOMBRE_ESTADO_ORDEN[o.status]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
