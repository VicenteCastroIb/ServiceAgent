import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import WhatsAppFloatButton from "@/components/WhatsAppFloatButton";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  weight: ["400", "500", "600", "700", "800"],
});

export const metadata: Metadata = {
  title: "ServiceAgent — Tu WhatsApp e Instagram, atendiendo solos",
  description:
    "El asistente con IA que conversa como una persona de tu equipo, no con menús robóticos. Responde al instante, agenda tus horas y cierra ventas por WhatsApp e Instagram, las 24 horas.",
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
      </body>
    </html>
  );
}
