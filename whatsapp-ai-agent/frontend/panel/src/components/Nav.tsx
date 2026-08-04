"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { cerrarSesion as cerrarSesionEnBackend, listarHandoffs } from "@/lib/api";

const TABS = [
  { href: "/tenants", label: "Negocios" },
  { href: "/conversaciones", label: "Conversaciones" },
  { href: "/handoffs", label: "Pausadas" },
];

export default function Nav() {
  const pathname = usePathname();
  const router = useRouter();
  const { esAdmin, cerrarSesionLocal } = useAuth();
  const [pausadas, setPausadas] = useState<number | null>(null);

  useEffect(() => {
    if (pathname === "/login") return;
    let cancelado = false;
    listarHandoffs()
      .then((res) => {
        if (!cancelado) setPausadas(res.length);
      })
      .catch(() => {
        // Silencioso: es solo un badge informativo, no bloquea nada.
      });
    return () => {
      cancelado = true;
    };
  }, [pathname]);

  if (pathname === "/login") return null;

  function onCerrarSesion() {
    // Best-effort: el back ya limpia la cookie httpOnly (ver
    // AuthController.logout); si la llamada falla igual navegamos y
    // limpiamos el estado local, no queremos dejar a alguien atrapado en el
    // panel por un problema de red puntual al cerrar sesión.
    cerrarSesionEnBackend().catch(() => {});
    cerrarSesionLocal();
    router.push("/login");
  }

  return (
    <nav className="border-b border-ink/10 bg-card">
      <div className="mx-auto flex max-w-[1280px] flex-wrap items-center justify-between gap-4 px-5 py-3.5 sm:px-8">
        <div className="flex items-center gap-6">
          <Link href="/tenants" className="flex items-center gap-2.5">
            <Image src="/brand/logo.jpg" alt="" width={36} height={36} className="shrink-0" />
            <span className="flex flex-col leading-none">
              <span className="text-[14.5px] font-bold tracking-[-0.01em] text-ink">ServiceAgent.</span>
              <span className="text-[10.5px] font-medium tracking-[0.03em] text-ink/40 uppercase">Panel</span>
            </span>
          </Link>

          <div className="flex items-center gap-1 rounded-full border border-ink/10 bg-cream p-1">
            {TABS.map((tab) => {
              const activo = pathname.startsWith(tab.href);
              return (
                <Link
                  key={tab.href}
                  href={tab.href}
                  className={`relative flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-[13px] font-semibold transition ${
                    activo ? "text-white" : "text-ink/60 hover:text-ink"
                  }`}
                  style={activo ? { backgroundImage: "linear-gradient(90deg,#b9862f,#8a5f22)" } : undefined}
                >
                  {tab.label}
                  {tab.href === "/handoffs" && !!pausadas && (
                    <span
                      className={`flex h-[17px] min-w-[17px] items-center justify-center rounded-full px-1 text-[10px] font-bold ${
                        activo ? "bg-white/25 text-white" : "bg-warn-bg text-warn"
                      }`}
                    >
                      {pausadas}
                    </span>
                  )}
                </Link>
              );
            })}
          </div>
        </div>

        <div className="flex items-center gap-4">
          {esAdmin && (
            <span className="flex items-center gap-1.5 text-[12px] font-medium text-ink/45">
              <span className="h-1.5 w-1.5 rounded-full bg-green" aria-hidden />
              Admin
            </span>
          )}
          <button
            onClick={onCerrarSesion}
            className="text-[13px] font-semibold text-ink/50 transition hover:text-ink"
          >
            Cerrar sesión
          </button>
        </div>
      </div>
    </nav>
  );
}
