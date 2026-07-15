import type { Accent } from "@/lib/accents";

interface ComparisonRow {
  feature: string;
  basico: boolean;
  pro: boolean;
}

interface ComparisonTableProps {
  rows: ComparisonRow[];
  accent: Accent;
}

export default function ComparisonTable({ rows, accent }: ComparisonTableProps) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-slate-200">
      <table className="w-full min-w-[420px] text-left text-sm">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-6 py-4 font-medium text-slate-500">Incluye</th>
            <th className="px-6 py-4 text-center font-medium text-slate-500">Básico</th>
            <th className="px-6 py-4 text-center font-semibold" style={{ color: accent.from }}>
              Pro
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.feature} className="border-t border-slate-100">
              <td className="px-6 py-3.5 text-slate-700">{row.feature}</td>
              <td className="px-6 py-3.5 text-center">{row.basico ? <CheckMark accent={accent} /> : <CrossMark />}</td>
              <td className="px-6 py-3.5 text-center">{row.pro ? <CheckMark accent={accent} /> : <CrossMark />}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CheckMark({ accent }: { accent: Accent }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" className="inline" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7" stroke={accent.from} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CrossMark() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" className="inline text-slate-300" aria-hidden="true">
      <path d="M6 6l12 12M18 6 6 18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
    </svg>
  );
}
