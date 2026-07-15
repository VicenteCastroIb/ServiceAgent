"use client";

import { FormEvent, useState } from "react";
import {
  ApiError,
  Availability,
  crearDisponibilidad,
  DiaSemana,
  listarDisponibilidad,
  Professional,
} from "@/lib/api";

const DIAS: { value: DiaSemana; label: string }[] = [
  { value: "MONDAY", label: "Lunes" },
  { value: "TUESDAY", label: "Martes" },
  { value: "WEDNESDAY", label: "Miércoles" },
  { value: "THURSDAY", label: "Jueves" },
  { value: "FRIDAY", label: "Viernes" },
  { value: "SATURDAY", label: "Sábado" },
  { value: "SUNDAY", label: "Domingo" },
];

const NOMBRE_DIA: Record<DiaSemana, string> = Object.fromEntries(
  DIAS.map((d) => [d.value, d.label])
) as Record<DiaSemana, string>;

export default function ProfessionalAvailability({ professional }: { professional: Professional }) {
  const [abierto, setAbierto] = useState(false);
  const [disponibilidad, setDisponibilidad] = useState<Availability[] | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [dayOfWeek, setDayOfWeek] = useState<DiaSemana>("MONDAY");
  const [startTime, setStartTime] = useState("10:00");
  const [endTime, setEndTime] = useState("19:00");
  const [slotMinutes, setSlotMinutes] = useState(30);
  const [guardando, setGuardando] = useState(false);

  function cargar() {
    setCargando(true);
    listarDisponibilidad(professional.id)
      .then(setDisponibilidad)
      .catch(() => setError("No se pudo cargar la disponibilidad."))
      .finally(() => setCargando(false));
  }

  function onToggle() {
    const nuevoEstado = !abierto;
    setAbierto(nuevoEstado);
    if (nuevoEstado && disponibilidad === null) {
      cargar();
    }
  }

  async function onAgregar(e: FormEvent) {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    try {
      await crearDisponibilidad(professional.id, { dayOfWeek, startTime, endTime, slotMinutes });
      cargar();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo guardar la disponibilidad.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="rounded-md border border-gray-200 bg-white">
      <button
        onClick={onToggle}
        className="flex w-full items-center justify-between px-4 py-3 text-left text-sm font-medium"
      >
        <span>
          {professional.name}
          {!professional.active && <span className="ml-2 text-xs text-gray-400">(inactivo)</span>}
        </span>
        <span className="text-gray-400">{abierto ? "▲" : "▼"}</span>
      </button>

      {abierto && (
        <div className="border-t border-gray-100 px-4 py-3">
          {cargando && <p className="text-xs text-gray-500">Cargando...</p>}

          {!cargando && disponibilidad && disponibilidad.length === 0 && (
            <p className="text-xs text-gray-500">Sin disponibilidad cargada todavía.</p>
          )}

          {!cargando && disponibilidad && disponibilidad.length > 0 && (
            <ul className="mb-3 space-y-1 text-xs text-gray-600">
              {disponibilidad.map((d) => (
                <li key={d.id}>
                  {NOMBRE_DIA[d.dayOfWeek]}: {d.startTime.slice(0, 5)} a {d.endTime.slice(0, 5)} (turnos de{" "}
                  {d.slotMinutes} min)
                </li>
              ))}
            </ul>
          )}

          <form onSubmit={onAgregar} className="flex flex-wrap items-end gap-2">
            <div>
              <label className="block text-xs text-gray-500">Día</label>
              <select
                value={dayOfWeek}
                onChange={(e) => setDayOfWeek(e.target.value as DiaSemana)}
                className="rounded-md border border-gray-300 px-2 py-1 text-xs"
              >
                {DIAS.map((d) => (
                  <option key={d.value} value={d.value}>
                    {d.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-500">Desde</label>
              <input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="rounded-md border border-gray-300 px-2 py-1 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-500">Hasta</label>
              <input
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="rounded-md border border-gray-300 px-2 py-1 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-500">Turnos (min)</label>
              <input
                type="number"
                min={5}
                step={5}
                value={slotMinutes}
                onChange={(e) => setSlotMinutes(Number(e.target.value))}
                className="w-16 rounded-md border border-gray-300 px-2 py-1 text-xs"
              />
            </div>
            <button
              type="submit"
              disabled={guardando}
              className="rounded-md bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {guardando ? "Agregando..." : "+ Agregar"}
            </button>
          </form>

          {error && <p className="mt-2 text-xs text-red-600">{error}</p>}
        </div>
      )}
    </div>
  );
}
