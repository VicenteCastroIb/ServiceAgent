"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { login } from "@/lib/api";
import { setToken } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setCargando(true);
    try {
      const token = await login(username, password);
      setToken(token);
      router.push("/tenants");
    } catch {
      setError("Usuario o clave incorrectos.");
    } finally {
      setCargando(false);
    }
  }

  return (
    <div className="mx-auto mt-16 max-w-sm">
      <h1 className="mb-1 text-xl font-semibold">Panel del negocio</h1>
      <p className="mb-6 text-sm text-gray-500">
        Ingresá con el usuario y clave que te dieron.
      </p>

      <form onSubmit={onSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium">Usuario</label>
          <input
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoFocus
          />
        </div>
        <div>
          <label className="block text-sm font-medium">Clave</label>
          <input
            type="password"
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={cargando}
          className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {cargando ? "Entrando..." : "Entrar"}
        </button>
      </form>
    </div>
  );
}
