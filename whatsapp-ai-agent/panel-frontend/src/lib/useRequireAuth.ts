"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getToken } from "./auth";

/** Redirige a /login si no hay token guardado. Devuelve true cuando hay token. */
export function useRequireAuth(): boolean {
  const router = useRouter();
  const token = typeof window !== "undefined" ? getToken() : null;

  useEffect(() => {
    if (!token) {
      router.replace("/login");
    }
  }, [token, router]);

  return !!token;
}
