/**
 * Número de WhatsApp de VENTAS (Vicente, no un tenant) - al que llegan los
 * prospectos desde la landing para que cierre la venta con demo en vivo
 * (doc sección 2: venta puerta a puerta, el diferencial frente a aibot.cl).
 * No confundir con el WhatsApp de cada negocio cliente, que es otro número
 * completamente aparte (Tenant.whatsappNumber, doc sección 5.6).
 */
const NUMERO_VENTAS = process.env.NEXT_PUBLIC_WHATSAPP_SALES_NUMBER ?? "";

/** Link de wa.me con mensaje precargado. Si no hay número configurado, cae a "#" (ver WhatsAppFloatButton). */
export function whatsappSalesLink(mensaje: string): string {
  if (!NUMERO_VENTAS) return "#";
  return `https://wa.me/${NUMERO_VENTAS}?text=${encodeURIComponent(mensaje)}`;
}
