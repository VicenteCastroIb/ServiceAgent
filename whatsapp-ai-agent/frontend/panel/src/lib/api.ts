const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");

  // credentials:"include" en vez de un header Authorization armado a mano: el
  // JWT viaja en una cookie httpOnly (ver AuthController), así que el propio
  // navegador la adjunta - este código nunca ve ni maneja el token.
  const response = await fetch(`${API_URL}${path}`, { ...options, headers, credentials: "include" });

  if (response.status === 401) {
    // Sin sesión vigente. No hay nada que "limpiar" del lado del cliente (la
    // cookie es httpOnly, invisible para JS) - solo redirigir a login, salvo
    // que ya estemos ahí (evita una navegación redundante que además pisaba
    // el mensaje de error de credenciales incorrectas en /auth/login).
    if (typeof window !== "undefined" && window.location.pathname !== "/login") {
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
  wooCommerceConfigurado: boolean;
  flowConfigurado: boolean;
  instagramAccountId: string | null;
  instagramConfigurado: boolean;
  ownerEmail: string | null;
  createdAt: string;
}

export interface Handoff {
  numeroCliente: string;
  motivo: string;
}

export interface ConversationSummary {
  id: number;
  channel: "WHATSAPP" | "INSTAGRAM";
  clientContact: string;
  lastMessageAt: string | null;
  tenantId: number;
  businessName: string;
}

export interface ConversationMessage {
  id: number;
  direction: "IN" | "OUT";
  sender: "CLIENTE" | "BOT" | "HUMANO";
  content: string;
  sentAt: string;
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

export interface Professional {
  id: number;
  name: string;
  active: boolean;
}

export type DiaSemana =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export interface Availability {
  id: number;
  dayOfWeek: DiaSemana;
  startTime: string;
  endTime: string;
  slotMinutes: number;
}

export interface Appointment {
  id: number;
  professional: Professional;
  clientPhoneNumber: string;
  service: string;
  startTime: string;
  status: "CONFIRMADA" | "CANCELADA" | "REAGENDADA" | "COMPLETADA" | "NO_SHOW";
  reminderSent: boolean;
  createdAt: string;
}

export interface Product {
  id: number;
  name: string;
  price: number;
  category: string | null;
  subcategory: string | null;
  externalId: number | null;
  imageUrl: string | null;
  purchaseUrl: string | null;
  stockQuantity: number | null;
  active: boolean;
  updatedAt: string | null;
}

export interface ProductoInput {
  name: string;
  price: number;
  category?: string;
  subcategory?: string;
  stockQuantity?: number;
}

export interface ReporteAgendamiento {
  desde: string;
  hasta: string;
  totalCitas: number;
  citasPorEstado: Partial<Record<Appointment["status"], number>>;
  noShowsEvitados: number;
  noShows: number;
  citasPorSemana: { inicioSemana: string; cantidad: number }[];
  horasPeak: { hora: number; cantidad: number }[];
}

export interface PaymentOrder {
  id: number;
  clientPhoneNumber: string;
  commerceOrder: string;
  amount: number;
  status: "PENDIENTE" | "PAGADA" | "RECHAZADA" | "ANULADA";
  createdAt: string;
  confirmedAt: string | null;
}

export interface SesionInfo {
  esAdmin: boolean;
  tenantId: number | null;
}

// Ya no devuelve el JWT: el backend lo deja en una cookie httpOnly (ver
// AuthController.login) y esto solo trae los datos de UI (esAdmin/tenantId,
// ver AuthProvider en auth-context.tsx).
export function login(username: string, password: string): Promise<SesionInfo> {
  return request<SesionInfo>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

// Lo consulta AuthProvider al cargar la app para saber si hay sesión vigente
// (reemplaza la decodificación del JWT que antes hacía el frontend).
export function obtenerSesion(): Promise<SesionInfo> {
  return request<SesionInfo>("/auth/me");
}

export function cerrarSesion(): Promise<void> {
  return request<void>("/auth/logout", { method: "POST" });
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

// Vacío ("") borra el email cargado (deja de notificar al dueño por handoff).
export function actualizarOwnerEmail(id: number, ownerEmail: string): Promise<Tenant> {
  return request<Tenant>(`/admin/tenants/${id}/owner-email`, {
    method: "PUT",
    body: JSON.stringify({ ownerEmail }),
  });
}

// --- Bandeja de conversaciones (modo híbrido, doc sección 2) ---

export function listarConversaciones(): Promise<ConversationSummary[]> {
  return request<ConversationSummary[]>("/admin/conversations");
}

export function listarMensajesConversacion(conversationId: number): Promise<ConversationMessage[]> {
  return request<ConversationMessage[]>(`/admin/conversations/${conversationId}/mensajes`);
}

export function responderConversacion(conversationId: number, texto: string): Promise<ConversationMessage> {
  return request<ConversationMessage>(`/admin/conversations/${conversationId}/responder`, {
    method: "POST",
    body: JSON.stringify({ texto }),
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

export function iniciarSuscripcion(tenantId: number, email: string): Promise<{ urlRegistroTarjeta: string }> {
  return request(`/admin/tenants/${tenantId}/billing/iniciar`, {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function marcarSuscripcionMorosa(tenantId: number): Promise<void> {
  return request<void>(`/admin/tenants/${tenantId}/billing/marcar-morosa`, {
    method: "POST",
  });
}

export interface EstadoCuenta {
  subscriptionStatus: TenantSubscription["status"] | null;
  suscripcionActiva: boolean;
  whatsappConfigurado: boolean;
  instagramConfigurado: boolean;
  listoParaOperar: boolean;
}

// Usado por CuentaGate (doc sección 12) para decidir si mostrar el dashboard
// completo o la pantalla de "completá tu pago"/"estamos activando tu cuenta".
export function obtenerEstadoCuenta(tenantId: number): Promise<EstadoCuenta> {
  return request<EstadoCuenta>(`/admin/tenants/${tenantId}/estado`);
}

// --- Agendamiento (Semana 5) ---

export function listarProfesionales(tenantId: number): Promise<Professional[]> {
  return request<Professional[]>(`/admin/tenants/${tenantId}/professionals`);
}

export function crearProfesional(tenantId: number, name: string): Promise<Professional> {
  return request<Professional>(`/admin/tenants/${tenantId}/professionals`, {
    method: "POST",
    body: JSON.stringify({ name }),
  });
}

export function listarDisponibilidad(professionalId: number): Promise<Availability[]> {
  return request<Availability[]>(`/admin/professionals/${professionalId}/availability`);
}

export function crearDisponibilidad(
  professionalId: number,
  input: { dayOfWeek: DiaSemana; startTime: string; endTime: string; slotMinutes: number }
): Promise<Availability> {
  return request<Availability>(`/admin/professionals/${professionalId}/availability`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function listarCitas(
  tenantId: number,
  filtros?: { professionalId?: number; desde?: string; hasta?: string }
): Promise<Appointment[]> {
  const params = new URLSearchParams();
  if (filtros?.professionalId) params.set("professionalId", String(filtros.professionalId));
  if (filtros?.desde) params.set("desde", filtros.desde);
  if (filtros?.hasta) params.set("hasta", filtros.hasta);
  const query = params.toString();
  return request<Appointment[]>(
    `/admin/tenants/${tenantId}/appointments${query ? `?${query}` : ""}`
  );
}

export function actualizarCita(
  id: number,
  input: { status?: Appointment["status"]; startTime?: string }
): Promise<Appointment> {
  return request<Appointment>(`/admin/appointments/${id}`, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}

export function obtenerReporteAgendamiento(
  tenantId: number,
  filtros?: { desde?: string; hasta?: string }
): Promise<ReporteAgendamiento> {
  const params = new URLSearchParams();
  if (filtros?.desde) params.set("desde", filtros.desde);
  if (filtros?.hasta) params.set("hasta", filtros.hasta);
  const query = params.toString();
  return request<ReporteAgendamiento>(
    `/admin/tenants/${tenantId}/appointments/reporte${query ? `?${query}` : ""}`
  );
}

// --- Catálogo (Semana 6) ---

export function fijarCredencialesWooCommerce(
  tenantId: number,
  input: { url: string; consumerKey: string; consumerSecret: string }
): Promise<Tenant> {
  return request<Tenant>(`/admin/tenants/${tenantId}/catalogo/woocommerce`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function sincronizarCatalogo(tenantId: number): Promise<{ productosSincronizados: number }> {
  return request(`/admin/tenants/${tenantId}/catalogo/sincronizar`, {
    method: "POST",
  });
}

export function listarProductos(tenantId: number): Promise<Product[]> {
  return request<Product[]>(`/admin/tenants/${tenantId}/catalogo/productos`);
}

// Alta manual del catálogo (sin necesidad de tienda WooCommerce) - ver
// CatalogController.crearProducto/actualizarProducto/eliminarProducto.

export function crearProducto(tenantId: number, input: ProductoInput): Promise<Product> {
  return request<Product>(`/admin/tenants/${tenantId}/catalogo/productos`, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function actualizarProducto(
  tenantId: number,
  productId: number,
  input: ProductoInput & { active: boolean }
): Promise<Product> {
  return request<Product>(`/admin/tenants/${tenantId}/catalogo/productos/${productId}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function eliminarProducto(tenantId: number, productId: number): Promise<void> {
  return request<void>(`/admin/tenants/${tenantId}/catalogo/productos/${productId}`, {
    method: "DELETE",
  });
}

// --- Pagos del tenant a sus propios clientes (Semana 6) ---

export function fijarCredencialesFlow(
  tenantId: number,
  input: { apiKey: string; secretKey: string }
): Promise<Tenant> {
  return request<Tenant>(`/admin/tenants/${tenantId}/pagos/flow`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function listarOrdenesPago(tenantId: number): Promise<PaymentOrder[]> {
  return request<PaymentOrder[]>(`/admin/tenants/${tenantId}/pagos/ordenes`);
}

// --- Instagram (Graph API directa de Meta) ---

export function fijarCredencialesInstagram(
  tenantId: number,
  input: { instagramAccountId: string; accessToken: string }
): Promise<Tenant> {
  return request<Tenant>(`/admin/tenants/${tenantId}/instagram/credenciales`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

// Self-service (doc sección 12): devuelve la URL de autorización de Meta a
// la que hay que redirigir al navegador (window.location.href), no un
// resultado final - la conexión se completa en el callback del backend.
export function iniciarConexionInstagram(tenantId: number): Promise<{ url: string }> {
  return request(`/admin/tenants/${tenantId}/instagram/oauth/iniciar`, {
    method: "POST",
  });
}
