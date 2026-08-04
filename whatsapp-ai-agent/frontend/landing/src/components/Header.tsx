"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV_LINKS = [
  { href: "/", label: "Inicio" },
  { href: "/agendamiento", label: "Agendamiento" },
  { href: "/ecommerce", label: "Ecommerce" },
  { href: "/#precios", label: "Precios" },
];

export default function Header() {
  const pathname = usePathname();

  return (
    <header className="fixed inset-x-0 top-0 z-50 border-b border-ink/10 bg-cream/90 backdrop-blur-md">
      <div className="mx-auto flex h-[76px] max-w-[1240px] flex-wrap items-center justify-between gap-6 px-5 sm:px-10">
        <Link href="/" className="flex shrink-0 items-center gap-2.5">
          <Image src="/brand/logo.jpg" alt="" width={34} height={34} priority className="shrink-0" />
          <span className="text-base font-bold text-ink">
            ServiceAgent<span className="text-green">.</span>
          </span>
        </Link>

        <nav className="flex flex-wrap items-center gap-7">
          {NAV_LINKS.map((link) => {
            const activo = link.href === "/" ? pathname === "/" : pathname?.startsWith(link.href);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`text-[13.5px] font-medium transition-colors ${
                  activo ? "text-green-light" : "text-ink/65 hover:text-ink"
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex shrink-0 items-center gap-5">
          <a
            href={`${process.env.NEXT_PUBLIC_PANEL_URL ?? "http://localhost:3000"}/login`}
            className="hidden text-[13.5px] font-medium text-ink/65 hover:text-ink sm:block"
          >
            Entrar
          </a>
          <Link
            href="/#precios"
            className="rounded-full bg-green px-5 py-2.5 text-[13.5px] font-semibold whitespace-nowrap text-white transition hover:bg-green-light"
          >
            Empieza gratis
          </Link>
        </div>
      </div>
    </header>
  );
}
