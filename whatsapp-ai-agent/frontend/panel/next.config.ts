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
 */
const csp = [
  "default-src 'self'",
  "script-src 'self'",
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
