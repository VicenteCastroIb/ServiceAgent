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
import { ACCENT_DUO_VIOLET, ACCENT_VIOLET, TEXT_GRADIENT } from "@/lib/accents";

export const metadata: Metadata = {
  title: "Catálogo vendiendo por WhatsApp con IA — ServiceAgent",
  description: "El asistente que conoce tu catálogo, recomienda productos y cierra la venta dentro del chat.",
};

const RUBROS = ["Tiendas de ropa", "Repuestos", "Tecnología", "Ferreterías", "Regalos", "Decoración"];

const ECOMMERCE_FEATURES = [
  "Todo lo del plan Pro",
  "Catálogo sincronizado con tu tienda",
  "El bot busca, recomienda y lleva al pago",
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
      "El link de pago se genera con tu propia pasarela (Transbank, Flow u otra que definas). El dinero llega directo a tu cuenta, nosotros no lo intermediamos.",
  },
  {
    pregunta: "¿Puedo usarlo sin tienda online?",
    respuesta: "Si todavía no tienes una tienda online, puedes cargar el catálogo manualmente desde el panel. Funciona igual, solo que sin sincronización automática.",
  },
];

export default function EcommercePage() {
  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="bg-grid-fade absolute inset-0" />
        <div className="relative mx-auto flex max-w-[1240px] flex-wrap items-center gap-12 px-5 py-[clamp(56px,8vw,88px)] sm:px-10">
          <div className="min-w-[300px] flex-[1.1_1_400px]">
            <span className="inline-flex items-center gap-2 rounded-full border border-violet-mid/35 bg-violet/8 px-3.5 py-1.5 text-xs font-semibold tracking-[0.04em] text-violet-light">
              TU CATÁLOGO VENDIENDO POR WHATSAPP CON IA
            </span>
            <h1 className="mt-[22px] text-[clamp(32px,4.2vw,50px)] leading-[1.1] font-extrabold tracking-[-0.03em] text-ink">
              Convierte cada{" "}
              <span className="bg-clip-text text-transparent" style={{ backgroundImage: TEXT_GRADIENT }}>
                &ldquo;¿tienen esto?&rdquo;
              </span>{" "}
              en una venta.
            </h1>
            <p className="mt-[22px] max-w-[460px] text-base leading-relaxed text-ink/65">
              El asistente con IA que conoce tu catálogo, recomienda productos y envía la ficha con precio y
              link de compra. Cierra ventas las 24 horas, también cuando tú no puedes.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/registro"
                className="rounded-[10px] px-[26px] py-[13px] text-[15px] font-semibold text-white shadow-[0_10px_28px_rgba(201,120,143,0.3)] transition hover:brightness-110"
                style={{ backgroundImage: ACCENT_VIOLET.gradient }}
              >
                Empieza gratis
              </Link>
              <Link
                href="#conversacion"
                className="rounded-[10px] border border-ink/15 px-[26px] py-[13px] text-[15px] font-semibold text-ink transition hover:border-ink/30"
              >
                Verlo vender
              </Link>
            </div>
            <p className="mt-5 text-[13px] text-ink/45">Sin tarjeta &nbsp;·&nbsp; Sin contrato &nbsp;·&nbsp; Sin instalar nada</p>
          </div>

          <div className="relative flex min-w-[300px] flex-[0.9_1_340px] justify-center" style={{ height: 420 }}>
            <div
              aria-hidden
              className="absolute top-[40%] left-1/2 h-[300px] w-[300px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-violet/14 blur-[100px]"
            />
            <ChatCard
              negocio="Tienda Roman"
              canal="instagram"
              className="relative self-center"
              mensajes={[
                { de: "cliente", texto: "hola, tienen un par de poleras y un buzo?" },
                {
                  de: "bot",
                  texto: "¡Hola! Sí, te dejo lo que tengo disponible 👇",
                  productos: [
                    { nombre: "Polera básica algodón · talla M", precio: "$9.990" },
                    { nombre: "Polera básica algodón · talla L", precio: "$9.990" },
                    { nombre: "Buzo canguro frisado · talla M", precio: "$22.990" },
                  ],
                },
                { de: "cliente", texto: "me llevo las dos poleras y el buzo talla M" },
                { de: "bot", texto: "¡Genial! 🙌 Total $42.970. Te lo despacho mañana entre 10:00 y 13:00, ¿confirmamos la dirección de siempre?" },
              ]}
            />
          </div>
        </div>
      </section>

      <RubrosStrip label="Pensado para negocios con catálogo:" rubros={RUBROS} />

      <ComparisonSection
        title="Cada consulta sin responder es una venta que se va"
        subtitle="Mira el antes y el después de tener tu catálogo respondiendo solo."
        problemas={[
          "Preguntas que llegan de noche y se responden al otro día, cuando el cliente ya compró en otro lado.",
          "Buscar manualmente cada producto, precio y stock para cada consulta.",
          "Carritos armados a mano, copiando y pegando links uno por uno.",
          "Ventas que se pierden porque nadie contestó a tiempo.",
        ]}
        beneficios={[
          "Responde con el producto exacto, al instante, a cualquier hora.",
          "Conoce tu catálogo real: precio, stock y ficha, sin buscar nada a mano.",
          "Arma el carrito y genera el link de pago en la misma conversación.",
          "Cierra la venta mientras tú haces otra cosa.",
        ]}
        accent={ACCENT_DUO_VIOLET}
      />

      <StepsSection
        eyebrow="CÓMO VENDE POR TI"
        eyebrowColor="violet"
        title="Sin técnicos, sin instalar nada"
        subtitle="Conectas tu catálogo y se pone a trabajar."
        steps={[
          {
            numero: 1,
            titulo: "El cliente pregunta",
            descripcion: "Por un producto, una marca o algo puntual que necesita. En lenguaje natural, como le hablaría a un vendedor.",
            photoLabel: "Foto: cliente escribiendo por Instagram desde su celular",
            color: "violet",
          },
          {
            numero: 2,
            titulo: "Busca en tu catálogo real",
            descripcion: "Encuentra el producto, con precio, foto y stock actualizado. Nunca inventa algo que no tienes.",
            photoLabel: "Foto: productos del catálogo ordenados sobre un mostrador",
            color: "green",
          },
          {
            numero: 3,
            titulo: "Arma el carrito y cobra",
            descripcion: "Genera el link de pago y se lo envía al cliente. Tú recibes el dinero, listo.",
            photoLabel: "Foto: celular mostrando un link de pago confirmado",
            color: "violet",
          },
        ]}
      />

      <FeatureGrid
        eyebrow="FUNCIONES"
        eyebrowColor="violet"
        title="Todo lo que hace por tus ventas"
        subtitle="No es un catálogo estático. Es un vendedor que busca, recomienda y cierra."
        largeBg="linear-gradient(135deg,rgba(201,120,143,0.07),rgba(185,134,47,0.06))"
        features={[
          {
            title: "Vende 24/7",
            description: "El cliente compra a la hora que sea, también de madrugada. Tu catálogo nunca cierra.",
            photoLabel: "Foto: tienda con celular activo respondiendo pedidos de madrugada",
            large: true,
          },
          {
            title: "Encuentra lo que se pide",
            description: "Busca por nombre, marca o característica dentro de tu catálogo real, no adivina.",
            photoLabel: "Foto: repisa de productos ordenados por categoría",
          },
          {
            title: "Nunca inventa precio o stock",
            description: "Cada respuesta se valida contra tu catálogo sincronizado. Si no hay stock, lo dice.",
            photoLabel: "Foto: etiqueta de precio y stock sobre un producto real",
          },
          {
            title: "Recomienda productos",
            description: "Sugiere algo que combine con lo que el cliente ya eligió, como lo haría un vendedor del local.",
            photoLabel: "Foto: dos productos combinados, tipo outfit o combo",
          },
          {
            title: "Cierra la venta en el chat",
            description: "Arma el carrito y genera el link de pago sin que el cliente salga de WhatsApp.",
            photoLabel: "Foto: celular mostrando un carrito de compra en el chat",
          },
        ]}
      />

      {/* Plan a medida */}
      <section className="border-t border-ink/10 bg-cream-alt">
        <div className="mx-auto max-w-[1240px] px-5 py-[clamp(72px,9vw,110px)] sm:px-10">
          <div className="mx-auto max-w-[560px] text-center">
            <h2 className="text-[clamp(28px,3.6vw,42px)] leading-[1.15] font-extrabold tracking-[-0.025em] text-ink">
              Se ajusta a tu catálogo
            </h2>
            <p className="mt-3.5 text-base leading-relaxed text-ink/55">
              Como cada tienda tiene un volumen distinto de productos, este plan se cotiza a medida.
            </p>
          </div>
          <div
            className="mx-auto mt-10 max-w-[460px] rounded-[22px] border-[1.5px] border-violet-mid/40 bg-card p-9 text-center"
            style={{ backgroundImage: "linear-gradient(180deg,rgba(201,120,143,0.10),rgba(185,134,47,0.05))" }}
          >
            <p className="text-sm font-semibold text-ink/65">Ecommerce</p>
            <p className="mt-3.5 text-[32px] font-extrabold text-ink">A tu medida</p>
            <p className="mt-2 text-sm text-ink/55">Se ajusta a tu catálogo y volumen de ventas.</p>
            <ul className="mt-6 flex flex-col gap-3 text-left">
              {ECOMMERCE_FEATURES.map((feature) => (
                <li key={feature} className="flex items-start gap-2.5 text-[13.5px] text-ink/80">
                  <span className="shrink-0 text-green-light">✓</span>
                  <span>{feature}</span>
                </li>
              ))}
            </ul>
            <Link
              href="/registro"
              className="mt-7 block rounded-xl px-6 py-3.5 text-center text-[14.5px] font-bold text-white transition hover:brightness-110"
              style={{ backgroundImage: ACCENT_VIOLET.gradient }}
            >
              Cotizar
            </Link>
          </div>
          <p className="mt-6 text-center text-sm text-ink/55">
            ¿Solo necesitas agendamiento?{" "}
            <Link href="/agendamiento" className="font-medium text-violet-light underline underline-offset-2">
              Mira los planes Básico y Pro
            </Link>
          </p>
        </div>
      </section>

      <div id="conversacion">
        <ConversationSection
          eyebrow="CONVERSACIÓN REAL"
          eyebrowColor="green"
          title="Encuentra el producto exacto y cierra la venta"
          description="El cliente pregunta por algo puntual y el bot responde con la ficha real: nombre, precio y link de compra, listo para pagar."
          checklist={[
            "Busca en tu catálogo real, nunca inventa productos.",
            "Responde con nombre, precio y link de compra.",
            "Arma el carrito con todo lo que el cliente pida.",
          ]}
          negocio="Tienda Roman"
          canal="instagram"
          reverse
          mensajes={[
            { de: "cliente", texto: "necesito un buzo talla L para regalo" },
            {
              de: "bot",
              texto: "Tengo este que se vende harto para eso 👕",
              ficha: { titulo: "Buzo canguro frisado · talla L", subtitulo: "Algodón peinado · color gris", precio: "$22.990" },
            },
            { de: "cliente", texto: "perfecto, lo llevo en gris" },
            { de: "bot", texto: "¡Genial! 🙌 Te dejo el link de pago y coordinamos el despacho para el viernes." },
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
          <FAQAccordion items={FAQ_ITEMS} accentColor="violet" />
        </div>
      </section>

      <CTABanner
        title="Convierte tu catálogo en un vendedor 24/7"
        subtitle="Mientras lees esto, alguien podría estar preguntando por un producto tuyo. Empecemos hoy."
        accent={ACCENT_DUO_VIOLET}
        primaryLabel="Empieza gratis"
        primaryHref="/registro"
      />
    </>
  );
}
