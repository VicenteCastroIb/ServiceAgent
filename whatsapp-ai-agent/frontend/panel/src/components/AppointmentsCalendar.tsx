"use client";

import { Appointment } from "@/lib/api";

const COLORES = [
  "bg-blue-100 text-blue-800 border-blue-200",
  "bg-purple-100 text-purple-800 border-purple-200",
  "bg-teal-100 text-teal-800 border-teal-200",
  "bg-pink-100 text-pink-800 border-pink-200",
  "bg-amber-100 text-amber-800 border-amber-200",
  "bg-lime-100 text-lime-800 border-lime-200",
];

const ESTADO_APAGADO: Record<Appointment["status"], boolean> = {
  CONFIRMADA: false,
  REAGENDADA: false,
  COMPLETADA: true,
  NO_SHOW: true,
  CANCELADA: true,
};

function colorProfesional(id: number): string {
  return COLORES[id % COLORES.length];
}

export default function AppointmentsCalendar({
  dias,
  citas,
  onSelect,
}: {
  dias: string[];
  citas: Appointment[];
  onSelect: (cita: Appointment) => void;
}) {
  const hoy = new Date();
  const hoyISO = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}-${String(
    hoy.getDate()
  ).padStart(2, "0")}`;

  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-7">
      {dias.map((diaISO) => {
        const citasDelDia = citas
          .filter((c) => c.startTime.slice(0, 10) === diaISO)
          .sort((a, b) => a.startTime.localeCompare(b.startTime));
        const fecha = new Date(diaISO + "T00:00:00");
        const esHoy = diaISO === hoyISO;

        return (
          <div
            key={diaISO}
            className={`rounded-md border p-2 ${esHoy ? "border-blue-300 bg-blue-50/40" : "border-gray-200 bg-white"}`}
          >
            <p className="mb-2 text-center text-xs font-medium capitalize text-gray-500">
              {fecha.toLocaleDateString("es-CL", { weekday: "short", day: "numeric", month: "short" })}
            </p>
            <div className="space-y-1">
              {citasDelDia.length === 0 && (
                <p className="text-center text-[11px] text-gray-300">—</p>
              )}
              {citasDelDia.map((c) => (
                <button
                  key={c.id}
                  onClick={() => onSelect(c)}
                  className={`w-full rounded border px-1.5 py-1 text-left text-[11px] leading-tight ${colorProfesional(
                    c.professional.id
                  )} ${ESTADO_APAGADO[c.status] ? "opacity-40 line-through" : ""}`}
                >
                  <span className="block font-medium">{c.startTime.slice(11, 16)} · {c.professional.name}</span>
                  <span className="block truncate">{c.service}</span>
                </button>
              ))}
            </div>
          </div>
        );
      })}
    </div>
  );
}
