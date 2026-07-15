"use client";

import { useEffect, useState } from "react";
import { obtenerReporteAgendamiento, ReporteAgendamiento } from "@/lib/api";

function toISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export default function ReportesAgendamiento({ tenantId }: { tenantId: number }) {
  const hoy = new Date();
  const hace12Semanas = new Date(hoy);
  hace12Semanas.setDate(hace12Semanas.getDate() - 84);

  const [desde, setDesde] = useState(toISO(hace12Semanas));
  const [hasta, setHasta] = useState(toISO(hoy));
  const [reporte, setReporte] = useState<ReporteAgendamiento | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [abierto, setAbierto] = useState(false);

  function cargar(f: { desde?: string; hasta?: string } = {}) {
    const rangoDesde = f.desde !== undefined ? f.desde : desde;
    const rangoHasta = f.hasta !== undefined ? f.hasta : hasta;
    obtenerReporteAgendamiento(tenantId, { desde: rangoDesde, hasta: rangoHasta })
      .then(setReporte)
      .catch(() => setError("No se pudo cargar el reporte."));
  }

  useEffect(() => {
    if (abierto && !reporte) cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [abierto]);

  const maxSemana = Math.max(1, ...(reporte?.citasPorSemana.map((s) => s.cantidad) ?? [1]));
  const maxHora = Math.max(1, ...(reporte?.horasPeak.map((h) => h.cantidad) ?? [1]));

  return (
    <section className="mt-8">
      <button
        onClick={() => setAbierto((v) => !v)}
        className="mb-3 text-sm font-medium text-gray-700 hover:text-gray-900"
      >
        {abierto ? "▾" : "▸"} Reportes
      </button>

      {abierto && (
        <div className="rounded-md border border-gray-200 bg-white p-4">
          {error && <p className="mb-3 text-sm text-red-600">{error}</p>}

          <div className="mb-4 flex flex-wrap items-end gap-3">
            <div>
              <label className="block text-xs text-gray-500">Desde</label>
              <input
                type="date"
                value={desde}
                onChange={(e) => {
                  setDesde(e.target.value);
                  cargar({ desde: e.target.value });
                }}
                className="rounded-md border border-gray-300 px-2 py-1 text-xs"
              />
            </div>
            <div>
              <label className="block text-xs text-gray-500">Hasta</label>
              <input
                type="date"
                value={hasta}
                onChange={(e) => {
                  setHasta(e.target.value);
                  cargar({ hasta: e.target.value });
                }}
                className="rounded-md border border-gray-300 px-2 py-1 text-xs"
              />
            </div>
          </div>

          {!reporte ? (
            <p className="text-sm text-gray-500">Cargando...</p>
          ) : (
            <div className="space-y-6">
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <Metrica label="Citas totales" valor={reporte.totalCitas} />
                <Metrica
                  label="No-shows evitados"
                  valor={reporte.noShowsEvitados}
                  nota="con recordatorio y sí se cumplieron"
                />
                <Metrica label="No-shows" valor={reporte.noShows} />
                <Metrica label="Completadas" valor={reporte.citasPorEstado.COMPLETADA ?? 0} />
              </div>

              <div>
                <h3 className="mb-2 text-xs font-medium text-gray-500">Citas por semana</h3>
                {reporte.citasPorSemana.length === 0 ? (
                  <p className="text-sm text-gray-500">Sin datos en este rango.</p>
                ) : (
                  <div className="space-y-1">
                    {reporte.citasPorSemana.map((s) => (
                      <div key={s.inicioSemana} className="flex items-center gap-2 text-xs">
                        <span className="w-20 shrink-0 text-gray-500">{s.inicioSemana}</span>
                        <div className="h-3 flex-1 rounded bg-gray-100">
                          <div
                            className="h-3 rounded bg-blue-500"
                            style={{ width: `${(s.cantidad / maxSemana) * 100}%` }}
                          />
                        </div>
                        <span className="w-6 text-right text-gray-600">{s.cantidad}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div>
                <h3 className="mb-2 text-xs font-medium text-gray-500">Horas peak</h3>
                {reporte.horasPeak.length === 0 ? (
                  <p className="text-sm text-gray-500">Sin datos en este rango.</p>
                ) : (
                  <div className="space-y-1">
                    {reporte.horasPeak.slice(0, 6).map((h) => (
                      <div key={h.hora} className="flex items-center gap-2 text-xs">
                        <span className="w-14 shrink-0 text-gray-500">
                          {String(h.hora).padStart(2, "0")}:00
                        </span>
                        <div className="h-3 flex-1 rounded bg-gray-100">
                          <div
                            className="h-3 rounded bg-emerald-500"
                            style={{ width: `${(h.cantidad / maxHora) * 100}%` }}
                          />
                        </div>
                        <span className="w-6 text-right text-gray-600">{h.cantidad}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </section>
  );
}

function Metrica({ label, valor, nota }: { label: string; valor: number; nota?: string }) {
  return (
    <div className="rounded-md border border-gray-200 p-3">
      <div className="text-2xl font-semibold text-gray-900">{valor}</div>
      <div className="text-xs text-gray-500">{label}</div>
      {nota && <div className="mt-1 text-[10px] text-gray-400">{nota}</div>}
    </div>
  );
}
