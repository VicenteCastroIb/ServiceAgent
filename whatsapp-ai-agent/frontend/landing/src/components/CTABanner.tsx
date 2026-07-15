import Link from "next/link";
import type { Accent } from "@/lib/accents";

interface CTABannerProps {
  title: string;
  subtitle: string;
  accent: Accent;
  primaryLabel?: string;
  primaryHref?: string;
  secondaryLabel?: string;
  secondaryHref?: string;
}

export default function CTABanner({
  title,
  subtitle,
  accent,
  primaryLabel = "Quiero ServiceAgent",
  primaryHref = "/precios",
  secondaryLabel,
  secondaryHref,
}: CTABannerProps) {
  return (
    <section className="mx-auto max-w-6xl px-6 py-16">
      <div
        className="overflow-hidden rounded-[2rem] px-8 py-14 text-center shadow-2xl sm:px-16"
        style={{ backgroundImage: accent.gradient }}
      >
        <h2 className="font-heading text-[28px] font-semibold text-white sm:text-[38px] sm:leading-[1.2]">{title}</h2>
        <p className="mx-auto mt-4 max-w-xl text-white/90">{subtitle}</p>
        <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
          <Link
            href={primaryHref}
            className="rounded-[14px] bg-white px-6 py-3 text-sm font-semibold text-slate-900 transition hover:bg-slate-100"
          >
            {primaryLabel}
          </Link>
          {secondaryLabel && secondaryHref && (
            <Link
              href={secondaryHref}
              className="rounded-[14px] border border-white/40 px-6 py-3 text-sm font-semibold text-white transition hover:bg-white/10"
            >
              {secondaryLabel}
            </Link>
          )}
        </div>
      </div>
    </section>
  );
}
