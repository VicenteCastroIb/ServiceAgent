import ChatCard, { type ChatMensaje } from "@/components/ChatCard";
import Reveal from "@/components/Reveal";

interface ConversationSectionProps {
  eyebrow: string;
  eyebrowColor?: "green" | "violet";
  title: string;
  description: string;
  checklist: string[];
  negocio: string;
  canal: "whatsapp" | "instagram";
  mensajes: ChatMensaje[];
  reverse?: boolean;
  /** Si la sección necesita fondo alterno (algunas "conversación real" lo llevan, otras no). */
  tinted?: boolean;
}

export default function ConversationSection({
  eyebrow,
  eyebrowColor = "green",
  title,
  description,
  checklist,
  negocio,
  canal,
  mensajes,
  reverse = false,
  tinted = false,
}: ConversationSectionProps) {
  return (
    <section className={tinted ? "border-t border-ink/10 bg-cream-alt" : undefined}>
      <div
        className={`mx-auto grid max-w-[1240px] grid-cols-[repeat(auto-fit,minmax(320px,1fr))] items-center gap-12 px-5 py-[clamp(72px,9vw,110px)] sm:px-10`}
      >
        <Reveal direccion={reverse ? "derecha" : "izquierda"} className={reverse ? "order-2" : "order-1"}>
          <ChatCard negocio={negocio} canal={canal} mensajes={mensajes} className="mx-auto" />
        </Reveal>
        <Reveal direccion={reverse ? "izquierda" : "derecha"} delay={100} className={reverse ? "order-1" : "order-2"}>
          <span className={`text-[12.5px] font-bold tracking-[0.1em] ${eyebrowColor === "green" ? "text-green-light" : "text-violet-light"}`}>
            {eyebrow}
          </span>
          <h2 className="mt-3.5 text-[clamp(26px,3.2vw,36px)] leading-[1.2] font-extrabold tracking-[-0.02em] text-ink">
            {title}
          </h2>
          <p className="mt-3.5 text-[15.5px] leading-relaxed text-ink/60">{description}</p>
          <ul className="mt-[22px] flex flex-col gap-3">
            {checklist.map((item) => (
              <li key={item} className="flex items-start gap-2.5 text-sm text-ink/75">
                <span className="text-green-light">✓</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </Reveal>
      </div>
    </section>
  );
}
