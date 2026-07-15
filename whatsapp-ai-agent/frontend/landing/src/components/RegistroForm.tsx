"use client";

import { FormEvent, ReactNode, useState } from "react";
import { useSearchParams } from "next/navigation";
import { ApiError, registrarNegocio, type PlanRegistro } from "@/lib/api";
import type { Accent } from "@/lib/accents";

const USERNAME_PATTERN = /^[a-zA-Z0-9._-]+$/;

interface RegistroFormProps {
  accent: Accent;
}

export default function RegistroForm({ accent }: RegistroFormProps) {
  const searchParams = useSearchParams();
  const planInicial: PlanRegistro = searchParams.get("plan") === "PRO" ? "PRO" : "BASICO";

  const [plan, setPlan] = useState<PlanRegistro>(planInicial);
  const [businessName, setBusinessName] = useState("");
  const [ownerEmail, setOwnerEmail] = useState("");
  const [panelUsername, setPanelUsername] = useState("");
  const [panelPassword, setPanelPassword] = useState("");
  const [panelPasswordConfirm, setPanelPasswordConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);
  const [resultado, setResultado] = useState<{ urlRegistroTarjeta: string | null; mensaje: string | null } | null>(
    null
  );

  function validar(): string | null {
    if (businessName.trim().length < 2) return "Contanos el nombre de tu negocio.";
    if (panelUsername.length < 4) return "El usuario debe tener al menos 4 caracteres.";
    if (!USERNAME_PATTERN.test(panelUsername)) return "El usuario solo puede tener letras, números, puntos, guiones y guion bajo.";
    if (panelPassword.length < 8) return "La clave debe tener al menos 8 caracteres.";
    if (panelPassword !== panelPasswordConfirm) return "Las claves no coinciden.";
    return null;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    const errorValidacion = validar();
    if (errorValidacion) {
      setError(errorValidacion);
      return;
    }

    setCargando(true);
    try {
      const respuesta = await registrarNegocio({
        businessName: businessName.trim(),
        ownerEmail,
        panelUsername,
        panelPassword,
        plan,
      });
      setResultado(respuesta);
      if (respuesta.urlRegistroTarjeta) {
        window.location.href = respuesta.urlRegistroTarjeta;
      }
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Algo salió mal. Probá de nuevo.");
    } finally {
      setCargando(false);
    }
  }

  if (resultado) {
    return (
      <div className="rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <div
          className="mx-auto flex h-12 w-12 items-center justify-center rounded-full text-white"
          style={{ backgroundImage: accent.gradient }}
        >
          ✓
        </div>
        <h2 className="mt-4 font-heading text-xl font-semibold text-slate-900">
          {resultado.urlRegistroTarjeta ? "Te estamos redirigiendo..." : "¡Listo, tu negocio quedó registrado!"}
        </h2>
        <p className="mt-2 text-sm text-slate-500">
          {resultado.urlRegistroTarjeta
            ? "Te llevamos a registrar tu tarjeta de forma segura en Flow."
            : resultado.mensaje}
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} className="rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
      <div className="mb-6 flex justify-center">
        <div className="inline-flex items-center rounded-full bg-slate-100 p-1 text-sm font-medium">
          <button
            type="button"
            onClick={() => setPlan("BASICO")}
            className={`rounded-full px-4 py-2 transition ${
              plan === "BASICO" ? "bg-white text-slate-900 shadow-sm" : "text-slate-500"
            }`}
          >
            Básico
          </button>
          <button
            type="button"
            onClick={() => setPlan("PRO")}
            className={`rounded-full px-4 py-2 transition ${
              plan === "PRO" ? "text-white shadow-sm" : "text-slate-500"
            }`}
            style={plan === "PRO" ? { backgroundImage: accent.gradient } : undefined}
          >
            Pro
          </button>
        </div>
      </div>

      <div className="space-y-4">
        <Campo label="Nombre de tu negocio">
          <input
            className={inputClass}
            value={businessName}
            onChange={(e) => setBusinessName(e.target.value)}
            placeholder="Ej: Cafetería Don José"
            autoFocus
          />
        </Campo>

        <Campo label="Tu email">
          <input
            type="email"
            className={inputClass}
            value={ownerEmail}
            onChange={(e) => setOwnerEmail(e.target.value)}
            placeholder="dueno@minegocio.cl"
            required
          />
        </Campo>

        <Campo label="Usuario para tu panel">
          <input
            className={inputClass}
            value={panelUsername}
            onChange={(e) => setPanelUsername(e.target.value)}
            placeholder="mi-negocio"
          />
        </Campo>

        <div className="grid grid-cols-2 gap-4">
          <Campo label="Clave">
            <input
              type="password"
              className={inputClass}
              value={panelPassword}
              onChange={(e) => setPanelPassword(e.target.value)}
            />
          </Campo>
          <Campo label="Repetí la clave">
            <input
              type="password"
              className={inputClass}
              value={panelPasswordConfirm}
              onChange={(e) => setPanelPasswordConfirm(e.target.value)}
            />
          </Campo>
        </div>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <button
          type="submit"
          disabled={cargando}
          style={{ backgroundImage: accent.gradient }}
          className="mt-2 w-full rounded-[14px] px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-slate-900/10 transition hover:brightness-110 disabled:opacity-50"
        >
          {cargando ? "Creando tu cuenta..." : "Crear mi cuenta"}
        </button>

        <p className="text-center text-xs text-slate-400">
          Vas a registrar tu tarjeta en Flow (nunca vemos tu número de tarjeta). Podés cancelar cuando quieras.
        </p>
      </div>
    </form>
  );
}

const inputClass =
  "mt-1 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm text-slate-900 focus:border-slate-400 focus:outline-none";

function Campo({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      {children}
    </label>
  );
}
