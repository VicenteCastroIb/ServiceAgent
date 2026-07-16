"use client";

import { useEffect, useRef, useState } from "react";
import type { MensajeChat } from "@/components/WhatsAppMockup";

const PAUSA_ANTES_DE_CLIENTE_MS = 650;
const PAUSA_ESCRIBIENDO_MS = 1100;
const PAUSA_ANTES_DE_REINICIAR_MS = 2600;

/**
 * Revela los mensajes de un WhatsAppMockup de a uno, simulando una
 * conversación en tiempo real: antes de cada mensaje del bot muestra el
 * indicador "escribiendo...", y al terminar la lista hace una pausa y
 * reinicia (loop ambiental, pensado para mockups de marketing en la
 * landing).
 *
 * - Arranca recién cuando el mockup entra en pantalla (IntersectionObserver),
 *   para no gastar la animación mientras el usuario todavía no llegó a esa
 *   sección.
 * - Respeta prefers-reduced-motion: si está activado, muestra todo de una
 *   sin animar nada.
 * - Si animado=false (default), es un pass-through sin timers ni observer -
 *   cero cambio de comportamiento para los usos que no lo pidan.
 */
export function useReproduccionChat(mensajes: MensajeChat[], animado: boolean) {
  const contenedorRef = useRef<HTMLDivElement | null>(null);
  const [visibleCount, setVisibleCount] = useState(animado ? 0 : mensajes.length);
  const [escribiendo, setEscribiendo] = useState(false);

  useEffect(() => {
    if (!animado) return;

    const prefiereMenosMovimiento =
      typeof window !== "undefined" && window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    if (prefiereMenosMovimiento) {
      setVisibleCount(mensajes.length);
      return;
    }

    const nodo = contenedorRef.current;
    if (!nodo) return;

    let timers: ReturnType<typeof setTimeout>[] = [];
    let cancelado = false;

    function limpiarTimers() {
      timers.forEach(clearTimeout);
      timers = [];
    }

    function reproducirDesde(indice: number) {
      if (cancelado) return;
      if (indice >= mensajes.length) {
        timers.push(
          setTimeout(() => {
            if (cancelado) return;
            setVisibleCount(0);
            setEscribiendo(false);
            reproducirDesde(0);
          }, PAUSA_ANTES_DE_REINICIAR_MS)
        );
        return;
      }

      const mensaje = mensajes[indice];
      const esBot = mensaje.de === "bot";
      const pausa = esBot ? PAUSA_ESCRIBIENDO_MS : PAUSA_ANTES_DE_CLIENTE_MS;

      if (esBot) setEscribiendo(true);

      timers.push(
        setTimeout(() => {
          if (cancelado) return;
          setEscribiendo(false);
          setVisibleCount(indice + 1);
          reproducirDesde(indice + 1);
        }, pausa)
      );
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) {
          observer.disconnect();
          reproducirDesde(0);
        }
      },
      { threshold: 0.4 }
    );
    observer.observe(nodo);

    return () => {
      cancelado = true;
      limpiarTimers();
      observer.disconnect();
    };
    // mensajes es estático en todos los usos actuales (arrays definidos afuera del render) - no hace falta re-suscribir por identidad de array.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [animado]);

  return {
    contenedorRef,
    mensajesVisibles: animado ? mensajes.slice(0, visibleCount) : mensajes,
    escribiendo: animado ? escribiendo : undefined,
  };
}
