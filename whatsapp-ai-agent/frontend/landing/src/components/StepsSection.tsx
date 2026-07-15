import type { Accent } from "@/lib/accents";
import type { ReactNode } from "react";

interface Step {
  icon: ReactNode;
  title: string;
  description: string;
}

interface StepsSectionProps {
  title: string;
  subtitle?: string;
  steps: Step[];
  accent: Accent;
}

export default function StepsSection({ title, subtitle, steps, accent }: StepsSectionProps) {
  return (
    <section className="mx-auto max-w-6xl px-6 py-20">
      <div className="mx-auto max-w-2xl text-center">
        <h2 className="font-heading text-[32px] font-semibold text-slate-900 sm:text-[44px] sm:leading-[55px]">{title}</h2>
        {subtitle && <p className="mt-4 text-slate-500">{subtitle}</p>}
      </div>

      <div className="mt-12 grid gap-6 sm:grid-cols-3">
        {steps.map((step, i) => (
          <div
            key={step.title}
            className="relative overflow-hidden rounded-2xl border-t-4 bg-white p-6 shadow-sm ring-1 ring-slate-100"
            style={{ borderTopColor: accent.from }}
          >
            <span className="pointer-events-none absolute -top-3 right-3 font-heading text-6xl font-semibold text-slate-50">
              {i + 1}
            </span>
            <div
              className="relative mb-4 flex h-11 w-11 items-center justify-center rounded-xl text-white"
              style={{ backgroundImage: accent.gradient }}
            >
              {step.icon}
            </div>
            <h3 className="relative font-heading text-lg font-semibold text-slate-900">{step.title}</h3>
            <p className="relative mt-2 text-sm text-slate-500">{step.description}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
