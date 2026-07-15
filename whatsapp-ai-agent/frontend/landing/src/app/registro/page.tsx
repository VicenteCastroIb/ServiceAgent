import type { Metadata } from "next";
import { Suspense } from "react";
import RegistroForm from "@/components/RegistroForm";
import { ACCENT_HOME } from "@/lib/accents";

export const metadata: Metadata = {
  title: "Creá tu cuenta — ServiceAgent",
  description: "Registrá tu negocio y activá tu asistente de WhatsApp e Instagram en minutos.",
};

export default function RegistroPage() {
  return (
    <section className="mx-auto max-w-lg px-6 py-16">
      <div className="mb-8 text-center">
        <h1 className="font-heading text-[32px] leading-[1.15] font-semibold text-slate-900 sm:text-[40px]">
          Creá tu cuenta
        </h1>
        <p className="mt-3 text-slate-500">
          Registrá tu negocio, activá tu suscripción y desde tu panel prendé el asistente para WhatsApp e
          Instagram.
        </p>
      </div>

      <Suspense fallback={null}>
        <RegistroForm accent={ACCENT_HOME} />
      </Suspense>
    </section>
  );
}
