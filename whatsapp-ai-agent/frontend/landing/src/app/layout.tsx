import type { Metadata } from "next";
import { Bricolage_Grotesque, Hanken_Grotesk } from "next/font/google";
import "./globals.css";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import WhatsAppFloatButton from "@/components/WhatsAppFloatButton";

const bricolage = Bricolage_Grotesque({
  subsets: ["latin"],
  variable: "--font-bricolage",
  weight: "variable",
});

const hanken = Hanken_Grotesk({
  subsets: ["latin"],
  variable: "--font-hanken",
  weight: "variable",
});

export const metadata: Metadata = {
  title: "ServiceAgent — Tu WhatsApp Business, trabajando solo",
  description:
    "El asistente con inteligencia artificial que conversa como persona, no con menús robóticos. Responde al instante, agenda tus horas y cierra tus ventas por WhatsApp e Instagram, también cuando tú no puedes.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className={`${bricolage.variable} ${hanken.variable}`}>
      <body className="flex min-h-screen flex-col bg-white font-sans text-[16px] leading-[27px] text-slate-700 antialiased">
        <Header />
        <main className="flex-1 pt-[76px]">{children}</main>
        <Footer />
        <WhatsAppFloatButton />
      </body>
    </html>
  );
}
