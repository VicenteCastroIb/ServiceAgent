"use client";

import { useState } from "react";

export interface FAQItem {
  pregunta: string;
  respuesta: string;
}

interface FAQAccordionProps {
  items: FAQItem[];
  /** Color del ícono +/× cuando la pregunta está abierta. */
  accentColor?: "green" | "violet";
}

export default function FAQAccordion({ items, accentColor = "green" }: FAQAccordionProps) {
  const [abierto, setAbierto] = useState<number | null>(null);

  return (
    <div className="mx-auto flex max-w-[820px] flex-col gap-3">
      {items.map((item, i) => {
        const expandido = abierto === i;
        return (
          <div key={item.pregunta} className="rounded-2xl border border-ink/10 bg-card px-[22px] py-[18px]">
            <button
              onClick={() => setAbierto(expandido ? null : i)}
              className="flex w-full items-center justify-between gap-4 text-left"
              aria-expanded={expandido}
            >
              <span className="text-[15px] font-semibold text-ink">{item.pregunta}</span>
              <span
                className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[15px] transition-transform ${
                  expandido ? "rotate-45" : ""
                } ${accentColor === "green" ? "bg-green/14 text-green-light" : "bg-violet/14 text-violet-light"}`}
              >
                <PlusIcon />
              </span>
            </button>
            {expandido && <p className="mt-3.5 text-[14.5px] leading-relaxed text-ink/60">{item.respuesta}</p>}
          </div>
        );
      })}
    </div>
  );
}

function PlusIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}
