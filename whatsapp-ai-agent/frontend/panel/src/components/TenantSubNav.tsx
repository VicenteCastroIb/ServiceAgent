"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export default function TenantSubNav({ tenantId }: { tenantId: number }) {
  const pathname = usePathname();

  const tabs = [
    { href: `/tenants/${tenantId}/edit`, label: "Contexto" },
    { href: `/tenants/${tenantId}/agendamiento`, label: "Agendamiento" },
    { href: `/tenants/${tenantId}/catalogo`, label: "Catálogo" },
    { href: `/tenants/${tenantId}/pagos`, label: "Pagos" },
  ];

  return (
    <div className="mb-6 flex flex-wrap items-center gap-3 border-b border-gray-200 pb-3 text-sm">
      <Link href="/tenants" className="text-gray-500 hover:underline">
        ← Negocios
      </Link>
      <span className="text-gray-300">|</span>
      {tabs.map((tab) => (
        <Link
          key={tab.href}
          href={tab.href}
          className={
            pathname === tab.href
              ? "font-medium text-blue-600"
              : "text-gray-600 hover:text-blue-600 hover:underline"
          }
        >
          {tab.label}
        </Link>
      ))}
    </div>
  );
}
