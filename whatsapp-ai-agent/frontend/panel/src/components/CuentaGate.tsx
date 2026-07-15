"use client";

import { ReactNode, useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { esAdminSegunToken, tenantIdSegunToken } from "@/lib/auth";
import { EstadoCuenta, obtenerEstadoCuenta } from "@/lib/api";

/**
 * Gating del panel por estado de cuenta (doc sección 12: "cuando la persona
 * inicie sesión y pague la mensualidad, se le habilita la sección de su
 * panel"). Envuelve toda la app (ver layout.tsx) y decide si mostrar el
 * dashboard real o una pantalla de "completá tu pago"/"estamos activando tu
 * cuenta" en su lugar.
 *
 * Solo aplica al DUEÑO de un negocio (login por tenant) - el admin (vos) ve
 * todo siempre, no tiene suscripción propia que pagar.
 *
 * Escape hatch: /tenants/{tenantId}/pagos (SU PROPIA página de pagos) nunca
 * se bloquea, aunque la cuenta no esté activa - si no, un dueño con la
 * tarjeta pendiente no podría llegar a la pantalla donde justamente se
 * registra la tarjeta (ver PagosPage, que ya permite iniciar la suscripción
 * sin ser admin - BillingController.iniciarSuscripcion usa puedeAcceder, no
 * esAdmin).
 *
 * Fail-open a propósito: si /estado no responde (problema de red/API
 * puntual), se muestra el dashboard en vez de bloquear todo el panel por un
 * error transitorio - este gate es UX, no la barrera de seguridad real (esa
 * la sigue poniendo el backend en cada endpoint, ver PanelAuth).
 */
export default function CuentaGate({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const [estado, setEstado] = useState<EstadoCuenta | null>(null);
  const [cargando, setCargando] = useState(true);

  const esAdmin = esAdminSegunToken();
  const tenantId = tenantIdSegunToken();
  const esPaginaDePagosPropia = tenantId != null && pathname?.startsWith(`/tenants/${tenantId}/pagos`);

  useEffect(() => {
    if (esAdmin || tenantId == null || pathname === "/login") {
      setCargando(false);
      return;
    }
    let cancelado = false;
    obtenerEstadoCuenta(tenantId)
      .then((res) => {
        if (!cancelado) setEstado(res);
      })
      .catch(() => {
        // fail-open (ver Javadoc de la clase) - se deja estado en null,
        // que más abajo se trata como "dejar pasar".
      })
      .finally(() => {
        if (!cancelado) setCargando(false);
      });
    return () => {
      cancelado = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [esAdmin, tenantId, pathname]);

  if (esAdmin || tenantId == null || pathname === "/login" || esPaginaDePagosPropia) {
    return <>{children}</>;
  }

  if (cargando) {
    return null;
  }

  if (estado === null || estado.listoParaOperar) {
    return <>{children}</>;
  }

  return <PantallaEstadoCuenta estado={estado} tenantId={tenantId} />;
}

function PantallaEstadoCuenta({ estado, tenantId }: { estado: EstadoCuenta; tenantId: number }) {
  const { texto, mostrarBotonPagos } = describirEstado(estado);

  return (
    <div className="mx-auto mt-16 max-w-md text-center">
      <div className="rounded-xl border border-gray-200 bg-white p-8 shadow-sm">
        <h1 className="text-lg font-semibold text-gray-900">{texto.titulo}</h1>
        <p className="mt-2 text-sm text-gray-500">{texto.detalle}</p>
        {mostrarBotonPagos && (
          <a
            href={`/tenants/${tenantId}/pagos`}
            className="mt-6 inline-block rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            Completar pago
          </a>
        )}
      </div>
    </div>
  );
}

function describirEstado(estado: EstadoCuenta): {
  texto: { titulo: string; detalle: string };
  mostrarBotonPagos: boolean;
} {
  if (estado.subscriptionStatus === "MOROSA") {
    return {
      texto: {
        titulo: "Tu pago está atrasado",
        detalle: "Regularizá tu suscripción para seguir usando tu asistente de WhatsApp e Instagram.",
      },
      mostrarBotonPagos: true,
    };
  }

  if (estado.subscriptionStatus === "CANCELADA") {
    return {
      texto: {
        titulo: "Tu suscripción está cancelada",
        detalle: "Reactivala para volver a usar tu asistente.",
      },
      mostrarBotonPagos: true,
    };
  }

  if (!estado.suscripcionActiva) {
    return {
      texto: {
        titulo: "Completá tu pago para activar tu cuenta",
        detalle: "Registrá tu tarjeta para empezar a usar tu asistente de WhatsApp e Instagram.",
      },
      mostrarBotonPagos: true,
    };
  }

  // Suscripción activa pero todavía sin ningún canal configurado - hoy el
  // aprovisionamiento de WhatsApp/Instagram lo hace el admin a mano (ver
  // InstagramController), así que acá no hay ninguna acción que el dueño
  // pueda hacer: solo esperar a que se lo activen.
  return {
    texto: {
      titulo: "¡Pago confirmado!",
      detalle: "Estamos activando tu WhatsApp e Instagram. Te vamos a avisar apenas esté listo.",
    },
    mostrarBotonPagos: false,
  };
}
