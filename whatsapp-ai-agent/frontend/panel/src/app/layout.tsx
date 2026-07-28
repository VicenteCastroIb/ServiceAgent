import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import Nav from "@/components/Nav";
import CuentaGate from "@/components/CuentaGate";
import { AuthProvider } from "@/lib/auth-context";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  weight: ["400", "500", "600", "700", "800"],
});

export const metadata: Metadata = {
  title: "Panel — ServiceAgent",
  description: "Panel de administración del agente de IA para WhatsApp e Instagram",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className={`h-full antialiased ${inter.variable}`}>
      <body className="flex min-h-full flex-col bg-cream font-sans text-ink">
        <AuthProvider>
          <Nav />
          <main className="mx-auto w-full max-w-[1280px] flex-1 px-5 py-8 sm:px-8">
            <CuentaGate>{children}</CuentaGate>
          </main>
        </AuthProvider>
      </body>
    </html>
  );
}
