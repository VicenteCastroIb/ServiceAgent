import Link from "next/link";

const FOOTER_LINKS = [
  { href: "/agendamiento", label: "Agendamiento" },
  { href: "/ecommerce", label: "Ecommerce" },
  { href: "/precios", label: "Precios" },
  { href: "/blog", label: "Blog" },
  { href: "#", label: "Contacto" },
  { href: "#", label: "Privacidad" },
  { href: "#", label: "Entrar" },
];

export default function Footer() {
  return (
    <footer className="border-t border-slate-100">
      <div className="mx-auto flex max-w-7xl flex-col items-center gap-6 px-6 py-10 sm:flex-row sm:justify-between">
        <Link href="/" className="flex items-center gap-2">
          <span className="font-heading text-base font-semibold text-slate-900">ServiceAgent.</span>
        </Link>

        <nav className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2">
          {FOOTER_LINKS.map((link) => (
            <Link key={link.label} href={link.href} className="text-sm text-slate-500 transition hover:text-slate-900">
              {link.label}
            </Link>
          ))}
        </nav>

        <p className="text-xs text-slate-400">© {new Date().getFullYear()} ServiceAgent</p>
      </div>
    </footer>
  );
}
