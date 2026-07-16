import type { Metadata } from "next";
import Link from "next/link";
import WhatsAppMockup from "@/components/WhatsAppMockup";
import FloatingChip from "@/components/FloatingChip";
import RubrosStrip from "@/components/RubrosStrip";
import FeatureGrid from "@/components/FeatureGrid";
import PricingCards, { type PlanPrecio } from "@/components/PricingCards";
import FAQAccordion from "@/components/FAQAccordion";
import CTABanner from "@/components/CTABanner";
import { ACCENT_AGENDAMIENTO, ACCENT_ECOMMERCE, ACCENT_HOME } from "@/lib/accents";
import {
  CalendarIcon,
  ChatIcon,
  CheckShieldIcon,
  ClockIcon,
  HandoffIcon,
  PackageIcon,
  SparkleIcon,
} from "@/lib/icons";

export const metadata: Metadata = {
  title: "ServiceAgent — Tu WhatsApp, atendiendo solo",
};

const RUBROS = ["Estéticas", "Dentistas", "Peluquerías", "Talleres mecánicos", "Tiendas", "Spas"];

const PLANES: PlanPrecio[] = [
  {
    nombre: "Básico",
    precioMensual: 19990,
    precioAnual: 199900,
    descripcion: "Para partir a atender por WhatsApp e Instagram.",
    features: [
      "Agente de IA reactivo por WhatsApp e Instagram",
      "Contexto propio del negocio: catálogo, precios, horarios y tono",
      "Número de WhatsApp incluido",
      "Panel con conversaciones y modo híbrido",
      "Derivación automática a un humano",
      "Soporte de lunes a viernes, horario oficina",
    ],
    ctaLabel: "Quiero ServiceAgent",
    ctaHref: "/precios",
  },
  {
    nombre: "Pro",
    precioMensual: 24990,
    precioAnual: 249900,
    descripcion: "Para negocios que agendan horas y no quieren perder ninguna.",
    features: [
      "Todo lo del plan Básico",
      "Agenda cupos, confirma y reagenda citas por WhatsApp",
      "Recordatorios automáticos antes de cada hora",
      "Varios profesionales o boxes, cada uno con su agenda",
      "Reportes: no-shows evitados, citas por semana, horas peak",
      "Atención 24/7 y soporte prioritario",
    ],
    destacado: true,
    ctaLabel: "Quiero ServiceAgent",
    ctaHref: "/precios",
  },
];

const FAQ_ITEMS = [
  {
    pregunta: "¿Necesito instalar algo?",
    respuesta:
      "No. Todo corre en la nube. Vos cargás tu catálogo, precios y horarios desde el panel web, y el asistente queda respondiendo por WhatsApp e Instagram sin instalar nada en tu celular ni en tu computador.",
  },
  {
    pregunta: "¿Puedo usar mi número de WhatsApp actual?",
    respuesta:
      "Se puede, con algunas salvedades técnicas que te explicamos al momento de activarlo. Lo que más recomendamos (y lo más simple) es usar un número nuevo dedicado solo a tu asistente, así tu número personal queda aparte.",
  },
  {
    pregunta: "¿El bot suena robótico?",
    respuesta:
      "No responde con menús tipo 'marca 1 para...'. Entiende lo que el cliente escribe en lenguaje natural y contesta con el tono que vos le definas, como lo haría alguien de tu equipo.",
  },
  {
    pregunta: "¿Qué pasa si no sabe responder algo?",
    respuesta:
      "Si el cliente lo pide, hay un reclamo, se negocia un precio, o el asistente no está seguro de la respuesta, deriva la conversación a una persona y te avisa. Vos podés tomar el control desde el panel en cualquier momento.",
  },
  {
    pregunta: "¿Sirve para mi rubro?",
    respuesta:
      "Sirve para cualquier negocio que atienda clientes por WhatsApp: tiendas de ropa, comida, estéticas, talleres, dentistas y servicios con hora agendada. Vos cargás el contexto de tu propio negocio, así que se adapta a lo que vendas.",
  },
];

export default function HomePage() {
  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden bg-[radial-gradient(120%_120%_at_15%_0%,#ecfdf5_0%,#a7f3d0_45%,#6ee7b7_100%)]">
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-6 py-20 lg:grid-cols-2 lg:py-28">
          <div>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/70 px-3 py-1 text-xs font-medium text-emerald-800 ring-1 ring-emerald-900/10">
              Atención por WhatsApp con IA
            </span>
            <h1 className="mt-5 font-heading text-[40px] leading-[1.08] font-semibold tracking-[-1px] text-slate-900 sm:text-[57.6px] sm:leading-[60px] sm:tracking-[-1.44px]">
              Tu WhatsApp,{" "}
              <span
                className="bg-clip-text text-transparent"
                style={{ backgroundImage: ACCENT_HOME.gradient }}
              >
                atendiendo solo.
              </span>
            </h1>
            <p className="mt-6 max-w-md text-[17px] leading-[27px] text-slate-600">
              El asistente con inteligencia artificial que conversa como una persona de tu equipo, no con
              menús robóticos. Responde al instante, agenda tus horas y cierra ventas de tu catálogo,
              también cuando vos no podés.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/precios"
                className="rounded-[14px] bg-[#0f172b] px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
              >
                Quiero ServiceAgent
              </Link>
              <Link
                href="/agendamiento"
                className="rounded-[14px] border-[1.25px] border-slate-200 bg-white px-6 py-3 text-sm font-semibold text-slate-900 transition hover:border-slate-300"
              >
                Míralo en acción
              </Link>
            </div>
            <p className="mt-5 text-sm text-emerald-900/70">
              ✓ Sin contrato &nbsp; ✓ Hablás con tu propio negocio, no con un bot genérico
            </p>
          </div>

          <div className="relative mx-auto flex justify-center">
            <FloatingChip
              emoji="💈"
              title="Corte + barba"
              subtitle="$12.990"
              className="-top-4 -left-6 rotate-[-4deg]"
            />
            <FloatingChip
              emoji="✅"
              title="Cita confirmada"
              subtitle="hoy 18:30"
              tone="success"
              className="-right-8 bottom-16 rotate-[3deg]"
            />
            <WhatsAppMockup
              negocio="Estética Bella"
              animado
              mensajes={[
                { de: "cliente", texto: "hasta qué hora atienden hoy?", hora: "13:05" },
                {
                  de: "bot",
                  texto: "¡Hola! 😊 Hoy atendemos hasta las 19:00.\nEstamos en Av. Providencia 1234, ¿te ayudo con algo más?",
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
        </div>
      </section>

      <RubrosStrip rubros={RUBROS} />

      {/* Dos formas de que trabaje por vos */}
      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="font-heading text-[32px] font-semibold text-slate-900 sm:text-[44px] sm:leading-[55px]">
            Dos formas de que trabaje por vos
          </h2>
          <p className="mt-4 text-slate-500">Elegí tu mundo: agenda que se llena sola, o catálogo que vende solo.</p>
        </div>

        <div className="mt-12 space-y-6">
          <div
            className="grid items-center gap-10 overflow-hidden rounded-[2rem] bg-slate-50 p-8 sm:p-12 lg:grid-cols-2"
          >
            <div className="flex justify-center">
              <WhatsAppMockup
                negocio="Óptica Vista"
                animado
                mensajes={[
                  { de: "cliente", texto: "Hola! tienen hora para mañana?", hora: "21:47" },
                  {
                    de: "bot",
                    texto: "¡Hola! Sí 🙂 Para examen de vista tengo a las 11:30 o 16:00. ¿Cuál te acomoda?",
                    hora: "21:47",
                  },
                  { de: "cliente", texto: "a las 16:00 porfa", hora: "21:48" },
                  {
                    de: "bot",
                    texto: "Perfecto, tu hora quedó confirmada para mañana 16:00 👓 Te esperamos.",
                    hora: "21:48",
                  },
                ]}
              />
            </div>
            <div>
              <span
                className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium"
                style={{ color: ACCENT_AGENDAMIENTO.from, backgroundColor: "#0d948814" }}
              >
                <CalendarIcon width={14} height={14} /> Agendamiento
              </span>
              <h3 className="mt-4 font-heading text-[26px] font-semibold text-slate-900">Tu agenda se llena sola</h3>
              <p className="mt-3 text-slate-500">
                Para estéticas, dentistas, peluquerías y todo negocio que vive de las horas. El bot ofrece
                tus cupos, confirma la cita y manda el recordatorio. Vos solo apareces.
              </p>
              <Link
                href="/agendamiento"
                className="mt-6 inline-flex items-center gap-2 rounded-[14px] px-6 py-3 text-sm font-semibold text-white transition hover:brightness-110"
                style={{ backgroundImage: ACCENT_AGENDAMIENTO.gradient }}
              >
                Ver agendamiento →
              </Link>
            </div>
          </div>

          <div className="grid items-center gap-10 overflow-hidden rounded-[2rem] bg-slate-50 p-8 sm:p-12 lg:grid-cols-2">
            <div className="order-2 lg:order-1">
              <span
                className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium"
                style={{ color: ACCENT_ECOMMERCE.from, backgroundColor: "#ea580c14" }}
              >
                <PackageIcon width={14} height={14} /> Ecommerce
              </span>
              <h3 className="mt-4 font-heading text-[26px] font-semibold text-slate-900">Vende mientras dormís</h3>
              <p className="mt-3 text-slate-500">
                El cliente pregunta por un producto y el bot le manda la ficha con foto, precio y link de
                compra. Como tener un vendedor contestando a cualquier hora.
              </p>
              <Link
                href="/ecommerce"
                className="mt-6 inline-flex items-center gap-2 rounded-[14px] px-6 py-3 text-sm font-semibold text-white transition hover:brightness-110"
                style={{ backgroundImage: ACCENT_ECOMMERCE.gradient }}
              >
                Ver ecommerce →
              </Link>
            </div>
            <div className="order-1 flex justify-center lg:order-2">
              <WhatsAppMockup
                negocio="Moto Parts"
                animado
                mensajes={[
                  { de: "cliente", texto: "hola, tienen filtro de aceite para una pulsar ns 200?", hora: "20:11" },
                  {
                    de: "bot",
                    texto: "¡Hola! Sí 🙌 El original sale $6.490, te lo dejo acá 👇",
                    hora: "20:11",
                    producto: {
                      emoji: "🛵",
                      nombre: "Filtro de aceite Bajaj Pulsar NS200",
                      subtitulo: "Original · compatible 200/220",
                      precio: "$6.490",
                      dominio: "MOTOPARTS.CL",
                    },
                  },
                  { de: "cliente", texto: "tienen para despachar hoy?", hora: "20:12" },
                  {
                    de: "bot",
                    texto: "Sí, si compras antes de las 18:00 te llega hoy mismo 🚀 ¿Te armo el pedido?",
                    hora: "20:12",
                  },
                ]}
              />
            </div>
          </div>
        </div>
      </section>

      {/* Puesta en marcha */}
      <section className="bg-slate-950 py-20">
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-6 lg:grid-cols-2">
          <div>
            <span className="inline-flex items-center rounded-full border border-emerald-400/30 px-3 py-1 text-xs font-medium text-emerald-400">
              Puesta en marcha en minutos
            </span>
            <h2 className="mt-5 font-heading text-[32px] leading-[1.15] font-semibold text-white sm:text-[40px]">
              De cero a atendiendo clientes, sin nada técnico de por medio.
            </h2>
            <p className="mt-4 text-slate-400">
              Conectá tu WhatsApp, contale tu negocio y listo. Del resto se encarga ServiceAgent. Sin
              instalar nada, sin escribir una línea de código.
            </p>

            <ol className="mt-8 space-y-6">
              {[
                {
                  n: 1,
                  t: "Te damos tu número ServiceAgent",
                  d: "Te entregamos un número de WhatsApp nuevo, dedicado a tu asistente. Sin tocar tu número personal.",
                },
                {
                  n: 2,
                  t: "Le contás tu negocio",
                  d: "Horarios, productos, precios y forma de hablar. El bot aprende a atender como vos.",
                },
                {
                  n: 3,
                  t: "Contesta y vende solo",
                  d: "Desde el primer día responde, agenda y cierra ventas las 24 horas, sin que muevas un dedo.",
                },
              ].map((paso) => (
                <li key={paso.n} className="flex gap-4">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-400/10 text-sm font-semibold text-emerald-400">
                    {paso.n}
                  </span>
                  <div>
                    <p className="font-medium text-white">{paso.t}</p>
                    <p className="mt-1 text-sm text-slate-400">{paso.d}</p>
                  </div>
                </li>
              ))}
            </ol>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
            <p className="mb-4 flex items-center gap-2 text-sm text-slate-300">
              <span className="h-2 w-2 rounded-full bg-emerald-400" /> Tu bot está en línea
            </p>
            <div className="space-y-3">
              {[
                { icon: <ChatIcon width={18} height={18} />, t: "WhatsApp conectado", d: "Tu número, listo para atender" },
                { icon: <PackageIcon width={18} height={18} />, t: "Catálogo cargado", d: "Tus productos y precios reales" },
                { icon: <SparkleIcon width={18} height={18} />, t: "Bot respondiendo", d: "Activo las 24 horas" },
              ].map((item) => (
                <div
                  key={item.t}
                  className="flex items-center gap-3 rounded-xl border border-white/10 bg-white/[0.02] px-4 py-3.5"
                >
                  <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-white/5 text-emerald-400">
                    {item.icon}
                  </span>
                  <div className="flex-1">
                    <p className="text-sm font-medium text-white">{item.t}</p>
                    <p className="text-xs text-slate-400">{item.d}</p>
                  </div>
                  <CheckShieldIcon width={18} height={18} className="text-emerald-400" />
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <FeatureGrid
        title="Todo lo que hace por vos"
        subtitle="No es un chatbot de respuestas fijas. Es un asistente que entiende, agenda y vende."
        features={[
          {
            icon: <ClockIcon width={20} height={20} />,
            title: "Responde al instante, 24/7",
            description: "Ningún cliente se queda esperando. Ni de madrugada, ni un domingo, ni cuando estás ocupado.",
            pastel: "bg-emerald-50 text-emerald-600",
          },
          {
            icon: <ChatIcon width={20} height={20} />,
            title: "Conversa como una persona",
            description: "Nada de menús con números. Entiende lo que le escriben y responde de forma natural.",
            pastel: "bg-blue-50 text-blue-600",
          },
          {
            icon: <CheckShieldIcon width={20} height={20} />,
            title: "Nunca inventa un producto o precio",
            description: "Cada respuesta se valida contra tu catálogo real. Si no está seguro, no adivina.",
            pastel: "bg-teal-50 text-teal-600",
          },
          {
            icon: <SparkleIcon width={20} height={20} />,
            title: "Siempre con tu tono",
            description: "Habla como tu marca, no como un robot genérico. Aprende tu forma de atender.",
            pastel: "bg-amber-50 text-amber-600",
          },
          {
            icon: <CalendarIcon width={20} height={20} />,
            title: "Agenda y vende, en un solo bot",
            description: "Confirma horas y cierra ventas de catálogo desde el mismo número de WhatsApp.",
            pastel: "bg-violet-50 text-violet-600",
          },
          {
            icon: <HandoffIcon width={20} height={20} />,
            title: "Te pasa la conversación cuando hace falta",
            description: "Si el cliente lo pide o el tema se complica, avisa a tu equipo y se hace a un lado.",
            pastel: "bg-pink-50 text-pink-600",
          },
        ]}
      />

      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="font-heading text-[32px] font-semibold text-slate-900 sm:text-[44px] sm:leading-[55px]">
            Precios claros, sin letra chica
          </h2>
          <p className="mt-4 text-slate-500">Pagá mensual o anual (2 meses gratis), vos elegís. Sin contrato ni letra chica.</p>
        </div>

        <div className="mt-12">
          <PricingCards planes={PLANES} accent={ACCENT_HOME} />
          <p className="mt-6 text-center text-sm text-slate-500">
            ¿Vendés con catálogo?{" "}
            <Link href="/ecommerce" className="font-medium text-emerald-600 underline underline-offset-2">
              Mirá el plan Ecommerce
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
        title="Dejá que tu WhatsApp trabaje por vos"
        subtitle="Mientras leés esto, podría estar entrando un mensaje que nadie está respondiendo. Empecemos hoy."
        accent={ACCENT_HOME}
        secondaryLabel="Ver precios"
        secondaryHref="/precios"
      />
    </>
  );
}
