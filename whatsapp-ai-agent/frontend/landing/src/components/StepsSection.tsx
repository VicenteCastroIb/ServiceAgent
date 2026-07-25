import PhotoSlot from "@/components/PhotoSlot";

export interface Step {
  numero: number;
  titulo: string;
  descripcion: string;
  photoLabel: string;
  color: "green" | "violet";
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
          {steps.map((step) => (
            <div key={step.numero} className="rounded-[20px] border border-ink/10 bg-card p-[22px]">
              <PhotoSlot label={step.photoLabel} height={150} className="mb-[18px]" />
              <span
                className={`flex h-11 w-11 items-center justify-center rounded-xl border text-[17px] font-extrabold ${
                  step.color === "green"
                    ? "border-green/30 bg-green/10 text-green-light"
                    : "border-violet-mid/35 bg-violet/10 text-violet-light"
                }`}
              >
                {step.numero}
              </span>
              <h3 className="mt-[18px] text-lg font-bold text-ink">{step.titulo}</h3>
              <p className="mt-2 text-[14.5px] leading-relaxed text-ink/60">{step.descripcion}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
