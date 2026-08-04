import PhotoSlot from "@/components/PhotoSlot";
import Reveal from "@/components/Reveal";

export interface Feature {
  title: string;
  description: string;
  photoLabel: string;
  /** Ruta en /public de la foto real (ej: "/images/features/omnicanal-real.jpg"). Si no viene, se muestra el placeholder con photoLabel. */
  photoSrc?: string;
  /** Card grande (2 unidades) — solo la primera del arreglo debería usar esto. */
  large?: boolean;
  /** Pills tipo "● WhatsApp" / "● Instagram" que solo se muestran en la card grande. */
  badges?: { label: string; color: "green" | "violet" }[];
}

/** Reparte la animación de entrada: izquierda / abajo (centro) / derecha, cíclico cada 3 cards. */
function direccionPorPosicion(i: number) {
  const resto = i % 3;
  return resto === 0 ? "izquierda" : resto === 1 ? "abajo" : "derecha";
}

interface FeatureGridProps {
  eyebrow: string;
  eyebrowColor?: "green" | "violet";
  title: string;
  subtitle?: string;
  features: Feature[];
  /** Gradiente de fondo de la card grande. Por defecto verde→violeta. */
  largeBg?: string;
}

export default function FeatureGrid({
  eyebrow,
  eyebrowColor = "green",
  title,
  subtitle,
  features,
  largeBg = "linear-gradient(135deg,rgba(185,134,47,0.07),rgba(201,120,143,0.06))",
}: FeatureGridProps) {
  return (
    <section id="features" className="mx-auto max-w-[1240px] px-5 py-[clamp(72px,9vw,110px)] sm:px-10">
      <div className="mx-auto max-w-[640px] text-center">
        <span className={`text-[12.5px] font-bold tracking-[0.1em] ${eyebrowColor === "green" ? "text-green-light" : "text-violet-light"}`}>
          {eyebrow}
        </span>
        <h2 className="mt-3.5 text-[clamp(28px,3.6vw,42px)] leading-[1.15] font-extrabold tracking-[-0.025em] text-ink">
          {title}
        </h2>
        {subtitle && <p className="mt-3.5 text-base leading-relaxed text-ink/55">{subtitle}</p>}
      </div>

      <div className="mt-12 flex flex-wrap items-start gap-[18px]">
        {features.map((feature, i) =>
          feature.large ? (
            <Reveal key={feature.title} direccion={direccionPorPosicion(i)} className="min-w-[300px] flex-[2_1_480px]">
              <div className="overflow-hidden rounded-[20px] border border-ink/10">
                <PhotoSlot label={feature.photoLabel} src={feature.photoSrc} height={220} rounded={false} />
                <div style={{ backgroundImage: largeBg }} className="p-6">
                  <div className="flex flex-wrap items-center justify-between gap-6">
                    <div className="max-w-[340px]">
                      <h3 className="text-[19px] font-bold text-ink">{feature.title}</h3>
                      <p className="mt-2 text-[14.5px] leading-relaxed text-ink/60">{feature.description}</p>
                    </div>
                    {feature.badges && (
                      <div className="flex gap-2.5">
                        {feature.badges.map((badge) => (
                          <span
                            key={badge.label}
                            className={`flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-semibold ${
                              badge.color === "green"
                                ? "border-green/30 bg-green/10 text-green-light"
                                : "border-violet-light/30 bg-violet/10 text-violet-light"
                            }`}
                          >
                            ● {badge.label}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </Reveal>
          ) : (
            <Reveal key={feature.title} direccion={direccionPorPosicion(i)} delay={80} className="min-w-[230px] flex-[1_1_260px]">
              <div className="overflow-hidden rounded-[20px] border border-ink/10 bg-card">
                <PhotoSlot label={feature.photoLabel} src={feature.photoSrc} height={180} rounded={false} />
                <div className="p-6">
                  <h3 className="text-[17px] font-bold text-ink">{feature.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-ink/60">{feature.description}</p>
                </div>
              </div>
            </Reveal>
          ),
        )}
      </div>
    </section>
  );
}
