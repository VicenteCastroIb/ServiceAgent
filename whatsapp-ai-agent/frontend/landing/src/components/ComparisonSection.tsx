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
    <section className="mx-auto max-w-[1240px] px-5 py-[clamp(72px,9vw,110px)] sm:px-10">
      <div className="mx-auto max-w-[640px] text-center">
        <h2 className="text-[clamp(28px,3.6vw,42px)] leading-[1.15] font-extrabold tracking-[-0.025em] text-ink">{title}</h2>
        {subtitle && <p className="mt-3.5 text-base leading-relaxed text-ink/55">{subtitle}</p>}
      </div>

      <div className="mt-11 grid grid-cols-[repeat(auto-fit,minmax(320px,1fr))] gap-5">
        <div className="rounded-[20px] border border-ink/10 bg-card p-8">
          <span className="inline-flex rounded-full border border-ink/12 px-3.5 py-1.5 text-[12.5px] font-semibold text-ink/50">
            Sin ServiceAgent
          </span>
          <ul className="mt-[22px] flex flex-col gap-4">
            {problemas.map((problema) => (
              <li key={problema} className="flex items-start gap-2.5 text-[14.5px] leading-relaxed text-ink/65">
                <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full border-[1.5px] border-ink/30" />
                <span>{problema}</span>
              </li>
            ))}
          </ul>
        </div>

        <div
          className="rounded-[20px] p-8 text-white shadow-[0_20px_60px_rgba(185,134,47,0.18)]"
          style={{ backgroundImage: accent.gradient }}
        >
          <span className="inline-flex rounded-full bg-white/20 px-3.5 py-1.5 text-[12.5px] font-semibold">Con ServiceAgent</span>
          <ul className="mt-[22px] flex flex-col gap-4">
            {beneficios.map((beneficio) => (
              <li key={beneficio} className="flex items-start gap-2.5 text-[14.5px] leading-relaxed font-medium">
                <span className="mt-0.5 shrink-0">✓</span>
                <span>{beneficio}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}
