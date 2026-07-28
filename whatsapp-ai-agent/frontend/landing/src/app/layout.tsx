import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import WhatsAppFloatButton from "@/components/WhatsAppFloatButton";
import Analytics from "@/components/Analytics";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  weight: ["400", "500", "600", "700", "800"],
});

// NEXT_PUBLIC_SITE_URL: dominio público final de la landing (ver DEPLOY.md).
// Sin ella, cae a localhost - metadataBase igual necesita algún valor
// absoluto para poder resolver las URLs relativas de Open Graph.
const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:3100";
const TITULO = "ServiceAgent — Tu WhatsApp e Instagram, atendiendo solos";
const DESCRIPCION =
  "El asistente con IA que conversa como una persona de tu equipo, no con menús robóticos. Responde al instante, agenda tus horas y cierra ventas por WhatsApp e Instagram, las 24 horas.";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: TITULO,
    template: "%s",
  },
  description: DESCRIPCION,
  openGraph: {
    title: TITULO,
    description: DESCRIPCION,
    url: SITE_URL,
    siteName: "ServiceAgent",
    locale: "es_CL",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: TITULO,
    description: DESCRIPCION,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className={inter.variable}>
      <body className="flex min-h-screen flex-col bg-cream font-sans text-[16px] leading-[27px] text-ink antialiased">
        <Header />
        <main className="flex-1 pt-[76px]">{children}</main>
        <Footer />
        <WhatsAppFloatButton />
        <Analytics />
      </body>
    </html>
  );
}
