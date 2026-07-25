import Link from "next/link";
import type { Accent } from "@/lib/accents";

interface CTABannerProps {
  title: string;
  subtitle: string;
  accent: Accent;
  primaryLabel?: string;
  primaryHref?: string;
  /** true para links externos (ej. wa.me) - se abren en pestaña nueva en vez de navegación interna de Next. */
  primaryExterna?: boolean;
  secondaryLabel?: string;
  secondaryHref?: string;
}

export default function CTABanner({
  title,
  subtitle,
  accent,
  primaryLabel = "Quiero ServiceAgent",
  primaryHref = "/precios",
  primaryExterna = false,
  secondaryLabel,
  secondaryHref,
}: CTABannerProps) {
  return (
    <section className="mx-auto max-w-[1240px] px-5 pb-[clamp(72px,9vw,100px)] sm:px-10">
      <div
        className="rounded-[28px] px-6 py-[clamp(40px,6vw,64px)] text-center shadow-[0_30px_80px_rgba(185,134,47,0.18)] sm:px-10"
        style={{ backgroundImage: accent.gradient }}
      >
        <h2 className="text-[clamp(26px,3.4vw,38px)] leading-[1.2] font-extrabold tracking-[-0.02em] text-white">{title}</h2>
        <p className="mx-auto mt-4 max-w-[520px] text-base leading-relaxed text-white/90">{subtitle}</p>
        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          {primaryExterna ? (
            <a
              href={primaryHref}
              target="_blank"
              rel="noreferrer"
              className="rounded-xl bg-white px-7 py-3.5 text-[15px] font-bold text-ink transition hover:bg-white/90"
            >
              {primaryLabel}
            </a>
          ) : (
            <Link
              href={primaryHref}
              className="rounded-xl bg-white px-7 py-3.5 text-[15px] font-bold text-ink transition hover:bg-white/90"
            >
              {primaryLabel}
            </Link>
          )}
          {secondaryLabel && secondaryHref && (
            <Link
              href={secondaryHref}
              className="rounded-xl border border-white/40 px-7 py-3.5 text-[15px] font-semibold text-white transition hover:bg-white/10"
            >
              {secondaryLabel}
            </Link>
          )}
        </div>
      </div>
    </section>
  );
}
