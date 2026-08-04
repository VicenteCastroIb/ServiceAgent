import type { Metadata } from "next";
import Link from "next/link";
import ChatCard from "@/components/ChatCard";
import RubrosStrip from "@/components/RubrosStrip";
import FeatureGrid from "@/components/FeatureGrid";
import StepsSection from "@/components/StepsSection";
import ComparisonSection from "@/components/ComparisonSection";
import PricingCards, { type PlanPrecio } from "@/components/PricingCards";
import FAQAccordion from "@/components/FAQAccordion";
import CTABanner from "@/components/CTABanner";
import { ACCENT_DUO_GREEN, TEXT_GRADIENT } from "@/lib/accents";

export const metadata: Metadata = {
  title: "ServiceAgent — Tu WhatsApp e Instagram, atendiendo solos",
};

const AVATARES = [
  { letra: "M", bg: "bg-[#F0997B]", text: "text-[#4A1B0C]" },
  { letra: "R", bg: "bg-[#5DCAA5]", text: "text-[#04342C]" },
  { letra: "J", bg: "bg-[#85B7EB]", text: "text-[#042C53]" },
  { letra: "C", bg: "bg-[#ED93B1]", text: "text-[#4B1528]" },
];

const RUBROS = ["Clínicas", "Tiendas online", "Peluquerías", "Talleres", "Dentistas"];

const PLANES: PlanPrecio[] = [
  {
    nombre: "Básico",
    precioMensual: 19990,
    precioAnual: 199900,
    descripcion: "Para partir a atender por WhatsApp e Instagram.",
    features: [
      "Agente de IA por WhatsApp e Instagram",
      "Contexto propio: catálogo, precios, horarios y tono",
      "Número de WhatsApp incluido",
      "Panel con conversaciones y modo híbrido",
      "Derivación automática a un humano",
    ],
    ctaLabel: "Quiero ServiceAgent",
    ctaHref: "/registro",
  },
  {
    nombre: "Pro",
    precioMensual: 24990,
    precioAnual: 249900,
    descripcion: "Para negocios que agendan horas y no quieren perder ninguna.",
    features: [
      "Todo lo del plan Básico",
      "Agenda cupos, confirma y reagenda citas",
      "Recordatorios automáticos antes de cada hora",
      "Varios profesionales o boxes, cada uno con su agenda",
      "Atención 24/7 y soporte prioritario",
    ],
    destacado: true,
    ctaLabel: "Quiero ServiceAgent",
    ctaHref: "/registro",
  },
];

const FAQ_ITEMS = [
  {
    pregunta: "¿Necesito instalar algo?",
    respuesta:
      "No. Todo corre en la nube. Cargas tu catálogo, precios y horarios desde el panel web, y el asistente queda respondiendo por WhatsApp e Instagram sin instalar nada.",
  },
  {
    pregunta: "¿Puedo usar mi número de WhatsApp actual?",
    respuesta:
      "Se puede, con algunas salvedades técnicas. Lo más simple es usar un número nuevo dedicado solo a tu asistente, así tu número personal queda aparte.",
  },
  {
    pregunta: "¿El sistema suena robótico?",
    respuesta: "No responde con menús tipo 'marca 1 para...'. Entiende lenguaje natural y contesta con el tono que tú le definas.",
  },
  {
    pregunta: "¿Qué pasa si no sabe responder algo?",
    respuesta: "Si el cliente lo pide, hay un reclamo o el asistente no está seguro, deriva la conversación a una persona y te avisa.",
  },
  {
    pregunta: "¿Sirve para mi rubro?",
    respuesta:
      "Sirve para cualquier negocio que atienda clientes por WhatsApp o Instagram: tiendas, estéticas, talleres, dentistas y servicios con hora agendada.",
  },
];

export default function HomePage() {
  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="bg-grid-fade absolute inset-0" />
        <div className="relative mx-auto flex max-w-[1240px] flex-wrap items-center gap-12 px-5 py-[clamp(56px,8vw,88px)] sm:px-10">
          <div className="min-w-[300px] flex-[1_1_400px]">
            <div className="inline-flex items-center gap-2 rounded-full border border-green/30 bg-green/8 px-3.5 py-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-green" />
              <span className="text-xs font-semibold tracking-[0.04em] text-green-light">ATIENDE SIN PERDER NI UN CLIENTE</span>
            </div>
            <h1 className="mt-[22px] text-[clamp(34px,4.6vw,56px)] leading-[1.06] font-extrabold tracking-[-0.03em] text-ink">
              Tu WhatsApp e Instagram,
              <br />
              <span className="bg-clip-text text-transparent" style={{ backgroundImage: TEXT_GRADIENT }}>
                atendiendo solos.
              </span>
            </h1>
            <p className="mt-[22px] max-w-[460px] text-base leading-relaxed text-ink/65">
              El asistente con IA que conversa como una persona de tu equipo, no con menús robóticos. Responde
              al instante, agenda tus horas y cierra ventas, las 24 horas.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/registro"
                className="rounded-[10px] bg-green px-[26px] py-[13px] text-[15px] font-semibold text-white shadow-[0_10px_28px_rgba(185,134,47,0.3)] transition hover:bg-green-light"
              >
                Empieza gratis
              </Link>
              <Link
                href="#como-funciona"
                className="rounded-[10px] border border-ink/15 px-[26px] py-[13px] text-[15px] font-semibold text-ink transition hover:border-ink/30"
              >
                Míralo en acción
              </Link>
            </div>
            <div className="mt-7 flex items-center gap-2.5">
              <div className="flex">
                {AVATARES.map((a, i) => (
                  <span
                    key={a.letra}
                    style={{ marginLeft: i === 0 ? 0 : -8 }}
                    className={`flex h-7 w-7 items-center justify-center rounded-full border-2 border-cream text-[11px] font-bold ${a.bg} ${a.text}`}
                  >
                    {a.letra}
                  </span>
                ))}
              </div>
              <span className="text-[12.5px] text-ink/50">
                <b className="text-ink">+30 locales</b> ya atienden solos
              </span>
            </div>
            <p className="mt-5 text-[13px] text-ink/45">
              Sin contrato &nbsp;·&nbsp; Sin tarjeta de crédito &nbsp;·&nbsp; Puesta en marcha en minutos
            </p>
          </div>

          <div className="relative min-w-[320px] flex-[1.3_1_460px]" style={{ height: 500 }}>
            <div
              aria-hidden
              className="absolute top-[28%] left-[24%] h-[340px] w-[340px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-green/12 blur-[100px]"
            />
            <div
              aria-hidden
              className="absolute top-[74%] left-[74%] h-[260px] w-[260px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-violet/14 blur-[100px]"
            />
            <ChatCard
              negocio="Estética Ópalo"
              canal="whatsapp"
              notaFinal="Cita agendada por IA"
              className="absolute top-0 left-0 z-[1]"
              style={{ animation: "float-slow 6.5s ease-in-out infinite", transform: "rotate(-2deg)" }}
              mensajes={[
                { de: "cliente", texto: "hola! ¿tienen hora para manicure hoy?" },
                { de: "sistema", texto: "¡Hola! Sí 💅 Tengo a las 16:00 o 18:30. ¿Cuál prefieres?" },
                { de: "cliente", texto: "a las 18:30" },
                { de: "sistema", texto: "Listo, quedas agendada hoy 18:30 con Camila 💅" },
                { de: "cliente", texto: "genial, gracias!" },
              ]}
            />
            <ChatCard
              negocio="Automotriz"
              canal="instagram"
              className="absolute right-0 bottom-0 z-[2]"
              style={{ animation: "float-slow 7.5s ease-in-out infinite", transform: "rotate(2deg)" }}
              mensajes={[
                { de: "cliente", texto: "hola, tienen un radiador para hilux 2018?" },
                {
                  de: "sistema",
                  texto: "¡Sí! Te lo dejo acá 👇",
                  ficha: { titulo: "Radiador Toyota Hilux 2018", subtitulo: "Aluminio · 3 hileras · compatible 2016–2020", precio: "$84.990" },
                },
                { de: "cliente", texto: "tienen para instalarlo hoy mismo?" },
                { de: "sistema", texto: "Sí, tráelo antes de las 16:00 y sales con el radiador instalado 🔧" },
              ]}
            />
          </div>
        </div>
      </section>

      <RubrosStrip label="Usado por negocios que crecen rápido:" rubros={RUBROS} />

      <FeatureGrid
        eyebrow="FUNCIONES"
        eyebrowColor="green"
        title="Todo lo que hace por ti"
        subtitle="No es un chatbot de respuestas fijas. Es un asistente que entiende, agenda y vende."
        features={[
          {
            title: "Omnicanal real",
            description:
              "Un mismo asistente atiende WhatsApp e Instagram con la misma memoria del negocio. Nada se pierde entre canales.",
            photoLabel: "Foto: dueño de negocio respondiendo WhatsApp e Instagram desde el celular",
            photoSrc: "/images/features/omnicanal-real.jpg",
            large: true,
          },
          {
            title: "Respuestas al instante, 24/7",
            description: "Ningún cliente espera. Ni de madrugada, ni un domingo.",
            photoLabel: "Foto: notificación de celular a la madrugada, respondida al instante",
            photoSrc: "/images/features/respuestas-instante.jpg",
          },
          {
            title: "Deriva a un humano cuando hace falta",
            description: "Si el cliente lo pide o el tema se complica, avisa a tu equipo con todo el contexto.",
            photoLabel: "Foto: persona del equipo tomando una llamada o revisando el panel",
            photoSrc: "/images/features/derivacion-humano.jpg",
          },
          {
            title: "Se conecta a tu CRM o base de datos",
            description: "Sincroniza catálogo, precios y clientes con las herramientas que ya usas.",
            photoLabel: "Foto: pantalla mostrando integración entre panel y planilla/CRM",
            photoSrc: "/images/features/integracion-panel-crm.jpg",
          },
          {
            title: "Nunca inventa un precio",
            description: "Cada respuesta se valida contra tu catálogo real. Si no está seguro, no adivina.",
            photoLabel: "Foto: catálogo de productos con precios reales sobre un mostrador",
            photoSrc: "/images/features/catalogo-productos.jpg",
          },
        ]}
      />

      <StepsSection
        eyebrow="CÓMO FUNCIONA"
        eyebrowColor="violet"
        title="De cero a atendiendo, en 3 pasos"
        subtitle="Sin instalar nada, sin escribir una línea de código."
        steps={[
          {
            numero: 1,
            titulo: "Conecta tus canales",
            descripcion: "Vincula tu WhatsApp Business e Instagram en minutos, sin nada técnico de por medio.",
            photoLabel: "Foto: celular vinculando WhatsApp Business e Instagram",
            photoSrc: "/images/features/conecta-canales.jpg",
            color: "green",
          },
          {
            numero: 2,
            titulo: "Entrena la IA con tu negocio",
            descripcion: "Carga catálogo, precios, horarios y el tono con el que quieres que hable.",
            photoLabel: "Foto: dueño de negocio cargando catálogo y precios en el panel",
            photoSrc: "/images/features/entrena-ia-negocio.jpg",
            color: "violet",
          },
          {
            numero: 3,
            titulo: "Deja que atienda y venda solo",
            descripcion: "Responde, agenda y cierra ventas 24/7, y te avisa cuando de verdad te necesita.",
            photoLabel: "Foto: negocio funcionando de noche, celular con chat activo sobre el mostrador",
            photoSrc: "/images/features/deja-atiende-solo.jpg",
            color: "green",
          },
        ]}
      />

      <ComparisonSection
        title="La diferencia se nota de inmediato"
        problemas={[
          "Clientes escriben a las 11pm y nadie contesta hasta el otro día.",
          "Tienes que estar pendiente del celular todo el día, incluso los fines de semana.",
          "Pierdes ventas por responder tarde, o no responder.",
          "Contratar y entrenar personal para atender cuesta tiempo y dinero.",
        ]}
        beneficios={[
          "Responde al instante, 24/7, incluso de madrugada.",
          "Tú solo intervienes cuando el cliente lo pide o hay algo importante.",
          "Ninguna consulta se queda sin respuesta.",
          "Un asistente entrenado con tu negocio, activo en minutos.",
        ]}
        accent={ACCENT_DUO_GREEN}
      />

      <section id="precios" className="border-t border-ink/10 bg-cream-alt">
        <div className="mx-auto max-w-[1240px] px-5 py-[clamp(72px,9vw,110px)] sm:px-10">
          <div className="mx-auto max-w-[640px] text-center">
            <h2 className="text-[clamp(28px,3.6vw,42px)] leading-[1.15] font-extrabold tracking-[-0.025em] text-ink">
              Precios claros, sin letra chica
            </h2>
            <p className="mt-3.5 text-base leading-relaxed text-ink/55">Paga mensual o anual, tú eliges. Sin contrato.</p>
          </div>

          <div className="mx-auto mt-11 max-w-[820px]">
            <PricingCards planes={PLANES} accent={ACCENT_DUO_GREEN} />
            <p className="mt-6 text-center text-sm text-ink/55">
              ¿Vendes con catálogo?{" "}
              <Link href="/ecommerce" className="font-medium text-green-light underline underline-offset-2">
                Mira el plan Ecommerce
              </Link>
            </p>
          </div>
        </div>
      </section>

      <section id="faq" className="mx-auto max-w-[820px] px-5 py-[clamp(72px,9vw,110px)] sm:px-10">
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
        title="Deja que tu WhatsApp trabaje por ti"
        subtitle="Mientras lees esto, podría estar entrando un mensaje que nadie está respondiendo. Empecemos hoy."
        accent={ACCENT_DUO_GREEN}
        primaryLabel="Empieza gratis"
        primaryHref="/registro"
        secondaryLabel="Ver precios"
        secondaryHref="/#precios"
      />
    </>
  );
}
