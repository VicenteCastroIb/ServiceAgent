"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { clearToken } from "@/lib/auth";

export default function Nav() {
  const pathname = usePathname();
  const router = useRouter();

  if (pathname === "/login") return null;

  function cerrarSesion() {
    clearToken();
    router.push("/login");
  }

  const linkClass = (href: string) =>
    `px-3 py-2 rounded-md text-sm font-medium ${
      pathname.startsWith(href)
        ? "bg-blue-600 text-white"
        : "text-gray-700 hover:bg-gray-100"
    }`;

  return (
    <nav className="border-b bg-white">
      <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-3">
        <div className="flex gap-2">
          <Link href="/tenants" className={linkClass("/tenants")}>
            Negocios
          </Link>
          <Link href="/conversaciones" className={linkClass("/conversaciones")}>
            Conversaciones
          </Link>
          <Link href="/handoffs" className={linkClass("/handoffs")}>
            Conversaciones pausadas
          </Link>
        </div>
        <button
          onClick={cerrarSesion}
          className="text-sm text-gray-500 hover:text-gray-800"
        >
          Cerrar sesión
        </button>
      </div>
    </nav>
  );
}
