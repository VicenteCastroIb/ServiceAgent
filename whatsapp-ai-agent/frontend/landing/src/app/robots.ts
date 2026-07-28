import type { MetadataRoute } from "next";

const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:3100";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      disallow: ["/registro"], // formulario de alta, no aporta nada indexado
    },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
