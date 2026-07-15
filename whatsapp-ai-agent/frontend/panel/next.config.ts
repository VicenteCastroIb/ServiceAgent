import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Standalone: junta solo lo necesario para correr (server.js + deps mínimas)
  // en .next/standalone, para una imagen Docker liviana - ver Dockerfile.
  output: "standalone",
};

export default nextConfig;
