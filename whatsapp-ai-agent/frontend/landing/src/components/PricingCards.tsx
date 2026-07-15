"use client";

import { useState } from "react";
import Link from "next/link";
import type { Accent } from "@/lib/accents";

export interface PlanPrecio {
  nombre: string;
  precioMensual: number | null;
  precioAnual: number | null;
  descripcion: string;
  features: string[];
  destacado?: boolean;
  ctaLabel: string;
  ctaHref: string;
}

interface PricingCardsProps {
  planes: PlanPrecio[];
  accent: Accent;
  mostrarToggle?: boolean;
}

export default function PricingCards({ planes, accent, mostrarToggle = true }: PricingCardsProps) {
  const [periodo, setPeriodo] = useState<"mensual" | "anual">("anual");

  return (
    <div>
      {mostrarToggle && (
        <div className="mb-10 flex justify-center">
          <div className="inline-flex items-center rounded-full bg-slate-100 p-1 text-sm font-medium">
            <button
              onClick={() => setPeriodo("mensual")}
              className={`rounded-full px-4 py-2 transition ${
                periodo === "mensual" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
              }`}
            >
              Mensual
            </button>
            <button
              onClick={() => setPeriodo("anual")}
              className={`flex items-center gap-2 rounded-full px-4 py-2 transition ${
                periodo === "anual" ? "text-white shadow-sm" : "text-slate-500"
              }`}
              style={periodo === "anual" ? { backgroundImage: accent.gradient } : undefined}
            >
              Anual
              <span className="rounded-full bg-white/20 px-1.5 py-0.5 text-[10px]">2 meses gratis</span>
            </button>
          </div>
        </div>
      )}

      <div className={`grid gap-6 ${planes.length >= 3 ? "lg:grid-cols-3" : "sm:grid-cols-2"}`}>
        {planes.map((plan) => {
          const precio = periodo === "anual" ? plan.precioAnual : plan.precioMensual;
          const ahorro =
            periodo === "anual" && plan.precioMensual != null && plan.precioAnual != null
              ? plan.precioMensual * 12 - plan.precioAnual
              : null;

          return (
            <div
              key={plan.nombre}
              className={`relative rounded-3xl border p-8 ${plan.destacado ? "border-2 shadow-xl" : "border-slate-200"}`}
              style={plan.destacado ? { borderColor: accent.from } : undefined}
            >
              {plan.destacado && (
                <span
                  className="absolute -top-3 left-1/2 -translate-x-1/2 rounded-full px-3 py-1 text-xs font-semibold whitespace-nowrap text-white"
                  style={{ backgroundImage: accent.gradient }}
                >
                  Más elegido
                </span>
              )}

              <p className="text-sm font-medium text-slate-500">{plan.nombre}</p>

              {precio != null ? (
                <>
                  <p className="mt-2 flex items-baseline gap-1">
                    <span className="font-heading text-4xl font-semibold text-slate-900">
                      ${formatearPrecio(precio)}
                    </span>
                    <span className="text-sm text-slate-400">/{periodo === "anual" ? "año" : "mes"}</span>
                  </p>
                  {ahorro != null && ahorro > 0 && (
                    <p className="mt-1 text-xs font-medium text-emerald-600">
                      Ahorras ${formatearPrecio(ahorro)} al año · 2 meses gratis 🎉
                    </p>
                  )}
                </>
              ) : (
                <p className="mt-2 font-heading text-3xl font-semibold text-slate-900">A tu medida</p>
              )}

              <p className="mt-3 text-sm text-slate-500">{plan.descripcion}</p>

              <ul className="mt-6 space-y-3">
                {plan.features.map((feature) => (
                  <li key={feature} className="flex items-start gap-2.5 text-sm text-slate-700">
                    <CheckIcon accent={accent} />
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>

              <Link
                href={plan.ctaHref}
                className={`mt-8 block rounded-[14px] px-6 py-3 text-center text-sm font-semibold transition ${
                  plan.destacado
                    ? "text-white hover:brightness-110"
                    : "border border-slate-200 text-slate-900 hover:border-slate-300"
                }`}
                style={plan.destacado ? { backgroundImage: accent.gradient } : undefined}
              >
                {plan.ctaLabel}
              </Link>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function formatearPrecio(valor: number): string {
  return valor.toLocaleString("es-CL");
}

function CheckIcon({ accent }: { accent: Accent }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7" stroke={accent.from} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
