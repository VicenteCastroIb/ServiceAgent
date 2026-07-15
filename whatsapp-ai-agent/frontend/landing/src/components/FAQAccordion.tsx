"use client";

import { useState } from "react";

export interface FAQItem {
  pregunta: string;
  respuesta: string;
}

export default function FAQAccordion({ items }: { items: FAQItem[] }) {
  const [abierto, setAbierto] = useState<number | null>(null);

  return (
    <div className="mx-auto max-w-2xl space-y-3">
      {items.map((item, i) => {
        const expandido = abierto === i;
        return (
          <div key={item.pregunta} className="rounded-2xl border border-slate-200 px-5 py-4">
            <button
              onClick={() => setAbierto(expandido ? null : i)}
              className="flex w-full items-center justify-between gap-4 text-left"
              aria-expanded={expandido}
            >
              <span className="font-medium text-slate-900">{item.pregunta}</span>
              <span
                className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-emerald-50 text-emerald-600 transition-transform ${
                  expandido ? "rotate-45" : ""
                }`}
              >
                <PlusIcon />
              </span>
            </button>
            {expandido && <p className="mt-3 text-sm text-slate-500">{item.respuesta}</p>}
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
