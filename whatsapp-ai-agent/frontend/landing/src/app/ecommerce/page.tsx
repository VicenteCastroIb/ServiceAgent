import type { Metadata } from "next";
import Link from "next/link";
import WhatsAppMockup from "@/components/WhatsAppMockup";
import FloatingChip from "@/components/FloatingChip";
import RubrosStrip from "@/components/RubrosStrip";
import ComparisonSection from "@/components/ComparisonSection";
import StepsSection from "@/components/StepsSection";
import FeatureGrid from "@/components/FeatureGrid";
import ConversationSection from "@/components/ConversationSection";
import FAQAccordion from "@/components/FAQAccordion";
import CTABanner from "@/components/CTABanner";
import { ACCENT_ECOMMERCE } from "@/lib/accents";
import { CardIcon, CheckShieldIcon, ClockIcon, HandoffIcon, PackageIcon, SearchIcon, SparkleIcon } from "@/lib/icons";

export const metadata: Metadata = {
  title: "Catálogo vendiendo por WhatsApp con IA — ServiceAgent",
  description: "El asistente que conoce tu catálogo, recomienda productos y cierra la venta con link de pago, dentro del chat.",
};

const RUBROS = ["Tiendas de ropa", "Repuestos", "Tecnología", "Ferreterías", "Regalos", "Deco"];

const ECOMMERCE_FEATURES = [
  "Todo lo del plan Pro",
  "Catálogo sincronizado con tu tienda (WooCommerce u otra)",
  "Ficha de producto con foto y precio directo en el chat",
  "Link de compra dentro de la misma conversación",
  "Recomendaciones y armado de carrito",
  "Pensado para catálogos grandes",
];

const FAQ_ITEMS = [
  {
    pregunta: "¿Cómo se sincroniza mi catálogo?",
    respuesta:
      "Conectamos tu tienda (WooCommerce u otra plataforma compatible) y tus productos, precios, fotos y stock quedan disponibles para que el bot los use en cada conversación.",
  },
  {
    pregunta: "¿Qué pasa si un producto se agota?",
    respuesta: "El bot responde con el stock real. Si no hay disponible, se lo dice al cliente en vez de venderle algo que no existe.",
  },
  {
    pregunta: "¿Cómo recibo el pago?",
    respuesta:
      "El link de pago se genera con tu propia pasarela (Transbank, Flow u otra que definas vos). El dinero llega directo a tu cuenta, nosotros no lo intermediamos.",
  },
  {
    pregunta: "¿Puedo usarlo sin tienda online?",
    respuesta:
      "Si todavía no tenés una tienda online, podés cargar el catálogo manualmente desde el panel. Funciona igual, solo que sin sincronización automática.",
  },
];

export default function EcommercePage() {
  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden bg-[radial-gradient(120%_120%_at_15%_0%,#fff7ed_0%,#fed7aa_45%,#fdba74_100%)]">
        <div className="mx-auto grid max-w-6xl items-center gap-16 px-6 py-20 lg:grid-cols-2 lg:py-28">
          <div>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white/70 px-3 py-1 text-xs font-medium text-orange-800 ring-1 ring-orange-900/10">
              <PackageIcon width={14} height={14} /> Tu catálogo vendiendo por WhatsApp con IA
            </span>
            <h1 className="mt-5 font-heading text-[36px] leading-[1.15] font-semibold tracking-[-0.5px] text-slate-900 sm:text-[46px]">
              Convertí cada{" "}
              <span className="bg-clip-text text-transparent" style={{ backgroundImage: ACCENT_ECOMMERCE.gradient }}>
                &ldquo;¿tienen esto?&rdquo;
              </span>{" "}
              en una venta.
            </h1>
            <p className="mt-6 max-w-md text-[17px] leading-[27px] text-slate-600">
              El asistente con inteligencia artificial que conoce tu catálogo, recomienda productos y manda
              la ficha con foto, precio y link de compra. Cierra ventas las 24 horas, también cuando vos no
              podés.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/precios"
                className="rounded-[14px] px-6 py-3 text-sm font-semibold text-white shadow-lg shadow-orange-900/10 transition hover:brightness-110"
                style={{ backgroundImage: ACCENT_ECOMMERCE.gradient }}
              >
                Quiero ServiceAgent
              </Link>
              <Link
                href="#conversacion"
                className="rounded-[14px] border-[1.25px] border-slate-200 bg-white px-6 py-3 text-sm font-semibold text-slate-900 transition hover:border-slate-300"
              >
                Míralo vender
              </Link>
            </div>
            <p className="mt-5 text-sm text-orange-900/70">Sin tarjeta. Sin contrato. Sin instalar nada.</p>
          </div>

          <div className="relative mx-auto flex justify-center">
            <FloatingChip emoji="👟" title="Nike Air Force 1" subtitle="$89.990" className="-top-2 -left-8 rotate-[-3deg]" />
            <FloatingChip emoji="🔌" title="Cargador 15W rápido" subtitle="$14.990" className="top-10 -right-10 rotate-[3deg]" />
            <FloatingChip
              emoji="🛍️"
              title="Venta cerrada"
              subtitle="$104.980 · hoy"
              tone="success"
              className="-bottom-4 -left-10 rotate-[-2deg]"
            />
            <FloatingChip emoji="🔋" title="Filtro Pulsar NS200" subtitle="$6.490" className="right-[-2.5rem] bottom-24" />
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
      </section>

      <RubrosStrip rubros={RUBROS} />

      <ComparisonSection
        title="Cada consulta sin responder es una venta que se va"
        subtitle="Mirá el antes y el después de tener tu catálogo respondiendo solo."
        problemas={[
          "Preguntas por WhatsApp que llegan de noche y se responden al otro día, cuando el cliente ya compró en otro lado.",
          "Buscar manualmente cada producto, precio y stock para cada consulta.",
          "Carritos armados a mano, copiando y pegando links uno por uno.",
          "Ventas que se pierden simplemente porque nadie contestó a tiempo.",
        ]}
        beneficios={[
          "Responde con el producto exacto, al instante, a cualquier hora.",
          "Conoce tu catálogo real: precio, stock y ficha, sin buscar nada a mano.",
          "Arma el carrito y genera el link de pago en la misma conversación.",
          "Cierra la venta mientras vos estás haciendo otra cosa.",
        ]}
        accent={ACCENT_ECOMMERCE}
      />

      <StepsSection
        title="Cómo vende por vos"
        subtitle="Sin técnicos, sin instalar nada. Conectás tu catálogo y se pone a trabajar."
        accent={ACCENT_ECOMMERCE}
        steps={[
          {
            icon: <SearchIcon width={20} height={20} />,
            title: "El cliente pregunta",
            description: "Por un producto, una marca o algo puntual que necesita. En lenguaje natural, como le hablaría a un vendedor.",
          },
          {
            icon: <PackageIcon width={20} height={20} />,
            title: "Busca en tu catálogo real",
            description: "Encuentra el producto, con precio, foto y stock actualizado. Nunca inventa algo que no tenés.",
          },
          {
            icon: <CardIcon width={20} height={20} />,
            title: "Arma el carrito y cobra",
            description: "Genera el link de pago y se lo manda al cliente. Vos recibís la plata, listo.",
          },
        ]}
      />

      <FeatureGrid
        title="Todo lo que hace por tus ventas"
        subtitle="No es un catálogo estático. Es un vendedor que busca, recomienda y cierra."
        features={[
          {
            icon: <ClockIcon width={20} height={20} />,
            title: "Vende 24/7",
            description: "El cliente compra a la hora que sea, también de madrugada. Tu catálogo nunca cierra.",
            pastel: "bg-orange-50 text-orange-600",
          },
          {
            icon: <SearchIcon width={20} height={20} />,
            title: "Encuentra lo que se pide",
            description: "Busca por nombre, marca o característica dentro de tu catálogo real, no adivina.",
            pastel: "bg-amber-50 text-amber-600",
          },
          {
            icon: <CheckShieldIcon width={20} height={20} />,
            title: "Nunca inventa precio o stock",
            description: "Cada respuesta se valida contra tu catálogo sincronizado. Si no hay stock, lo dice.",
            pastel: "bg-emerald-50 text-emerald-600",
          },
          {
            icon: <SparkleIcon width={20} height={20} />,
            title: "Recomienda productos",
            description: "Sugiere algo que combine con lo que el cliente ya eligió, como lo haría un vendedor del local.",
            pastel: "bg-pink-50 text-pink-600",
          },
          {
            icon: <CardIcon width={20} height={20} />,
            title: "Cierra la venta en el chat",
            description: "Arma el carrito y genera el link de pago sin que el cliente salga de WhatsApp.",
            pastel: "bg-blue-50 text-blue-600",
          },
          {
            icon: <HandoffIcon width={20} height={20} />,
            title: "Te pasa lo que importa",
            description: "Reclamos, negociación de precio o dudas puntuales se derivan a una persona de tu equipo.",
            pastel: "bg-violet-50 text-violet-600",
          },
        ]}
      />

      <div id="conversacion">
        <ConversationSection
          eyebrow="Conversación real"
          title="Encuentra el producto exacto y cierra la venta"
          description="El cliente pregunta por algo puntual y el bot responde con la ficha real: foto, precio y link de compra, listo para pagar."
          checklist={[
            "Busca en tu catálogo real, nunca inventa productos.",
            "Manda ficha con foto, precio y link de compra.",
            "Arma el carrito con todo lo que el cliente pida.",
          ]}
          negocio="Moto Parts"
          reverse
          mensajes={[
            { de: "cliente", texto: "necesito un cargador rápido para moto", hora: "11:02" },
            {
              de: "bot",
              texto: "Tengo este que se vende harto para eso 🔌",
              hora: "11:02",
              producto: {
                emoji: "🔌",
                nombre: "Cargador USB 15W a prueba de agua",
                subtitulo: "Instalación en el manubrio",
                precio: "$14.990",
                dominio: "MOTOPARTS.CL",
              },
            },
            { de: "cliente", texto: "dale, lo llevo", hora: "11:03" },
            {
              de: "bot",
              texto: "¡Genial! 🙌 Te dejo el link de pago, apenas confirmes te lo despachamos.",
              hora: "11:03",
            },
          ]}
        />
      </div>

      <section className="mx-auto max-w-6xl px-6 py-20">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="font-heading text-[32px] font-semibold text-slate-900 sm:text-[44px] sm:leading-[55px]">
            Se ajusta a tu catálogo
          </h2>
          <p className="mt-4 text-slate-500">Como cada tienda tiene un volumen distinto de productos, este plan se cotiza a medida.</p>
        </div>

        <div className="mx-auto mt-12 max-w-lg rounded-3xl border-2 p-10 text-center shadow-xl" style={{ borderColor: ACCENT_ECOMMERCE.from }}>
          <p className="text-sm font-medium text-slate-500">Ecommerce</p>
          <p className="mt-2 font-heading text-4xl font-semibold text-slate-900">A tu medida</p>
          <p className="mt-3 text-sm text-slate-500">Se ajusta a tu catálogo y volumen de ventas.</p>
          <ul className="mt-6 space-y-3 text-left">
            {ECOMMERCE_FEATURES.map((feature) => (
              <li key={feature} className="flex items-start gap-2.5 text-sm text-slate-700">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0" aria-hidden="true">
                  <path
                    d="M5 12.5l4.5 4.5L19 7"
                    stroke={ACCENT_ECOMMERCE.from}
                    strokeWidth="2.2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                <span>{feature}</span>
              </li>
            ))}
          </ul>
          <Link
            href="/precios"
            className="mt-8 block rounded-[14px] px-6 py-3 text-sm font-semibold text-white transition hover:brightness-110"
            style={{ backgroundImage: ACCENT_ECOMMERCE.gradient }}
          >
            Cotizar
          </Link>
        </div>

        <p className="mt-6 text-center text-sm text-slate-500">
          ¿Solo necesitás agendamiento?{" "}
          <Link href="/agendamiento" className="font-medium text-orange-600 underline underline-offset-2">
            Mirá los planes Básico y Pro →
          </Link>
        </p>
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
        title="Convertí tu catálogo en un vendedor 24/7"
        subtitle="Mientras leés esto, alguien podría estar preguntando por un producto tuyo. Empecemos hoy."
        accent={ACCENT_ECOMMERCE}
        secondaryLabel="Ver precios"
        secondaryHref="/precios"
      />
    </>
  );
}
