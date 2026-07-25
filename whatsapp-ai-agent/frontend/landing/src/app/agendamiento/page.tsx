import type { Metadata } from "next";
import Link from "next/link";
import ChatCard from "@/components/ChatCard";
import RubrosStrip from "@/components/RubrosStrip";
import ComparisonSection from "@/components/ComparisonSection";
import StepsSection from "@/components/StepsSection";
import FeatureGrid from "@/components/FeatureGrid";
import ConversationSection from "@/components/ConversationSection";
import FAQAccordion from "@/components/FAQAccordion";
import CTABanner from "@/components/CTABanner";
import { ACCENT_DUO_GREEN, TEXT_GRADIENT } from "@/lib/accents";

export const metadata: Metadata = {
  title: "Agendamiento por WhatsApp con IA — ServiceAgent",
  description: "El asistente que ofrece tus horas libres, confirma la cita y envía el recordatorio, todo por WhatsApp.",
};

const RUBROS = ["Estéticas", "Dentistas", "Peluquerías", "Barberías", "Spas", "Kinesiólogos"];

const FAQ_ITEMS = [
  {
    pregunta: "¿Cómo sabe el bot mis horarios disponibles?",
    respuesta:
      "Tú cargas tu disponibilidad semanal por profesional o box desde el panel: días, horario de atención y duración de cada cita. El bot solo ofrece esos cupos libres, nunca inventa uno.",
  },
  {
    pregunta: "¿Envía recordatorios a mis clientes?",
    respuesta:
      "Sí, en el plan Pro. Envía un recordatorio automático antes de cada cita para bajar los no-shows, sin que tengas que acordarte de escribirle a nadie.",
  },
  {
    pregunta: "¿Qué pasa si el cliente quiere algo que el bot no maneja?",
    respuesta:
      "Deriva la conversación a una persona y te avisa. Puedes responder desde el panel, tomando el control de esa conversación puntual, mientras el resto se sigue agendando solo.",
  },
  {
    pregunta: "¿Sirve para mi rubro?",
    respuesta: "Sirve para cualquier negocio que atienda con hora agendada: estéticas, dentistas, peluquerías, barberías, talleres, kinesiólogos y más.",
  },
];

export default function AgendamientoPage() {
  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="bg-grid-fade absolute inset-0" />
        <div className="relative mx-auto flex max-w-[1240px] flex-wrap items-center gap-12 px-5 py-[clamp(56px,8vw,88px)] sm:px-10">
          <div className="min-w-[300px] flex-[1.1_1_400px]">
            <span className="inline-flex items-center gap-2 rounded-full border border-green/30 bg-green/8 px-3.5 py-1.5 text-xs font-semibold tracking-[0.04em] text-green-light">
              AGENDAMIENTO POR WHATSAPP CON IA
            </span>
            <h1 className="mt-[22px] text-[clamp(32px,4.2vw,50px)] leading-[1.1] font-extrabold tracking-[-0.03em] text-ink">
              Tu agenda{" "}
              <span className="bg-clip-text text-transparent" style={{ backgroundImage: TEXT_GRADIENT }}>
                se llena sola
              </span>
              , sin que muevas un dedo.
            </h1>
            <p className="mt-[22px] max-w-[460px] text-base leading-relaxed text-ink/65">
              El asistente con IA que atiende por WhatsApp, ofrece tus horas libres, confirma la cita y envía
              el recordatorio. Las 24 horas, también cuando tú no puedes.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/registro"
                className="rounded-[10px] bg-green px-[26px] py-[13px] text-[15px] font-semibold text-white shadow-[0_10px_28px_rgba(185,134,47,0.3)] transition hover:bg-green-light"
              >
                Empieza gratis
              </Link>
              <Link
                href="#conversacion"
                className="rounded-[10px] border border-ink/15 px-[26px] py-[13px] text-[15px] font-semibold text-ink transition hover:border-ink/30"
              >
                Ver cómo conversa
              </Link>
            </div>
            <p className="mt-5 text-[13px] text-ink/45">Sin tarjeta &nbsp;·&nbsp; Sin contrato &nbsp;·&nbsp; Sin instalar nada</p>
          </div>

          <div className="relative flex min-w-[300px] flex-[0.9_1_340px] justify-center" style={{ height: 420 }}>
            <div
              aria-hidden
              className="absolute top-[40%] left-1/2 h-[300px] w-[300px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-green/12 blur-[100px]"
            />
            <ChatCard
              negocio="Barbería Norte"
              canal="whatsapp"
              notaFinal="Cita agendada por IA"
              className="relative self-center"
              mensajes={[
                { de: "cliente", texto: "hola, ¿queda cupo para corte y barba hoy?" },
                { de: "bot", texto: "¡Hola! Sí 💈 Tengo con Diego a las 18:00 o con Marco a las 19:30. ¿Cuál prefieres?" },
                { de: "cliente", texto: "con Marco, a las 7 y media" },
                { de: "bot", texto: "Perfecto, quedas agendado con Marco hoy 19:30 💈" },
              ]}
            />
          </div>
        </div>
      </section>

      <RubrosStrip label="Pensado para negocios con hora agendada:" rubros={RUBROS} />

      <ComparisonSection
        title="Agendar a mano te cuesta dinero"
        subtitle="Cada mensaje sin responder es una hora que no se reservó."
        problemas={[
          "Mensajes que llegan a medianoche y nadie responde hasta el otro día.",
          "Horas que se pierden porque el cliente no alcanzó a confirmar.",
          "La libreta o el Excel que solo tú entiendes y que hay que llenar a mano.",
          "Sillas vacías por gente que olvidó su hora.",
        ]}
        beneficios={[
          "Responde al instante, a cualquier hora, todos los días.",
          "Ofrece los cupos libres y deja la cita confirmada de inmediato.",
          "Todo queda agendado solo, sin libreta ni Excel.",
          "Recordatorio automático antes de cada hora: menos olvidos.",
        ]}
        accent={ACCENT_DUO_GREEN}
      />

      <StepsSection
        eyebrow="CÓMO AGENDA POR TI"
        eyebrowColor="violet"
        title="Sin técnicos, sin instalar nada"
        subtitle="Le enseñas tu agenda y se pone a trabajar."
        steps={[
          {
            numero: 1,
            titulo: "El cliente escribe",
            descripcion: "Pregunta por una hora a cualquier momento del día. El bot responde de inmediato, como una persona.",
            photoLabel: "Foto: celular mostrando una conversación de WhatsApp entrando",
            color: "green",
          },
          {
            numero: 2,
            titulo: "Ofrece tus cupos libres",
            descripcion: "Conoce tu agenda y muestra las horas disponibles. El cliente elige y la cita queda confirmada.",
            photoLabel: "Foto: agenda o calendario con horas disponibles en una pantalla",
            color: "violet",
          },
          {
            numero: 3,
            titulo: "Confirma y recuerda",
            descripcion: "Envía el recordatorio antes de la hora. Menos olvidos, menos sillas vacías, sin que muevas un dedo.",
            photoLabel: "Foto: persona recibiendo un recordatorio de cita en el celular",
            color: "green",
          },
        ]}
      />

      <FeatureGrid
        eyebrow="FUNCIONES"
        eyebrowColor="green"
        title="Todo lo que hace por tu agenda"
        subtitle="No es un chatbot de respuestas fijas. Es un asistente que entiende, agenda y te ahorra horas."
        features={[
          {
            title: "Agenda 24/7",
            description: "El cliente reserva a la hora que sea, también de noche o un domingo. Tu agenda se llena mientras descansas.",
            photoLabel: "Foto: negocio con celular activo de noche, mostrador cerrado",
            large: true,
          },
          {
            title: "Menos no-shows",
            description: "Recordatorios automáticos antes de cada cita. Menos personas que no llegan y menos dinero perdido.",
            photoLabel: "Foto: recordatorio de cita en la pantalla de un celular",
          },
          {
            title: "Confirma la cita sola",
            description: "Ofrece los cupos, toma la reserva y la deja confirmada. Tú solo apareces a atender.",
            photoLabel: "Foto: cliente sonriendo al recibir confirmación de su cita",
          },
          {
            title: "Te pasa lo que importa",
            description: "Si el cliente pide algo especial o se complica, deriva la conversación. Tú decides cuándo intervenir.",
            photoLabel: "Foto: persona del equipo tomando el control de una conversación",
          },
          {
            title: "Conversación natural",
            description: "Nada de 'marca 1 para agendar'. Entiende lo que le escriben y responde con el tono de tu negocio.",
            photoLabel: "Foto: persona escribiendo un mensaje relajado en el celular",
          },
        ]}
      />

      <div id="conversacion">
        <ConversationSection
          eyebrow="CONVERSACIÓN REAL"
          eyebrowColor="violet"
          title="Responde las dudas y termina agendando"
          description="El cliente pregunta por horarios, ubicación o estacionamiento, y el bot aprovecha para invitarlo a reservar. Atiende como tu mejor recepcionista, a cualquier hora."
          checklist={[
            "Entiende preguntas sueltas, no solo comandos.",
            "Cierra la conversación ofreciendo una hora.",
            "Habla con el tono de tu negocio, no como un robot.",
          ]}
          negocio="Estética Ópalo"
          canal="whatsapp"
          tinted
          mensajes={[
            { de: "cliente", texto: "hola, ¿hasta qué hora atienden hoy?" },
            { de: "bot", texto: "Hoy hasta las 19:00 🕖 ¿te ayudo a agendar algo?" },
            { de: "cliente", texto: "sí, necesito un corte de pelo" },
            { de: "bot", texto: "Perfecto, tengo mañana a las 11:00 o 15:00. ¿Cuál prefieres?" },
            { de: "cliente", texto: "a las 15:00" },
            { de: "bot", texto: "Listo, quedas agendada mañana 15:00 💅" },
          ]}
        />
      </div>

      <section className="mx-auto max-w-[820px] px-5 py-[clamp(72px,9vw,110px)] sm:px-10">
        <div className="text-center">
          <h2 className="text-[clamp(28px,3.6vw,42px)] leading-[1.15] font-extrabold tracking-[-0.025em] text-ink">
            Preguntas frecuentes
          </h2>
        </div>
        <div className="mt-10">
          <FAQAccordion items={FAQ_ITEMS} accentColor="green" />
        </div>
      </section>

      <CTABanner
        title="Deja que tu WhatsApp llene tu agenda"
        subtitle="Mientras lees esto, alguien podría estar escribiendo para reservar y nadie le responde. Empecemos hoy."
        accent={ACCENT_DUO_GREEN}
        primaryLabel="Empieza gratis"
        primaryHref="/registro"
      />
    </>
  );
}
