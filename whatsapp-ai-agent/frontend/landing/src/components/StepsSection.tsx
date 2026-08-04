import PhotoSlot from "@/components/PhotoSlot";
import Reveal from "@/components/Reveal";

export interface Step {
  numero: number;
  titulo: string;
  descripcion: string;
  photoLabel: string;
  /** Ruta en /public de la foto real. Si no viene, se muestra el placeholder con photoLabel. */
  photoSrc?: string;
  color: "green" | "violet";
}

/** Reparte la animación de entrada: izquierda / abajo (centro) / derecha, cíclico cada 3 cards. */
function direccionPorPosicion(i: number) {
  const resto = i % 3;
  return resto === 0 ? "izquierda" : resto === 1 ? "abajo" : "derecha";
}

interface StepsSectionProps {
  eyebrow: string;
  eyebrowColor?: "green" | "violet";
  title: string;
  subtitle?: string;
  steps: Step[];
}

export default function StepsSection({ eyebrow, eyebrowColor = "violet", title, subtitle, steps }: StepsSectionProps) {
  return (
    <section className="border-t border-ink/10 bg-cream-alt">
      <div className="mx-auto max-w-[1240px] px-5 py-[clamp(72px,9vw,110px)] sm:px-10">
        <div className="mx-auto max-w-[640px] text-center">
          <span className={`text-[12.5px] font-bold tracking-[0.1em] ${eyebrowColor === "green" ? "text-green-light" : "text-violet-light"}`}>
            {eyebrow}
          </span>
          <h2 className="mt-3.5 text-[clamp(28px,3.6vw,42px)] leading-[1.15] font-extrabold tracking-[-0.025em] text-ink">
            {title}
          </h2>
          {subtitle && <p className="mt-3.5 text-base leading-relaxed text-ink/55">{subtitle}</p>}
        </div>

        <div className="mt-[52px] grid grid-cols-[repeat(auto-fit,minmax(260px,1fr))] gap-7">
          {steps.map((step, i) => (
            <Reveal key={step.numero} direccion={direccionPorPosicion(i)} delay={i * 60}>
              <div className="overflow-hidden rounded-[20px] border border-ink/10 bg-card">
                <PhotoSlot label={step.photoLabel} src={step.photoSrc} height={240} rounded={false} />
                <div className="p-[22px]">
                  <h3 className="text-lg font-bold text-ink">{step.titulo}</h3>
                  <p className="mt-2 text-[14.5px] leading-relaxed text-ink/60">{step.descripcion}</p>
                </div>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
