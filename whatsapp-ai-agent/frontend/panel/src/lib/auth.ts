const TOKEN_KEY = "panel_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  window.localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  window.localStorage.removeItem(TOKEN_KEY);
}

/**
 * Lee el claim tenantId del JWT sin verificar la firma - solo para decidir
 * qué mostrar en la UI (mostrar/ocultar acciones admin-only). El backend
 * sigue siendo la única fuente de verdad real: cada endpoint admin-only
 * revalida esto con PanelAuth del lado del servidor, esto es puramente
 * cosmético para no mostrar botones que van a devolver 403.
 */
export function esAdminSegunToken(): boolean {
  const token = getToken();
  if (!token) return false;
  try {
    const payload = token.split(".")[1];
    const json = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    return typeof json.tenantId !== "number";
  } catch {
    return false;
  }
}

/**
 * Lee el tenantId del JWT (null si es el admin o si no hay token) - mismo
 * criterio "cosmético" que esAdminSegunToken: solo para decidir qué mostrar
 * (ej. CuentaGate consultando /estado del propio negocio), nunca como fuente
 * de autorización real.
 */
export function tenantIdSegunToken(): number | null {
  const token = getToken();
  if (!token) return null;
  try {
    const payload = token.split(".")[1];
    const json = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    return typeof json.tenantId === "number" ? json.tenantId : null;
  } catch {
    return null;
  }
}
