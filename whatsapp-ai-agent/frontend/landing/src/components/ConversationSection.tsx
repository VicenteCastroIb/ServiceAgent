import WhatsAppMockup, { type MensajeChat } from "@/components/WhatsAppMockup";

interface ConversationSectionProps {
  eyebrow: string;
  title: string;
  description: string;
  checklist: string[];
  negocio: string;
  mensajes: MensajeChat[];
  reverse?: boolean;
}

export default function ConversationSection({
  eyebrow,
  title,
  description,
  checklist,
  negocio,
  mensajes,
  reverse = false,
}: ConversationSectionProps) {
  return (
    <section className="mx-auto max-w-6xl px-6 py-20">
      <div className={`flex flex-col items-center gap-12 lg:flex-row ${reverse ? "lg:flex-row-reverse" : ""}`}>
        <div className="flex justify-center lg:w-1/2">
          <WhatsAppMockup negocio={negocio} mensajes={mensajes} />
        </div>
        <div className="lg:w-1/2">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
            {eyebrow}
          </span>
          <h2 className="mt-4 font-heading text-[28px] leading-tight font-semibold text-slate-900 sm:text-[34px]">
            {title}
          </h2>
          <p className="mt-4 text-slate-500">{description}</p>
          <ul className="mt-5 space-y-3">
            {checklist.map((item) => (
              <li key={item} className="flex items-start gap-2.5 text-sm text-slate-700">
                <CheckDot />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

function CheckDot() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" className="mt-0.5 shrink-0 text-emerald-500" aria-hidden="true">
      <circle cx="12" cy="12" r="10" fill="currentColor" fillOpacity="0.12" />
      <path d="M8 12.5l2.5 2.5L16 9.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
