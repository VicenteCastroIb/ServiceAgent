"use client";

import { FormEvent, useState } from "react";
import { actualizarCita, ApiError, Appointment } from "@/lib/api";

const NOMBRE_ESTADO: Record<Appointment["status"], string> = {
  CONFIRMADA: "Confirmada",
  CANCELADA: "Cancelada",
  REAGENDADA: "Reagendada",
  COMPLETADA: "Completada",
  NO_SHOW: "No se presentó",
};

function partesFecha(iso: string): { fecha: string; hora: string } {
  const [fecha, horaCompleta] = iso.split("T");
  return { fecha, hora: (horaCompleta ?? "00:00:00").slice(0, 5) };
}

export default function CitaDetailPanel({
  cita,
  onClose,
  onActualizada,
}: {
  cita: Appointment;
  onClose: () => void;
  onActualizada: (cita: Appointment) => void;
}) {
  const [accionando, setAccionando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const inicial = partesFecha(cita.startTime);
  const [reagendarFecha, setReagendarFecha] = useState(inicial.fecha);
  const [reagendarHora, setReagendarHora] = useState(inicial.hora);
  const [reagendando, setReagendando] = useState(false);

  async function onCambiarEstado(status: Appointment["status"]) {
    setAccionando(true);
    setError(null);
    try {
      const actualizada = await actualizarCita(cita.id, { status });
      onActualizada(actualizada);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo actualizar la cita.");
    } finally {
      setAccionando(false);
    }
  }

  async function onReagendar(e: FormEvent) {
    e.preventDefault();
    setReagendando(true);
    setError(null);
    try {
      const actualizada = await actualizarCita(cita.id, {
        startTime: `${reagendarFecha}T${reagendarHora}:00`,
      });
      onActualizada(actualizada);
    } catch (err) {
      setError(
        err instanceof ApiError
          ? err.message
          : "No se pudo reagendar (¿ese profesional ya tiene otra cita a esa hora?)."
      );
    } finally {
      setReagendando(false);
    }
  }

  return (
    <div className="fixed inset-0 z-20 flex justify-end bg-black/30" onClick={onClose}>
      <div
        className="h-full w-full max-w-sm overflow-y-auto bg-white p-5 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between">
          <h2 className="text-lg font-semibold">Cita #{cita.id}</h2>
          <button onClick={onClose} className="text-sm text-gray-400 hover:text-gray-600">
            Cerrar
          </button>
        </div>

        <dl className="space-y-2 text-sm text-gray-700">
          <div>
            <dt className="text-xs text-gray-400">Profesional</dt>
            <dd>{cita.professional?.name ?? "—"}</dd>
          </div>
          <div>
            <dt className="text-xs text-gray-400">Servicio</dt>
            <dd>{cita.service}</dd>
          </div>
          <div>
            <dt className="text-xs text-gray-400">Cliente</dt>
            <dd>{cita.clientPhoneNumber}</dd>
          </div>
          <div>
            <dt className="text-xs text-gray-400">Horario</dt>
            <dd>{cita.startTime.replace("T", " ").slice(0, 16)}</dd>
          </div>
          <div>
            <dt className="text-xs text-gray-400">Estado</dt>
            <dd className="font-medium">{NOMBRE_ESTADO[cita.status]}</dd>
          </div>
        </dl>

        <div className="mt-5 flex flex-wrap gap-2">
          <button
            onClick={() => onCambiarEstado("COMPLETADA")}
            disabled={accionando}
            className="rounded-md bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
          >
            Marcar completada
          </button>
          <button
            onClick={() => onCambiarEstado("NO_SHOW")}
            disabled={accionando}
            className="rounded-md bg-amber-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-amber-700 disabled:opacity-50"
          >
            No se presentó
          </button>
          <button
            onClick={() => onCambiarEstado("CANCELADA")}
            disabled={accionando}
            className="rounded-md bg-red-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
          >
            Cancelar cita
          </button>
        </div>

        <div className="mt-6 border-t border-gray-100 pt-4">
          <p className="mb-2 text-xs font-medium text-gray-500">Reagendar</p>
          <form onSubmit={onReagendar} className="flex flex-wrap items-end gap-2">
            <input
              type="date"
              value={reagendarFecha}
              onChange={(e) => setReagendarFecha(e.target.value)}
              className="rounded-md border border-gray-300 px-2 py-1 text-xs"
            />
            <input
              type="time"
              value={reagendarHora}
              onChange={(e) => setReagendarHora(e.target.value)}
              className="rounded-md border border-gray-300 px-2 py-1 text-xs"
            />
            <button
              type="submit"
              disabled={reagendando}
              className="rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {reagendando ? "Guardando..." : "Guardar nuevo horario"}
            </button>
          </form>
        </div>

        {error && <p className="mt-3 text-xs text-red-600">{error}</p>}
      </div>
    </div>
  );
}
