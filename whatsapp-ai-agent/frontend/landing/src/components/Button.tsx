import Link from "next/link";
import type { Accent } from "@/lib/accents";
import type { ReactNode } from "react";

interface ButtonProps {
  href: string;
  children: ReactNode;
  variant?: "primary" | "secondary" | "dark" | "white";
  accent?: Accent;
  className?: string;
}

const BASE =
  "inline-flex items-center justify-center gap-2 rounded-[14px] px-6 py-3 text-sm font-semibold transition";

export default function Button({ href, children, variant = "primary", accent, className = "" }: ButtonProps) {
  if (variant === "secondary") {
    return (
      <Link
        href={href}
        className={`${BASE} border-[1.25px] border-slate-200 bg-white text-slate-900 hover:border-slate-300 ${className}`}
      >
        {children}
      </Link>
    );
  }

  if (variant === "dark") {
    return (
      <Link href={href} className={`${BASE} bg-[#0f172b] text-white hover:bg-slate-800 ${className}`}>
        {children}
      </Link>
    );
  }

  if (variant === "white") {
    return (
      <Link href={href} className={`${BASE} bg-white text-slate-900 hover:bg-slate-100 ${className}`}>
        {children}
      </Link>
    );
  }

  const style = accent ? { backgroundImage: accent.gradient } : undefined;
  return (
    <Link
      href={href}
      style={style}
      className={`${BASE} text-white shadow-lg shadow-slate-900/10 hover:brightness-110 ${className}`}
    >
      {children}
    </Link>
  );
}
