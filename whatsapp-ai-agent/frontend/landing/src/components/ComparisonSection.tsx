import type { Accent } from "@/lib/accents";

interface ComparisonSectionProps {
  title: string;
  subtitle?: string;
  problemas: string[];
  beneficios: string[];
  accent: Accent;
}

export default function ComparisonSection({ title, subtitle, problemas, beneficios, accent }: ComparisonSectionProps) {
  return (
    <section className="mx-auto max-w-6xl px-6 py-20">
      <div className="mx-auto max-w-2xl text-center">
        <h2 className="font-heading text-[32px] leading-[1.2] font-semibold text-slate-900 sm:text-[44px] sm:leading-[55px]">
          {title}
        </h2>
        {subtitle && <p className="mt-4 text-slate-500">{subtitle}</p>}
      </div>

      <div className="mt-12 grid gap-6 lg:grid-cols-2">
        <div className="rounded-3xl bg-slate-50 p-8">
          <p className="mb-5 inline-flex items-center gap-2 rounded-full bg-slate-200/70 px-3 py-1 text-xs font-medium text-slate-500">
            Sin ServiceAgent
          </p>
          <ul className="space-y-4">
            {problemas.map((problema) => (
              <li key={problema} className="flex items-start gap-3 text-sm text-slate-600">
                <CircleIcon />
                <span>{problema}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="rounded-3xl p-8 text-white shadow-xl" style={{ backgroundImage: accent.gradient }}>
          <p className="mb-5 inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-xs font-medium">
            Con ServiceAgent
          </p>
          <ul className="space-y-4">
            {beneficios.map((beneficio) => (
              <li key={beneficio} className="flex items-start gap-3 text-sm font-medium">
                <CheckCircleIcon />
                <span>{beneficio}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

function CircleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0 text-slate-300" aria-hidden="true">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.6" />
    </svg>
  );
}

function CheckCircleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0" aria-hidden="true">
      <circle cx="12" cy="12" r="9" fill="white" fillOpacity="0.2" />
      <path d="M8 12.5l2.5 2.5L16 9.5" stroke="white" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
