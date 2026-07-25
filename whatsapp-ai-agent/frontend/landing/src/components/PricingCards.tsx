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
  ctaExterna?: boolean;
  secondaryCtaLabel?: string;
  secondaryCtaHref?: string;
}

interface PricingCardsProps {
  planes: PlanPrecio[];
  accent: Accent;
  mostrarToggle?: boolean;
}

export default function PricingCards({ planes, accent, mostrarToggle = true }: PricingCardsProps) {
  const [periodo, setPeriodo] = useState<"mensual" | "anual">("mensual");

  return (
    <div>
      {mostrarToggle && (
        <div className="mb-9 flex justify-center">
          <div className="inline-flex items-center gap-1 rounded-full border border-ink/10 bg-card p-[5px] text-sm font-medium">
            <button
              onClick={() => setPeriodo("mensual")}
              className={`rounded-full px-[18px] py-2.5 text-[13.5px] font-semibold transition ${
                periodo === "mensual" ? "text-white" : "text-ink/55"
              }`}
              style={periodo === "mensual" ? { backgroundImage: "linear-gradient(90deg,#b9862f,#8a5f22)" } : undefined}
            >
              Mensual
            </button>
            <button
              onClick={() => setPeriodo("anual")}
              className={`flex items-center gap-1.5 rounded-full px-[18px] py-2.5 text-[13.5px] font-semibold transition ${
                periodo === "anual" ? "text-white" : "text-ink/55"
              }`}
              style={periodo === "anual" ? { backgroundImage: "linear-gradient(90deg,#b9862f,#8a5f22)" } : undefined}
            >
              Anual
              <span className="rounded-full bg-white/25 px-[7px] py-0.5 text-[10px]">2 meses gratis</span>
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
              className={`relative rounded-[22px] p-9 ${
                plan.destacado
                  ? "border-[1.5px] border-green/50 shadow-[0_25px_70px_rgba(185,134,47,0.14)]"
                  : "border border-ink/12 bg-card"
              }`}
              style={
                plan.destacado
                  ? { backgroundImage: "linear-gradient(180deg,rgba(185,134,47,0.10),rgba(201,120,143,0.06))" }
                  : undefined
              }
            >
              {plan.destacado && (
                <span
                  className="absolute -top-[13px] left-1/2 -translate-x-1/2 rounded-full px-3.5 py-1.5 text-xs font-bold whitespace-nowrap text-white"
                  style={{ backgroundImage: "linear-gradient(90deg,#b9862f,#c9788f)" }}
                >
                  Más elegido
                </span>
              )}

              <p className="text-sm font-semibold text-ink/65">{plan.nombre}</p>

              {precio != null ? (
                <>
                  <p className="mt-3.5 flex items-baseline gap-1.5">
                    <span className="text-[38px] font-extrabold tracking-[-0.02em] text-ink">
                      ${formatearPrecio(precio)}
                    </span>
                    <span className="text-sm text-ink/45">/{periodo === "anual" ? "año" : "mes"}</span>
                  </p>
                  <p className="mt-1.5 min-h-[18px] text-[12.5px] font-semibold text-green-light">
                    {ahorro != null && ahorro > 0 ? `Ahorras $${formatearPrecio(ahorro)} al año` : ""}
                  </p>
                </>
              ) : (
                <p className="mt-3.5 text-[32px] font-extrabold text-ink">A tu medida</p>
              )}

              <p className="mt-3.5 text-sm leading-relaxed text-ink/60">{plan.descripcion}</p>

              <ul className="mt-6 flex flex-col gap-3">
                {plan.features.map((feature) => (
                  <li key={feature} className="flex items-start gap-2.5 text-[13.5px] text-ink/80">
                    <CheckIcon accent={accent} />
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>

              {plan.ctaExterna ? (
                <a
                  href={plan.ctaHref}
                  target="_blank"
                  rel="noreferrer"
                  className={`mt-7 block rounded-xl px-6 py-3.5 text-center text-[14.5px] font-bold transition ${
                    plan.destacado ? "text-white" : "border border-ink/15 text-ink hover:border-ink/30"
                  }`}
                  style={plan.destacado ? { backgroundImage: "linear-gradient(90deg,#b9862f,#8a5f22)" } : undefined}
                >
                  {plan.ctaLabel}
                </a>
              ) : (
                <Link
                  href={plan.ctaHref}
                  className={`mt-7 block rounded-xl px-6 py-3.5 text-center text-[14.5px] font-bold transition ${
                    plan.destacado ? "text-white" : "border border-ink/15 text-ink hover:border-ink/30"
                  }`}
                  style={plan.destacado ? { backgroundImage: "linear-gradient(90deg,#b9862f,#8a5f22)" } : undefined}
                >
                  {plan.ctaLabel}
                </Link>
              )}

              {plan.secondaryCtaLabel && plan.secondaryCtaHref && (
                <Link
                  href={plan.secondaryCtaHref}
                  className="mt-3 block text-center text-xs text-ink/40 underline underline-offset-2 hover:text-ink/70"
                >
                  {plan.secondaryCtaLabel}
                </Link>
              )}
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

/** El check siempre es dorado, sea cual sea el accent de la página (ver handoff). */
function CheckIcon(_props: { accent: Accent }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7" stroke="#8a5f22" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
