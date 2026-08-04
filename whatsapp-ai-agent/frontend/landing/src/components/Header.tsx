"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";

const NAV_LINKS = [
  { href: "/", label: "Inicio" },
  { href: "/agendamiento", label: "Agendamiento" },
  { href: "/ecommerce", label: "Ecommerce" },
  { href: "/#precios", label: "Precios" },
];

export default function Header() {
  // Arriba del todo: header transparente con la misma imagen del hero
  // (continuacion visual real, via bg-fixed). Al scrollear pasa a fondo
  // crema solido y el texto cambia a tono oscuro para seguir legible.
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    function onScroll() {
      setScrolled(window.scrollY > 8);
    }
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 overflow-hidden border-b transition-all duration-700 ease-in-out ${
        scrolled ? "border-ink/10 bg-cream shadow-[0_1px_0_rgba(43,38,32,0.04)]" : "border-transparent"
      }`}
    >
      {!scrolled && (
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 bg-fixed bg-[url('/brand/hero-fondo.jpg')] bg-cover bg-top"
        />
      )}
      <div className="relative mx-auto flex h-[76px] max-w-[1240px] flex-wrap items-center justify-between gap-6 px-5 sm:px-10">
        <Link href="/" className="flex shrink-0 items-center gap-2.5">
          <Image src="/brand/logo.png" alt="" width={34} height={34} priority className="shrink-0" />
          <span className={`text-[17px] font-black transition-colors ${scrolled ? "text-ink" : "text-white"}`}>
            ServiceAgent<span className="text-green">.</span>
          </span>
        </Link>

        <nav className="flex flex-wrap items-center gap-7">
          {NAV_LINKS.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={`relative text-[14.5px] font-bold transition-all duration-200 ease-out after:absolute after:inset-x-0 after:-bottom-1 after:h-[2px] after:origin-left after:scale-x-0 after:rounded-full after:transition-transform after:duration-300 after:ease-out after:content-[''] hover:after:scale-x-100 active:scale-95 ${
                scrolled ? "text-ink hover:text-ink/70 after:bg-ink" : "text-white hover:text-white/75 after:bg-white"
              }`}
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="flex shrink-0 items-center gap-5">
          <a
            href={`${process.env.NEXT_PUBLIC_PANEL_URL ?? "http://localhost:3000"}/login`}
            className={`relative hidden text-[14.5px] font-bold transition-all duration-200 ease-out after:absolute after:inset-x-0 after:-bottom-1 after:h-[2px] after:origin-left after:scale-x-0 after:rounded-full after:transition-transform after:duration-300 after:ease-out after:content-[''] hover:after:scale-x-100 active:scale-95 sm:block ${
              scrolled ? "text-ink hover:text-ink/70 after:bg-ink" : "text-white hover:text-white/75 after:bg-white"
            }`}
          >
            Entrar
          </a>
          <Link
            href="/#precios"
            className="rounded-full bg-green px-5 py-2.5 text-[13.5px] font-semibold whitespace-nowrap text-white transition-all duration-200 hover:bg-green-light active:scale-95"
          >
            Empieza gratis
          </Link>
        </div>
      </div>
    </header>
  );
}
