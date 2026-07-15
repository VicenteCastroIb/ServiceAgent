interface FloatingChipProps {
  emoji: string;
  title: string;
  subtitle: string;
  tone?: "neutral" | "success";
  className?: string;
}

export default function FloatingChip({ emoji, title, subtitle, tone = "neutral", className = "" }: FloatingChipProps) {
  return (
    <div
      className={`absolute hidden w-[190px] items-center gap-3 rounded-2xl bg-white px-3 py-2.5 shadow-xl shadow-slate-900/10 ring-1 ring-slate-900/5 lg:flex ${className}`}
    >
      <span
        className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-base ${
          tone === "success" ? "bg-emerald-100" : "bg-slate-100"
        }`}
      >
        {emoji}
      </span>
      <div className="min-w-0">
        <p className="truncate text-xs font-semibold text-slate-800">{title}</p>
        <p className={`text-xs ${tone === "success" ? "text-emerald-600" : "text-slate-500"}`}>{subtitle}</p>
      </div>
    </div>
  );
}
