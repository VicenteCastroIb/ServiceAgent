interface RubrosStripProps {
  label: string;
  rubros: string[];
}

export default function RubrosStrip({ label, rubros }: RubrosStripProps) {
  return (
    <section className="border-y border-ink/10 px-5 py-7 sm:px-10">
      <div className="mx-auto flex max-w-[1240px] flex-wrap items-center justify-center gap-[clamp(20px,4vw,44px)]">
        <span className="text-[13px] font-medium text-ink/45">{label}</span>
        {rubros.map((rubro) => (
          <span key={rubro} className="font-mono text-[13.5px] font-semibold tracking-[0.02em] text-ink/60 uppercase">
            {rubro}
          </span>
        ))}
      </div>
    </section>
  );
}
