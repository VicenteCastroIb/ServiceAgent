const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/**
 * Cliente mínimo para el único endpoint del backend que la landing necesita:
 * el registro self-service (doc sección 12). A diferencia de
 * frontend/panel/src/lib/api.ts, acá no hay token ni sesión - la landing es
 * 100% pública, este es el único llamado que hace a la API.
 */
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, { ...options, headers });
  } catch {
    throw new ApiError(0, "No pudimos conectar con el servidor. Probá de nuevo en unos minutos.");
  }

  if (!response.ok) {
    const data = await response.json().catch(() => null);
    const mensaje = data?.mensaje || `Error ${response.status}`;
    throw new ApiError(response.status, mensaje);
  }

  return (await response.json()) as T;
}

export type PlanRegistro = "BASICO" | "PRO";

export interface RegistroInput {
  businessName: string;
  ownerEmail: string;
  panelUsername: string;
  panelPassword: string;
  plan: PlanRegistro;
}

export interface RegistroResponse {
  tenantId: number;
  urlRegistroTarjeta: string | null;
  mensaje: string | null;
}

export function registrarNegocio(input: RegistroInput): Promise<RegistroResponse> {
  return request<RegistroResponse>("/public/registro", {
    method: "POST",
    body: JSON.stringify(input),
  });
}
