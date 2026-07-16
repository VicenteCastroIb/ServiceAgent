import type { Metadata } from "next";
import Link from "next/link";
import WhatsAppMockup from "@/components/WhatsAppMockup";
import RubrosStrip from "@/components/RubrosStrip";
import ComparisonSection from "@/components/ComparisonSection";
import StepsSection from "@/components/StepsSection";
import FeatureGrid from "@/components/FeatureGrid";
import ConversationSection from "@/components/ConversationSection";
import PricingCards, { type PlanPrecio } from "@/components/PricingCards";
import FAQAccordion from "@/components/FAQAccordion";
import CTABanner from "@/components/CTABanner";
import { ACCENT_AGENDAMIENTO } from "@/lib/accents";
import { BanIcon, BellIcon, CalendarIcon, ChatIcon, CheckShieldIcon, ClockIcon, RepeatIcon, UsersIcon } from "@/lib/icons";

export const metadata: Metadata = {
  title: "Agendamiento por WhatsApp con IA — ServiceAgent",
  description: "El asistente que ofrece tus horas libres, confirma la cita y manda el recordatorio, todo por WhatsApp.",
};

const RUBROS = ["Estéticas", "Dentistas", "Peluquerías", "Barberías", "Spas", "Kinesiólogos"];

const PLANES: PlanPrecio[] = [
  {
    nombre: "Básico",
    precioMensual: 19990,
    precioAnual: 199900,
    descripcion: "Para partir a atender y agendar por WhatsApp.",
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
    ctaHref: "/precios",
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
    ctaHref: "/precios",
  },
];

const FAQ_ITEMS = [
  {
    pregunta: "¿Cómo sabe el bot mis horarios disponibles?",
    respuesta:
      "Vos cargás tu disponibilidad semanal por profesional o box desde el panel: días, horario de atención y duración de cada cita. El bot solo ofrece esos cupos libres, nunca inventa uno.",
  },
  {
    pregunta: "¿Manda recordatorios a mis clientes?",
    respuesta:
      "Sí, en el plan Pro. Manda un recordatorio automático antes de cada cita para bajar los no-shows, sin que vos tengas que acordarte de escribirle a nadie.",
  },
  {
    pregunta: "¿Qué pasa si el cliente quiere algo que el bot no maneja?",
    respuesta:
      "Deriva la conversación a un humano y te avisa. Vos podés responder desde el panel, tomando el control de esa conversación puntual, mientras el resto se sigue agendando solo.",
  },
  {
    pregunta: "¿Sirve para mi rubro?",
    respuesta:
      "Sirve para cualquier negocio que atienda con hora agendada: estéticas, dentistas, peluquerías, barberías, talleres, kinesiólogos y más.",
  },
];

export default function AgendamientoPage() {
  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden bg-[radial-gradient(120%_120%_at_15%_0%,#f0fdfa_0%,#99f6e4_45%,#5eead4_100%)]">
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-6 py-20 lg:grid-cols-2 lg:py-28">
          <div>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/70 px-3 py-1 text-xs font-medium text-teal-800 ring-1 ring-teal-900/10">
              <CalendarIcon width={14} height={14} /> Agendamiento por WhatsApp con IA
            </span>
            <h1 className="mt-5 font-heading text-[36px] leading-[1.1] font-semibold tracking-[-0.5px] text-slate-900 sm:text-[48px] sm:leading-[1.15]">
              Tu agenda{" "}
              <span className="bg-clip-text text-transparent" style={{ backgroundImage: ACCENT_AGENDAMIENTO.gradient }}>
                se llena sola
              </span>
              , sin que muevas un dedo.
            </h1>
            <p className="mt-6 max-w-md text-[17px] leading-[27px] text-slate-600">
              El asistente con inteligencia artificial que atiende por WhatsApp, ofrece tus horas libres,
              confirma la cita y manda el recordatorio. Las 24 horas, también cuando vos no podés.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/precios"
                className="rounded-[14px] px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-teal-900/10 transition hover:brightness-110"
                style={{ backgroundImage: ACCENT_AGENDAMIENTO.gradient }}
              >
                Quiero ServiceAgent
              </Link>
              <Link
                href="#conversacion"
                className="rounded-[14px] border-[1.25px] border-slate-200 bg-white px-6 py-3 text-sm font-semibold text-slate-900 transition hover:border-slate-300"
              >
                Demo, mirá cómo conversa
              </Link>
            </div>
            <p className="mt-5 text-sm text-teal-900/70">Sin tarjeta. Sin contrato. Sin instalar nada.</p>
          </div>

          <div className="flex justify-center">
            <WhatsAppMockup
              negocio="Barbería Norte"
              animado
              mensajes={[
                { de: "cliente", texto: "hola, queda cupo para un corte hoy?", hora: "16:20" },
                { de: "bot", texto: "¡Hola! Sí 💈 Hoy me queda a las 18:00 o 19:30. ¿Cuál te sirve?", hora: "16:20" },
                { de: "cliente", texto: "a las 7 y media", hora: "16:21" },
                {
                  de: "bot",
                  texto: "Genial, quedaste agendado para las 19:30 💈 Te mando un recordatorio antes.",
                  hora: "16:21",
                },
              ]}
            />
          </div>
        </div>
      </section>

      <RubrosStrip rubros={RUBROS} />

      <ComparisonSection
        title="Agendar a mano te cuesta plata"
        subtitle="Cada mensaje sin responder es una hora que no se reservó. Mirá el antes y el después."
        problemas={[
          "Mensajes que llegan a medianoche y nadie responde hasta el otro día.",
          "Horas que se pierden porque el cliente no alcanzó a confirmar.",
          "La libreta o el Excel que solo vos entendés y que hay que llenar a mano.",
          "Sillas vacías por gente que se olvidó de su hora.",
        ]}
        beneficios={[
          "Responde al instante, a cualquier hora, todos los días.",
          "Ofrece los cupos libres y deja la cita confirmada al toque.",
          "Todo queda agendado solo, sin libreta ni Excel.",
          "Recordatorio automático antes de cada hora: menos olvidos.",
        ]}
        accent={ACCENT_AGENDAMIENTO}
      />

      <StepsSection
        title="Cómo agenda por vos"
        subtitle="Sin técnicos, sin instalar nada. Le enseñás tu agenda y se pone a trabajar."
        accent={ACCENT_AGENDAMIENTO}
        steps={[
          {
            icon: <ChatIcon width={20} height={20} />,
            title: "El cliente escribe",
            description: "Pregunta por una hora a cualquier momento del día. El bot le responde al toque, como una persona.",
          },
          {
            icon: <CalendarIcon width={20} height={20} />,
            title: "Ofrece tus cupos libres",
            description: "Conoce tu agenda y le muestra las horas disponibles. El cliente elige y la cita queda confirmada.",
          },
          {
            icon: <BellIcon width={20} height={20} />,
            title: "Confirma y recuerda",
            description: "Manda el recordatorio antes de la hora. Menos olvidos, menos sillas vacías, sin que muevas un dedo.",
          },
        ]}
      />

      <FeatureGrid
        title="Todo lo que hace por tu agenda"
        subtitle="No es un chatbot de respuestas fijas. Es un asistente que entiende, agenda y te ahorra horas."
        features={[
          {
            icon: <ClockIcon width={20} height={20} />,
            title: "Agenda 24/7",
            description: "El cliente reserva a la hora que sea, también de noche o un domingo. Tu agenda se llena mientras descansás.",
            pastel: "bg-teal-50 text-teal-600",
          },
          {
            icon: <BanIcon width={20} height={20} />,
            title: "Menos no-shows",
            description: "Recordatorios automáticos antes de cada cita. Menos personas que no llegan y menos plata perdida.",
            pastel: "bg-orange-50 text-orange-600",
          },
          {
            icon: <CheckShieldIcon width={20} height={20} />,
            title: "Confirma la cita sola",
            description: "Ofrece los cupos, toma la reserva y la deja confirmada. Vos solo aparecés a atender.",
            pastel: "bg-emerald-50 text-emerald-600",
          },
          {
            icon: <UsersIcon width={20} height={20} />,
            title: "Te pasa lo que importa",
            description: "Si el cliente pide algo especial o se complica, te deriva la conversación. Vos decidís cuándo entrar.",
            pastel: "bg-blue-50 text-blue-600",
          },
          {
            icon: <ChatIcon width={20} height={20} />,
            title: "Conversación natural",
            description: "Nada de 'marca 1 para agendar'. Entiende lo que le escriben y responde con el tono de tu negocio.",
            pastel: "bg-violet-50 text-violet-600",
          },
          {
            icon: <RepeatIcon width={20} height={20} />,
            title: "Clientes que vuelven",
            description: "Después de atender puede recordar la próxima hora o invitar a reservar de nuevo. Más recurrencia, solo.",
            pastel: "bg-teal-50 text-teal-600",
          },
        ]}
      />

      <div id="conversacion">
        <ConversationSection
          eyebrow="Conversación real"
          title="Responde las dudas y termina agendando"
          description="El cliente pregunta por horarios, ubicación o estacionamiento, y el bot aprovecha para invitarlo a reservar. Atiende como tu mejor recepcionista, a cualquier hora."
          checklist={[
            "Entiende preguntas sueltas, no solo comandos.",
            "Cierra la conversación ofreciendo una hora.",
            "Habla con el tono de tu negocio, no como un robot.",
          ]}
          negocio="Estética Bella"
          mensajes={[
            { de: "cliente", texto: "hasta qué hora atienden hoy?", hora: "13:05" },
            {
              de: "bot",
              texto: "Hoy hasta las 19:00 🕖\nEstamos en Av. Providencia 1234. ¿Te ayudo con algo más?",
              hora: "13:05",
            },
            { de: "cliente", texto: "tienen estacionamiento?", hora: "13:06" },
            {
              de: "bot",
              texto: "Sí, tenemos convenio con el edificio de al lado 🅿️ ¿Quieres que te agende una hora?",
              hora: "13:06",
            },
            { de: "cliente", texto: "sí, mañana a las 15:00", hora: "13:07" },
            {
              de: "bot",
              texto: "Listo, quedaste agendada para mañana 15:00 💅 Te mando un recordatorio antes.",
              hora: "13:07",
            },
          ]}
        />
      </div>

      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="font-heading text-[32px] font-semibold text-slate-900 sm:text-[44px] sm:leading-[55px]">
            Precios claros, sin letra chica
          </h2>
          <p className="mt-4 text-slate-500">Pagá mensual o anual (2 meses gratis), tú eliges. Sin contrato ni letra chica.</p>
        </div>

        <div className="mt-12">
          <PricingCards planes={PLANES} accent={ACCENT_AGENDAMIENTO} />
          <p className="mt-6 text-center text-sm text-slate-500">
            ¿Vendés productos con catálogo?{" "}
            <Link href="/ecommerce" className="font-medium text-teal-600 underline underline-offset-2">
              Mirá el plan Ecommerce →
            </Link>
          </p>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="font-heading text-[32px] font-semibold text-slate-900 sm:text-[44px] sm:leading-[55px]">
            Preguntas frecuentes
          </h2>
        </div>
        <div className="mt-10">
          <FAQAccordion items={FAQ_ITEMS} />
        </div>
      </section>

      <CTABanner
        title="Dejá que tu WhatsApp llene tu agenda"
        subtitle="Mientras leés esto, alguien podría estar escribiendo para reservar y nadie le responde. Empecemos hoy."
        accent={ACCENT_AGENDAMIENTO}
      />
    </>
  );
}
