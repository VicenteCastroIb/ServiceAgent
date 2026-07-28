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
    <div className="mb-6 flex flex-wrap items-center gap-3 border-b border-ink/10 pb-3 text-sm">
      <Link href="/tenants" className="font-medium text-ink/45 hover:text-ink">
        ← Negocios
      </Link>
      <span className="text-ink/15">|</span>
      {tabs.map((tab) => (
        <Link
          key={tab.href}
          href={tab.href}
          className={
            pathname === tab.href
              ? "font-semibold text-green-light"
              : "text-ink/55 hover:text-ink"
          }
        >
          {tab.label}
        </Link>
      ))}
    </div>
  );
}
