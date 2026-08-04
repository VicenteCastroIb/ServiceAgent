import Image from "next/image";
import Link from "next/link";

const PRODUCTO_LINKS = [
  { href: "/agendamiento", label: "Agendamiento" },
  { href: "/ecommerce", label: "Ecommerce" },
  { href: "/#precios", label: "Precios" },
];

const RECURSOS_LINKS = [
  { href: "/#faq", label: "Preguntas frecuentes" },
  { href: "#", label: "Contacto" },
];

const LEGAL_LINKS = [
  { href: "/privacidad", label: "Privacidad" },
  { href: "/terminos", label: "Términos" },
];

export default function Footer() {
  return (
    <footer className="border-t border-ink/10">
      <div className="mx-auto grid max-w-[1240px] grid-cols-[repeat(auto-fit,minmax(160px,1fr))] gap-8 px-5 py-14 sm:px-10">
        <div>
          <div className="flex items-center gap-2.5">
            <Image src="/brand/logo.png" alt="" width={28} height={28} className="shrink-0" />
            <span className="text-[15px] font-bold text-ink">
              ServiceAgent<span className="text-green">.</span>
            </span>
          </div>
          <p className="mt-3.5 max-w-[260px] text-[13px] leading-relaxed text-ink/45">
            Agentes de IA para atender WhatsApp e Instagram, 24/7.
          </p>
          <div className="mt-5 flex gap-2.5">
            {["IG", "X", "in"].map((label) => (
              <span
                key={label}
                className="flex h-8 w-8 items-center justify-center rounded-full border border-ink/12 text-[11px] font-bold text-ink/60"
              >
                {label}
              </span>
            ))}
          </div>
        </div>

        <FooterColumn title="PRODUCTO" links={PRODUCTO_LINKS} />
        <FooterColumn title="RECURSOS" links={RECURSOS_LINKS} />
        <FooterColumn title="LEGAL" links={LEGAL_LINKS} />
      </div>
      <div className="border-t border-ink/10 px-5 py-5 text-center sm:px-10">
        <p className="text-[12.5px] text-ink/40">© {new Date().getFullYear()} ServiceAgent. Todos los derechos reservados.</p>
      </div>
    </footer>
  );
}

function FooterColumn({ title, links }: { title: string; links: { href: string; label: string }[] }) {
  return (
    <div>
      <p className="text-[12.5px] font-bold tracking-[0.04em] text-ink">{title}</p>
      <div className="mt-4 flex flex-col gap-2.5">
        {links.map((link) => (
          <Link key={link.label} href={link.href} className="text-[13.5px] text-ink/50 transition hover:text-ink/80">
            {link.label}
          </Link>
        ))}
      </div>
    </div>
  );
}
