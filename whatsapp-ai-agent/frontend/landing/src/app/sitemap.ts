import type { MetadataRoute } from "next";

// Convención de Next.js App Router: este archivo genera /sitemap.xml solo.
// NEXT_PUBLIC_SITE_URL debe apuntar al dominio público final (ver DEPLOY.md) -
// mientras no esté cargada, cae a localhost (no rompe el build, pero hay que
// setearla antes de enviar esto a Google Search Console).
const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:3100";

const RUTAS_ESTATICAS = ["", "/precios", "/agendamiento", "/ecommerce", "/blog", "/privacidad", "/terminos"];

export default function sitemap(): MetadataRoute.Sitemap {
  const ahora = new Date();
  return RUTAS_ESTATICAS.map((ruta) => ({
    url: `${SITE_URL}${ruta}`,
    lastModified: ahora,
    changeFrequency: ruta === "" ? "weekly" : "monthly",
    priority: ruta === "" ? 1 : 0.7,
  }));
}
