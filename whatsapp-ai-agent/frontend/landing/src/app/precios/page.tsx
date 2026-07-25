import { redirect } from "next/navigation";

/**
 * La sección de precios ahora vive en la home (`/#precios`), como pide el
 * rediseño oscuro (ver design_handoff_dark_rebrand). Esta ruta se mantiene
 * solo para no romper links/bookmarks viejos a /precios.
 */
export default function PreciosPage() {
  redirect("/#precios");
}
