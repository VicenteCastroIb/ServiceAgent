"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV_LINKS = [
  { href: "/", label: "Inicio" },
  { href: "/agendamiento", label: "Agendamiento" },
  { href: "/ecommerce", label: "Ecommerce" },
  { href: "/precios", label: "Precios" },
  { href: "/blog", label: "Blog" },
];

export default function Header() {
  const pathname = usePathname();

  return (
    <header className="fixed inset-x-0 top-0 z-50 border-b border-white/10 bg-slate-950/90 backdrop-blur-md">
      <div className="mx-auto flex h-[76px] max-w-7xl items-center justify-between px-6">
        <Link href="/" className="flex shrink-0 items-center gap-2">
          <LogoMark />
          <span className="font-heading text-lg font-semibold text-white">ServiceAgent.</span>
        </Link>

        <nav className="hidden items-center gap-8 md:flex">
          {NAV_LINKS.map((link) => {
            const activo = link.href === "/" ? pathname === "/" : pathname?.startsWith(link.href);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`text-sm font-medium transition-colors ${
                  activo ? "text-emerald-400" : "text-slate-300 hover:text-white"
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex shrink-0 items-center gap-4">
          {/* Placeholder: apunta al login real del panel una vez desplegado. */}
          <Link href="#" className="hidden text-sm font-medium text-slate-300 hover:text-white sm:block">
            Entrar
          </Link>
          <Link
            href="/precios"
            className="rounded-[14px] bg-[#0f172b] px-6 py-3 text-sm font-semibold text-white ring-1 ring-white/10 transition hover:bg-slate-800"
          >
            Quiero ServiceAgent
          </Link>
        </div>
      </div>
    </header>
  );
}

function LogoMark() {
  return (
    <svg width="26" height="26" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path
        d="M12 3C7 3 3 6.6 3 11c0 2.2 1 4.2 2.7 5.6-.1 1-.4 2.3-1.2 3.4 1.5 0 3-.5 4.2-1.4 1 .3 2.1.4 3.3.4 5 0 9-3.6 9-8S17 3 12 3Z"
        stroke="#34D399"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path d="M9 11c0-1.7 1.3-3 3-3" stroke="#34D399" strokeWidth="1.6" strokeLinecap="round" />
    </svg>
  );
}
