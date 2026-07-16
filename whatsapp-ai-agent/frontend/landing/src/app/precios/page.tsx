import type { Metadata } from "next";
import Link from "next/link";
import PricingCards, { type PlanPrecio } from "@/components/PricingCards";
import ComparisonTable from "@/components/ComparisonTable";
import CTABanner from "@/components/CTABanner";
import { ACCENT_HOME } from "@/lib/accents";
import { whatsappSalesLink } from "@/lib/whatsapp";

export const metadata: Metadata = {
  title: "Precios — ServiceAgent",
  description: "Planes claros para atender, agendar y vender por WhatsApp con IA. Sin contrato, cancelás cuando quieras.",
};

const PLANES: PlanPrecio[] = [
  {
    nombre: "Básico",
    precioMensual: 19990,
    precioAnual: 199900,
    descripcion: "Para partir a atender por WhatsApp e Instagram.",
    features: [
      "Asistente de WhatsApp con IA que agenda solo",
      "Citas ilimitadas",
      "Tu número de WhatsApp incluido",
      "Panel con agenda, clientes y conversaciones",
      "Reagendar y cancelar por el mismo WhatsApp",
      "Horarios, servicios y duración configurables",
      "Soporte de lunes a viernes, horario oficina",
    ],
    ctaLabel: "Quiero ServiceAgent",
    ctaHref: whatsappSalesLink("Hola! Quiero contratar el plan Básico de ServiceAgent para mi negocio."),
    ctaExterna: true,
    secondaryCtaLabel: "Prefiero registrarme yo mismo",
    secondaryCtaHref: "/registro?plan=BASICO",
  },
  {
    nombre: "Pro",
    precioMensual: 24990,
    precioAnual: 249900,
    descripcion: "Para negocios con más volumen y más automatización.",
    features: [
      "Todo lo del plan Básico",
      "Recordatorios automáticos con confirmación de cita",
      "Varios profesionales o boxes, cada uno con su agenda",
      "Notificaciones por email de cada cita",
      "Reportes: no-shows evitados, citas por semana, horas peak",
      "Tu asistente atiende 24/7; soporte prioritario para vos",
    ],
    destacado: true,
    ctaLabel: "Quiero ServiceAgent",
    ctaHref: whatsappSalesLink("Hola! Quiero contratar el plan Pro de ServiceAgent para mi negocio."),
    ctaExterna: true,
    secondaryCtaLabel: "Prefiero registrarme yo mismo",
    secondaryCtaHref: "/registro?plan=PRO",
  },
  {
    nombre: "Ecommerce",
    precioMensual: null,
    precioAnual: null,
    descripcion: "Catálogo sincronizado con tu tienda. El bot busca, recomienda y vende.",
    features: [
      "Todo lo del plan Pro",
      "Catálogo sincronizado con tu tienda",
      "El bot busca, recomienda y lleva al pago",
      "Pensado para catálogos grandes",
    ],
    ctaLabel: "Cotizar",
    ctaHref: "/ecommerce",
  },
];

const FEATURE_ROWS = [
  { feature: "Asistente de WhatsApp con IA que agenda solo", basico: true, pro: true },
  { feature: "Citas / agendamiento ilimitado", basico: true, pro: true },
  { feature: "Tu número de WhatsApp incluido", basico: true, pro: true },
  { feature: "Panel con agenda, clientes y conversaciones", basico: true, pro: true },
  { feature: "Reagendar y cancelar por WhatsApp", basico: true, pro: true },
  { feature: "Horarios, servicios y duración configurables", basico: true, pro: true },
  { feature: "Recordatorios automáticos con confirmación", basico: false, pro: true },
  { feature: "Varios profesionales o boxes, cada uno con su agenda", basico: false, pro: true },
  { feature: "Notificaciones por email de cada cita", basico: false, pro: true },
  { feature: "Reportes (no-shows, citas por semana, horas peak)", basico: false, pro: true },
  { feature: "Atención 24/7", basico: false, pro: true },
  { feature: "Soporte prioritario", basico: false, pro: true },
];

export default function PreciosPage() {
  return (
    <>
      <section className="bg-slate-50/60 px-6 py-16 text-center">
        <span className="inline-flex items-center rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-500 ring-1 ring-slate-200">
          Sin contrato · cancelá cuando quieras
        </span>
        <h1 className="mx-auto mt-5 max-w-2xl font-heading text-[36px] leading-[1.15] font-semibold text-slate-900 sm:text-[48px]">
          Precios claros, sin{" "}
          <span className="bg-clip-text text-transparent" style={{ backgroundImage: ACCENT_HOME.gradient }}>
            letra chica
          </span>
        </h1>
        <p className="mx-auto mt-4 max-w-xl text-slate-500">
          Elegí cómo pagar y arrancá con tu asistente de WhatsApp. Sin instalar nada y sin contrato.
        </p>
      </section>

      <section className="mx-auto max-w-6xl px-6 py-16">
        <PricingCards planes={PLANES} accent={ACCENT_HOME} />
      </section>

      <section className="mx-auto max-w-4xl px-6 pb-20">
        <h2 className="text-center font-heading text-2xl font-semibold text-slate-900">Compará los planes</h2>
        <p className="mt-2 text-center text-sm text-slate-500">Todo lo que incluye cada plan de agendamiento.</p>

        <div className="mt-8">
          <ComparisonTable rows={FEATURE_ROWS} accent={ACCENT_HOME} />
        </div>

        <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-50/60 p-6 text-center">
          <p className="text-sm text-slate-600">
            <span className="font-medium text-slate-900">¿Tenés un ecommerce?</span> El plan Ecommerce incluye
            todo lo del Pro más el catálogo sincronizado con tu tienda: el bot busca, recomienda y lleva al
            pago. Como depende de tu catálogo y tu volumen, lo cotizamos a medida.{" "}
            <Link href="/ecommerce" className="font-medium text-emerald-600 underline underline-offset-2">
              Ver plan Ecommerce →
            </Link>
          </p>
        </div>
      </section>

      <CTABanner
        title="Empezá a atender con ServiceAgent"
        subtitle="Sin tarjeta para probar, sin instalar nada y sin dejar a tus clientes esperando."
        accent={ACCENT_HOME}
        primaryLabel="Hablar por WhatsApp"
        primaryHref={whatsappSalesLink("Hola! Quiero saber más sobre ServiceAgent para mi negocio.")}
        primaryExterna
        secondaryLabel="Registrarme yo mismo"
        secondaryHref="/registro?plan=BASICO"
      />
    </>
  );
}
