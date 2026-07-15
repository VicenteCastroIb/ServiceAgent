const PASTEL_CLASSES = [
  "bg-amber-50 text-amber-700",
  "bg-emerald-50 text-emerald-700",
  "bg-blue-50 text-blue-700",
  "bg-pink-50 text-pink-700",
  "bg-violet-50 text-violet-700",
  "bg-teal-50 text-teal-700",
];

interface RubrosStripProps {
  rubros: string[];
}

export default function RubrosStrip({ rubros }: RubrosStripProps) {
  return (
    <section className="border-y border-slate-100 bg-slate-50/60 py-8">
      <div className="mx-auto flex max-w-6xl flex-col items-center gap-4 px-6 sm:flex-row sm:justify-center">
        <span className="text-xs font-semibold tracking-wide text-slate-400 uppercase">Pensado para</span>
        <div className="flex flex-wrap justify-center gap-2">
          {rubros.map((rubro, i) => (
            <span
              key={rubro}
              className={`rounded-full px-3.5 py-1.5 text-sm font-medium ${PASTEL_CLASSES[i % PASTEL_CLASSES.length]}`}
            >
              {rubro}
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}
