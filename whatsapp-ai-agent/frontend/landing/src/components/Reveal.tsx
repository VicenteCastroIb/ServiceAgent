"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";

type Direccion = "izquierda" | "derecha" | "abajo";

const DESDE: Record<Direccion, string> = {
  izquierda: "-translate-x-12",
  derecha: "translate-x-12",
  abajo: "translate-y-8",
};

/**
 * Envuelve contenido y lo anima al entrar en viewport (fade + slide desde un
 * costado), vía IntersectionObserver. Se anima una sola vez (no se re-oculta
 * al volver a scrollear). Respeta prefers-reduced-motion.
 */
export default function Reveal({
  children,
  direccion = "abajo",
  delay = 0,
  className = "",
}: {
  children: ReactNode;
  direccion?: Direccion;
  delay?: number;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true);
          observer.disconnect();
        }
      },
      { threshold: 0.15, rootMargin: "0px 0px -80px 0px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={ref}
      className={`transition-all duration-700 ease-out motion-reduce:transition-none motion-reduce:transform-none ${
        visible ? "translate-x-0 translate-y-0 opacity-100" : `opacity-0 ${DESDE[direccion]}`
      } ${className}`}
      style={{ transitionDelay: visible ? `${delay}ms` : "0ms" }}
    >
      {children}
    </div>
  );
}
