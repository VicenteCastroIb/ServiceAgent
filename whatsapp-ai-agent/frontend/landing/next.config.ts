import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Standalone: junta solo lo necesario para correr (server.js + deps mínimas)
  // en .next/standalone, para una imagen Docker liviana - mismo patrón que frontend/panel.
  output: "standalone",
};

export default nextConfig;
