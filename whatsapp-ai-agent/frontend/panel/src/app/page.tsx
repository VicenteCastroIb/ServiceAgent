"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";

export default function Home() {
  const router = useRouter();
  const { autenticado, cargando } = useAuth();

  useEffect(() => {
    if (!cargando) {
      router.replace(autenticado ? "/tenants" : "/login");
    }
  }, [autenticado, cargando, router]);

  return null;
}
