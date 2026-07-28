"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "./auth-context";

/**
 * Redirige a /login si no hay sesión vigente (según GET /auth/me, resuelto
 * una vez por AuthProvider - ver auth-context.tsx). Devuelve true recién
 * cuando se confirmó que hay sesión; false mientras se resuelve /auth/me o
 * si no hay sesión - mismo contrato que antes (cuando esto miraba
 * sincrónicamente si había un JWT en localStorage).
 */
export function useRequireAuth(): boolean {
  const router = useRouter();
  const { autenticado, cargando } = useAuth();

  useEffect(() => {
    if (!cargando && !autenticado) {
      router.replace("/login");
    }
  }, [cargando, autenticado, router]);

  return autenticado && !cargando;
}
