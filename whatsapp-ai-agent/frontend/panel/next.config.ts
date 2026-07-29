import type { NextConfig } from "next";

// Backend, para connect-src del CSP de abajo. Misma env var que ya usa
// lib/api.ts - se reusa acá en build time (next.config.ts corre en Node).
const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * El panel es la superficie más sensible del proyecto (credenciales de
 * tenants, catálogo, agenda, cobros) - CSP más estricta que la de la
 * landing: nada de scripts/estilos de terceros, sin analytics. style-src
 * necesita 'unsafe-inline' porque Next.js inyecta estilos inline para el CSS
 * crítico de cada página (no hay forma de evitarlo sin un setup de nonces
 * más elaborado). frame-ancestors 'none' + X-Frame-Options DENY: el panel no
 * tiene ningún caso de uso legítimo para ser embebido en un iframe
 * (clickjacking sobre acciones como borrar un tenant o cerrar sesión).
 *
 * script-src también necesita 'unsafe-inline': el App Router de Next.js
 * inyecta el payload de hidratación (RSC data) en <script> inline sin src
 * (self.__next_f.push(...)) en CADA página - sin 'unsafe-inline' el browser
 * los bloquea, la hidratación nunca recibe datos y la página queda en blanco
 * para siempre sin ni siquiera loguear un error claro (encontrado en
 * producción: /login quedaba completamente vacío, 0 errores de consola,
 * window.__next_f.length=0 confirmó que los scripts inline nunca corrieron).
 * La alternativa correcta para evitar 'unsafe-inline' es CSP con nonce vía
 * middleware.ts (ver docs de Next.js) - vale la pena migrar a eso más
 * adelante dado que el panel es la superficie más sensible, pero por ahora
 * se prioriza restaurar el login. No hay XSS-por-inline-script nuevo acá:
 * el panel nunca interpola contenido de usuario en un <script>, solo React
 * renderiza vía JSX (auto-escapado) o el propio payload RSC de Next.js.
 */
const csp = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data:",
  `connect-src 'self' ${API_URL}`,
  "font-src 'self' data:",
  "object-src 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "frame-ancestors 'none'",
  "upgrade-insecure-requests",
].join("; ");

const securityHeaders = [
  { key: "Content-Security-Policy", value: csp },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
  { key: "Strict-Transport-Security", value: "max-age=63072000; includeSubDomains; preload" },
];

const nextConfig: NextConfig = {
  // Standalone: junta solo lo necesario para correr (server.js + deps mínimas)
  // en .next/standalone, para una imagen Docker liviana - ver Dockerfile.
  output: "standalone",
  async headers() {
    return [
      {
        source: "/:path*",
        headers: securityHeaders,
      },
    ];
  },
};

export default nextConfig;
