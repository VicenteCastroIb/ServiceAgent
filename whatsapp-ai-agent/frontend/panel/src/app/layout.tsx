import type { Metadata } from "next";
import "./globals.css";
import Nav from "@/components/Nav";

export const metadata: Metadata = {
  title: "Panel - Agente de IA",
  description: "Panel de administración del agente de IA para WhatsApp e Instagram",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es" className="h-full antialiased">
      <body className="min-h-full flex flex-col bg-gray-50 text-gray-900 font-sans">
        <Nav />
        <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-6">{children}</main>
      </body>
    </html>
  );
}
