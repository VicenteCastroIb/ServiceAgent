import type { ReactNode } from "react";

interface Feature {
  icon: ReactNode;
  title: string;
  description: string;
  pastel: string;
}

interface FeatureGridProps {
  title: string;
  subtitle?: string;
  features: Feature[];
}

export default function FeatureGrid({ title, subtitle, features }: FeatureGridProps) {
  return (
    <section className="mx-auto max-w-6xl px-6 py-20">
      <div className="mx-auto max-w-2xl text-center">
        <h2 className="font-heading text-[32px] font-semibold text-slate-900 sm:text-[44px] sm:leading-[55px]">{title}</h2>
        {subtitle && <p className="mt-4 text-slate-500">{subtitle}</p>}
      </div>

      <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {features.map((feature) => (
          <div key={feature.title} className="rounded-2xl border border-slate-100 p-6 transition hover:shadow-md">
            <div className={`mb-4 flex h-10 w-10 items-center justify-center rounded-xl ${feature.pastel}`}>{feature.icon}</div>
            <h3 className="font-heading text-base font-semibold text-slate-900">{feature.title}</h3>
            <p className="mt-1.5 text-sm text-slate-500">{feature.description}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
