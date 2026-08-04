"use client";

import Image from "next/image";
import { FormEvent, Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { login } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { establecerSesion } = useAuth();
  const pagoConfirmado = searchParams.get("pago") === "confirmado";

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      const sesion = await login(username, password);
      establecerSesion(sesion);
      router.push("/tenants");
    } catch {
      setError("Usuario o clave incorrectos.");
    } finally {
      setCargando(false);
    }
  }

  return (
    <div className="mx-auto mt-20 max-w-sm">
      <div className="mb-7 flex items-center gap-2.5">
        <Image src="/brand/logo.jpg" alt="" width={36} height={36} className="shrink-0" />
        <span className="text-[15px] font-bold tracking-[-0.01em] text-ink">ServiceAgent.</span>
      </div>

      <h1 className="mb-1 text-xl font-bold text-ink">Panel del negocio</h1>
      <p className="mb-6 text-sm text-ink/50">
        Ingresá con el usuario y clave que te dieron.
      </p>

      {pagoConfirmado && (
        <p className="mb-4 rounded-[10px] border border-ok/25 bg-ok-bg px-3.5 py-2.5 text-sm text-ok">
          ¡Listo! Tu pago quedó registrado. Iniciá sesión con el usuario y clave que creaste al registrarte.
        </p>
      )}

      <form onSubmit={onSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-ink/75">Usuario</label>
          <input
            className="mt-1 w-full rounded-[10px] border border-ink/15 bg-card px-3 py-2 text-sm text-ink focus:border-green/50 focus:outline-none"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoFocus
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-ink/75">Clave</label>
          <input
            type="password"
            className="mt-1 w-full rounded-[10px] border border-ink/15 bg-card px-3 py-2 text-sm text-ink focus:border-green/50 focus:outline-none"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>

        {error && (
          <p className="rounded-lg border border-error/20 bg-error-bg px-3 py-2 text-sm text-error">{error}</p>
        )}

        <button
          type="submit"
          disabled={cargando}
          className="w-full rounded-[10px] px-4 py-2.5 text-sm font-semibold text-white shadow-[0_8px_20px_rgba(185,134,47,0.25)] transition hover:brightness-105 disabled:opacity-50"
          style={{ backgroundImage: "linear-gradient(90deg,#b9862f,#8a5f22)" }}
        >
          {cargando ? "Entrando..." : "Entrar"}
        </button>
      </form>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}
