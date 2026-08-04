import Image from "next/image";

/**
 * Foto de una tarjeta de features/steps. Si `src` viene seteado, renderiza la
 * imagen real (next/image, recortada con object-cover al alto fijo de la
 * tarjeta). Si no, muestra el placeholder con la descripción de la foto
 * pendiente (ver PROMPTS_IMAGENES_LEONARDO.md para encargar la que falte).
 */
interface PhotoSlotProps {
  label: string;
  src?: string;
  height?: number;
  className?: string;
  /**
   * false = sin esquinas ni borde propios, para ir "a sangre" (edge-to-edge)
   * pegada a los bordes de una card con overflow-hidden (así el recorte lo
   * hace la card y la imagen queda más grande / protagonista). Default true.
   */
  rounded?: boolean;
}

export default function PhotoSlot({ label, src, height = 150, className = "", rounded = true }: PhotoSlotProps) {
  const shape = rounded ? "rounded-[14px] border border-ink/8" : "";

  if (src) {
    return (
      <div
        style={{ height }}
        className={`relative w-full overflow-hidden bg-ink/[0.03] ${shape} ${className}`}
      >
        <Image
          src={src}
          alt={label.replace(/^Foto:\s*/i, "")}
          fill
          sizes="(min-width: 1024px) 520px, 100vw"
          className="object-cover"
        />
      </div>
    );
  }

  return (
    <div
      role="img"
      aria-label={label}
      title={label}
      style={{ height }}
      className={`flex w-full items-center justify-center bg-ink/[0.03] px-4 text-center ${shape} ${className}`}
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
