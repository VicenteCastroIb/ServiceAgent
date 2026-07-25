export interface Accent {
  name: string;
  /** CSS gradient string, listo para usar en style={{ backgroundImage: accent.gradient }} */
  gradient: string;
  from: string;
  to: string;
}

/** Botón/badges sólidos en dorado (antes verde/WhatsApp) — usado en Home y Agendamiento. */
export const ACCENT_GREEN: Accent = {
  name: "gold",
  gradient: "linear-gradient(90deg,#b9862f,#8a5f22)",
  from: "#b9862f",
  to: "#8a5f22",
};

/** Botón/badges sólidos en rosa pastel (antes violeta/Instagram) — usado en Ecommerce. */
export const ACCENT_VIOLET: Accent = {
  name: "pink",
  gradient: "linear-gradient(90deg,#c9788f,#8a4a5a)",
  from: "#c9788f",
  to: "#8a4a5a",
};

/** Dúo dorado→rosa para tarjetas "Con ServiceAgent" y banners CTA (Home, Agendamiento). */
export const ACCENT_DUO_GREEN: Accent = {
  name: "duo-gold",
  gradient: "linear-gradient(120deg,#b9862f,#c9788f)",
  from: "#b9862f",
  to: "#c9788f",
};

/** Dúo rosa→dorado para tarjetas "Con ServiceAgent" y banners CTA (Ecommerce). */
export const ACCENT_DUO_VIOLET: Accent = {
  name: "duo-pink",
  gradient: "linear-gradient(120deg,#c9788f,#b9862f)",
  from: "#c9788f",
  to: "#b9862f",
};

/** Gradiente de texto (headline hero) — dorado a rosa pastel. */
export const TEXT_GRADIENT = "linear-gradient(90deg,#b9862f,#c9788f)";

// Mantenidos por compatibilidad con código existente que aún no migró.
export const ACCENT_HOME = ACCENT_DUO_GREEN;
export const ACCENT_AGENDAMIENTO = ACCENT_GREEN;
export const ACCENT_ECOMMERCE = ACCENT_VIOLET;
