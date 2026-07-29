import type { NextConfig } from "next";

// Backend, para connect-src/CSP de abajo. Misma env var que ya usa el resto
// del sitio (lib/api.ts) - no se agrega nada nuevo acá, solo se reusa en
// build time (next.config.ts corre en Node, no en browser). El panel no
// entra en connect-src: Header.tsx solo enlaza a /login con un <a>, no lo
// llama por fetch desde acá.
const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * CSP pensada para lo que esta landing realmente carga: scripts propios +
 * GA4/Meta Pixel (Analytics.tsx, ambos opcionales vía env var) vía <Script
 * strategy="afterInteractive">. 'unsafe-inline' en script-src es necesario
 * porque GA4/Meta Pixel se inicializan con snippets inline (patrón estándar
 * de ambos proveedores) - no hay XSS-por-inline-script real acá porque no se
 * interpola contenido de usuario en esos snippets, solo IDs de env vars
 * propias. object-src/base-uri/frame-ancestors sí quedan estrictos: no hay
 * ningún caso de uso legítimo para objetos embebidos ni para que este sitio
 * se embeba en un iframe ajeno (clickjacking).
 */
const csp = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline' https://www.googletagmanager.com https://connect.facebook.net",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: https://www.facebook.com https://www.google-analytics.com",
  `connect-src 'self' ${API_URL} https://www.google-analytics.com https://region1.google-analytics.com https://www.facebook.com`,
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
  // en .next/standalone, para una imagen Docker liviana - mismo patrón que frontend/panel.
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
