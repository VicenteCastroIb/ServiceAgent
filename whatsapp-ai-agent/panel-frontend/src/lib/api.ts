import { clearToken, getToken } from "./auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (response.status === 401) {
    // El token expiró o es inválido: se limpia y se manda a login.
    clearToken();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new ApiError(401, "No autenticado");
  }

  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new ApiError(response.status, text || `Error ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export interface Tenant {
  id: number;
  businessName: string;
  whatsappNumber: string;
  businessContext: string;
  plan: "BASICO" | "PRO" | "CATALOGO";
  panelUsername: string | null;
  createdAt: string;
}

export interface Handoff {
  numeroCliente: string;
  motivo: string;
}

export interface TenantSubscription {
  id: number;
  paymentMethod: "MANUAL" | "FLOW_AUTOMATICO";
  paidUntil: string | null;
  status: "PENDIENTE_TARJETA" | "ACTIVA" | "MOROSA" | "CANCELADA";
  lastPaymentAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export async function login(username: string, password: string): Promise<string> {
  const data = await request<{ token: string }>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
  return data.token;
}

export function listarTenants(): Promise<Tenant[]> {
  return request<Tenant[]>("/admin/tenants");
}

export function buscarTenant(id: number): Promise<Tenant> {
  return request<Tenant>(`/admin/tenants/${id}`);
}

export function crearTenant(input: {
  businessName: string;
  whatsappNumber: string;
  businessContext: string;
  panelUsername?: string;
  panelPassword?: string;
}): Promise<Tenant> {
  return request<Tenant>("/admin/tenants", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function actualizarContextoTenant(id: number, businessContext: string): Promise<Tenant> {
  return request<Tenant>(`/admin/tenants/${id}/contexto`, {
    method: "PUT",
    body: JSON.stringify({ businessContext }),
  });
}

export function eliminarTenant(id: number): Promise<void> {
  return request<void>(`/admin/tenants/${id}`, {
    method: "DELETE",
  });
}

export function listarHandoffs(): Promise<Handoff[]> {
  return request<Handoff[]>("/admin/handoffs");
}

export function reanudarHandoff(numeroCliente: string): Promise<void> {
  return request<void>(`/admin/handoffs/${encodeURIComponent(numeroCliente)}/reanudar`, {
    method: "POST",
  });
}

// null significa "todavía no se registró ningún pago para este negocio"
// (ni manual ni por Flow) - no es un error, es un estado válido.
export async function obtenerSuscripcion(tenantId: number): Promise<TenantSubscription | null> {
  try {
    return await request<TenantSubscription>(`/admin/tenants/${tenantId}/billing`);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      return null;
    }
    throw e;
  }
}

export function registrarPagoManual(tenantId: number, paidUntil: string): Promise<TenantSubscription> {
  return request<TenantSubscription>(`/admin/tenants/${tenantId}/billing/manual`, {
    method: "PUT",
    body: JSON.stringify({ paidUntil }),
  });
}
