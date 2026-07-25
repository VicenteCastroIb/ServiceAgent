/**
 * Placeholder para una foto real pendiente de encargar/subir (ver README del
 * handoff de diseño: cada tarjeta de features/steps tiene un "image-slot").
 * Reemplazar por un <Image> de next/image cuando exista el asset final.
 */
interface PhotoSlotProps {
  label: string;
  height?: number;
  className?: string;
}

export default function PhotoSlot({ label, height = 150, className = "" }: PhotoSlotProps) {
  return (
    <div
      role="img"
      aria-label={label}
      title={label}
      style={{ height }}
      className={`flex w-full items-center justify-center rounded-[14px] border border-ink/8 bg-ink/[0.03] px-4 text-center ${className}`}
    >
      <div className="flex flex-col items-center gap-2 text-ink/30">
        <ImageIcon />
        <span className="max-w-[220px] text-[11px] leading-snug">{label}</span>
      </div>
    </div>
  );
}

function ImageIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="3.5" y="4.5" width="17" height="15" rx="2.5" stroke="currentColor" strokeWidth="1.5" />
      <circle cx="8.5" cy="9.5" r="1.5" stroke="currentColor" strokeWidth="1.5" />
      <path d="M5 16.5 9.5 12 13 15l2.5-2.5L20 17" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
