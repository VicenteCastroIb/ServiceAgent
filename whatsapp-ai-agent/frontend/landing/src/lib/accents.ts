export interface Accent {
  name: string;
  /** CSS gradient string, listo para usar en style={{ backgroundImage: accent.gradient }} */
  gradient: string;
  from: string;
  to: string;
}

export const ACCENT_HOME: Accent = {
  name: "home",
  gradient: "linear-gradient(100deg, #059669, #16A34A)",
  from: "#059669",
  to: "#16A34A",
};

export const ACCENT_AGENDAMIENTO: Accent = {
  name: "agendamiento",
  gradient: "linear-gradient(100deg, #0D9488, #16A34A)",
  from: "#0D9488",
  to: "#16A34A",
};

export const ACCENT_ECOMMERCE: Accent = {
  name: "ecommerce",
  gradient: "linear-gradient(100deg, #EA580C, #D97706)",
  from: "#EA580C",
  to: "#D97706",
};
