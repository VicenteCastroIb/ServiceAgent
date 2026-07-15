"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";
import {
  ApiError,
  Appointment,
  buscarTenant,
  crearProfesional,
  listarCitas,
  listarProfesionales,
  Professional,
  Tenant,
} from "@/lib/api";
import TenantSubNav from "@/components/TenantSubNav";
import ProfessionalAvailability from "@/components/ProfessionalAvailability";
import AppointmentsCalendar from "@/components/AppointmentsCalendar";
import CitaDetailPanel from "@/components/CitaDetailPanel";
import ReportesAgendamiento from "@/components/ReportesAgendamiento";

const NOMBRE_ESTADO_CITA: Record<Appointment["status"], string> = {
  CONFIRMADA: "Confirmada",
  CANCELADA: "Cancelada",
  REAGENDADA: "Reagendada",
  COMPLETADA: "Completada",
  NO_SHOW: "No se presentó",
};

function toISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function inicioSemana(d: Date): Date {
  const date = new Date(d);
  date.setHours(0, 0, 0, 0);
  const dia = date.getDay(); // 0 = domingo
  const diff = dia === 0 ? -6 : 1 - dia;
  date.setDate(date.getDate() + diff);
  return date;
}

function addDias(iso: string, n: number): string {
  const d = new Date(iso + "T00:00:00");
  d.setDate(d.getDate() + n);
  return toISO(d);
}

function diasEntre(desde: string, hasta: string): string[] {
  const dias: string[] = [];
  let actual = desde;
  let guard = 0;
  while (actual <= hasta && guard < 62) {
    dias.push(actual);
    actual = addDias(actual, 1);
    guard += 1;
  }
  return dias;
}

export default function AgendamientoPage() {
  const listo = useRequireAuth();
  const params = useParams<{ id: string }>();
  const tenantId = Number(params.id);

  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [profesionales, setProfesionales] = useState<Professional[] | null>(null);
  const [citas, setCitas] = useState<Appointment[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [nombreProfesional, setNombreProfesional] = useState("");
  const [guardando, setGuardando] = useState(false);

  const semanaActual = inicioSemana(new Date());
  const [vista, setVista] = useState<"lista" | "calendario">("calendario");
  const [filtroProfesionalId, setFiltroProfesionalId] = useState<number | "">("");
  const [desde, setDesde] = useState(toISO(semanaActual));
  const [hasta, setHasta] = useState(addDias(toISO(semanaActual), 6));

  const [citaSeleccionada, setCitaSeleccionada] = useState<Appointment | null>(null);

  function cargarCitas(f: { profesionalId?: number | ""; desde?: string; hasta?: string } = {}) {
    const profesionalId = f.profesionalId !== undefined ? f.profesionalId : filtroProfesionalId;
    const rangoDesde = f.desde !== undefined ? f.desde : desde;
    const rangoHasta = f.hasta !== undefined ? f.hasta : hasta;
    listarCitas(tenantId, {
      professionalId: profesionalId === "" ? undefined : profesionalId,
      desde: rangoDesde || undefined,
      hasta: rangoHasta || undefined,
    })
      .then(setCitas)
      .catch(() => setError("No se pudieron cargar las citas."));
  }

  function cargar() {
    buscarTenant(tenantId).then(setTenant).catch(() => setError("No se pudo cargar el negocio."));
    listarProfesionales(tenantId)
      .then(setProfesionales)
      .catch(() => setError("No se pudieron cargar los profesionales."));
    cargarCitas();
  }

  useEffect(() => {
    if (listo) cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [listo, tenantId]);

  if (!listo) return null;

  async function onCrearProfesional(e: FormEvent) {
    e.preventDefault();
    if (!nombreProfesional.trim()) return;
    setGuardando(true);
    setError(null);
    try {
      await crearProfesional(tenantId, nombreProfesional.trim());
      setNombreProfesional("");
      listarProfesionales(tenantId).then(setProfesionales);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo crear el profesional.");
    } finally {
      setGuardando(false);
    }
  }

  function onCambiarProfesionalFiltro(valor: string) {
    const profesionalId = valor === "" ? "" : Number(valor);
    setFiltroProfesionalId(profesionalId);
    cargarCitas({ profesionalId });
  }

  function onCambiarDesde(valor: string) {
    setDesde(valor);
    cargarCitas({ desde: valor });
  }

  function onCambiarHasta(valor: string) {
    setHasta(valor);
    cargarCitas({ hasta: valor });
  }

  function irASemana(offsetDias: number) {
    const nuevaDesde = addDias(desde, offsetDias);
    const nuevaHasta = addDias(hasta, offsetDias);
    setDesde(nuevaDesde);
    setHasta(nuevaHasta);
    cargarCitas({ desde: nuevaDesde, hasta: nuevaHasta });
  }

  function irASemanaActual() {
    const nuevaDesde = toISO(inicioSemana(new Date()));
    const nuevaHasta = addDias(nuevaDesde, 6);
    setDesde(nuevaDesde);
    setHasta(nuevaHasta);
    cargarCitas({ desde: nuevaDesde, hasta: nuevaHasta });
  }

  function onCitaActualizada(actualizada: Appointment) {
    setCitas((prev) => (prev ? prev.map((c) => (c.id === actualizada.id ? actualizada : c)) : prev));
    setCitaSeleccionada(actualizada);
  }

  const citasOrdenadas = citas?.slice().sort((a, b) => a.startTime.localeCompare(b.startTime)) ?? [];
  const dias = diasEntre(desde, hasta);

  return (
    <div className="max-w-5xl">
      <TenantSubNav tenantId={tenantId} />

      <h1 className="text-xl font-semibold">{tenant?.businessName ?? "Agendamiento"}</h1>
      {tenant && tenant.plan === "BASICO" && (
        <p className="mt-1 text-sm text-amber-600">
          Este negocio está en plan Básico: el bot todavía no va a usar agendamiento aunque cargues
          profesionales y disponibilidad acá. Subilo a Pro o Catálogo para activarlo.
        </p>
      )}

      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

      <section className="mt-6">
        <h2 className="mb-2 text-sm font-medium text-gray-700">Profesionales / boxes</h2>

        <form onSubmit={onCrearProfesional} className="mb-3 flex gap-2">
          <input
            value={nombreProfesional}
            onChange={(e) => setNombreProfesional(e.target.value)}
            placeholder="Nombre del profesional o box"
            className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={guardando}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {guardando ? "Creando..." : "+ Agregar"}
          </button>
        </form>

        {profesionales?.length === 0 && (
          <p className="text-sm text-gray-500">Todavía no hay profesionales cargados.</p>
        )}

        <div className="space-y-2">
          {profesionales?.map((p) => (
            <ProfessionalAvailability key={p.id} professional={p} />
          ))}
        </div>
      </section>

      <section className="mt-8">
        <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
          <h2 className="text-sm font-medium text-gray-700">Citas</h2>
          <div className="flex rounded-md border border-gray-300 text-xs">
            <button
              onClick={() => setVista("lista")}
              className={`px-3 py-1.5 ${vista === "lista" ? "bg-gray-800 text-white" : "text-gray-600"}`}
            >
              Lista
            </button>
            <button
              onClick={() => setVista("calendario")}
              className={`px-3 py-1.5 ${vista === "calendario" ? "bg-gray-800 text-white" : "text-gray-600"}`}
            >
              Calendario
            </button>
          </div>
        </div>

        <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-gray-200 bg-gray-50 p-3">
          <div>
            <label className="block text-xs text-gray-500">Profesional</label>
            <select
              value={filtroProfesionalId}
              onChange={(e) => onCambiarProfesionalFiltro(e.target.value)}
              className="rounded-md border border-gray-300 px-2 py-1 text-xs"
            >
              <option value="">Todos</option>
              {profesionales?.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs text-gray-500">Desde</label>
            <input
              type="date"
              value={desde}
              onChange={(e) => onCambiarDesde(e.target.value)}
              className="rounded-md border border-gray-300 px-2 py-1 text-xs"
            />
          </div>
          <div>
            <label className="block text-xs text-gray-500">Hasta</label>
            <input
              type="date"
              value={hasta}
              onChange={(e) => onCambiarHasta(e.target.value)}
              className="rounded-md border border-gray-300 px-2 py-1 text-xs"
            />
          </div>
          {vista === "calendario" && (
            <div className="flex gap-1">
              <button
                onClick={() => irASemana(-7)}
                className="rounded-md border border-gray-300 px-2 py-1 text-xs text-gray-600 hover:bg-white"
              >
                ← Semana
              </button>
              <button
                onClick={irASemanaActual}
                className="rounded-md border border-gray-300 px-2 py-1 text-xs text-gray-600 hover:bg-white"
              >
                Hoy
              </button>
              <button
                onClick={() => irASemana(7)}
                className="rounded-md border border-gray-300 px-2 py-1 text-xs text-gray-600 hover:bg-white"
              >
                Semana →
              </button>
            </div>
          )}
        </div>

        {vista === "calendario" ? (
          <AppointmentsCalendar dias={dias} citas={citas ?? []} onSelect={setCitaSeleccionada} />
        ) : (
          <div className="overflow-hidden rounded-md border border-gray-200 bg-white">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500">
                <tr>
                  <th className="px-4 py-2 font-medium">Fecha y hora</th>
                  <th className="px-4 py-2 font-medium">Profesional</th>
                  <th className="px-4 py-2 font-medium">Servicio</th>
                  <th className="px-4 py-2 font-medium">Cliente</th>
                  <th className="px-4 py-2 font-medium">Estado</th>
                </tr>
              </thead>
              <tbody>
                {citasOrdenadas.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-4 py-4 text-gray-500">
                      Todavía no hay citas en este rango.
                    </td>
                  </tr>
                )}
                {citasOrdenadas.map((c) => (
                  <tr
                    key={c.id}
                    onClick={() => setCitaSeleccionada(c)}
                    className="cursor-pointer border-t border-gray-100 hover:bg-gray-50"
                  >
                    <td className="px-4 py-2">{c.startTime.replace("T", " ").slice(0, 16)}</td>
                    <td className="px-4 py-2">{c.professional?.name}</td>
                    <td className="px-4 py-2 text-gray-500">{c.service}</td>
                    <td className="px-4 py-2 text-gray-500">{c.clientPhoneNumber}</td>
                    <td className="px-4 py-2 text-gray-500">{NOMBRE_ESTADO_CITA[c.status]}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <ReportesAgendamiento tenantId={tenantId} />

      {citaSeleccionada && (
        <CitaDetailPanel
          cita={citaSeleccionada}
          onClose={() => setCitaSeleccionada(null)}
          onActualizada={onCitaActualizada}
        />
      )}
    </div>
  );
}
