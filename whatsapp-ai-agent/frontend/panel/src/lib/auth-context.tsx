"use client";

import { createContext, ReactNode, useCallback, useContext, useEffect, useState } from "react";
import { obtenerSesion, SesionInfo } from "./api";

interface AuthState extends SesionInfo {
  autenticado: boolean;
  cargando: boolean;
}

interface AuthContextValue extends AuthState {
  /** Vuelve a consultar /auth/me (por si la sesión cambió en otra pestaña). */
  refrescar: () => Promise<void>;
  /** Actualiza el estado local justo después de un login exitoso, sin esperar un segundo round-trip a /auth/me. */
  establecerSesion: (sesion: SesionInfo) => void;
  /** Limpia el estado local justo después de un logout (el backend ya limpió la cookie, ver AuthController.logout). */
  cerrarSesionLocal: () => void;
}

const ESTADO_INICIAL: AuthState = { esAdmin: false, tenantId: null, autenticado: false, cargando: true };

const AuthContext = createContext<AuthContextValue>({
  ...ESTADO_INICIAL,
  refrescar: async () => {},
  establecerSesion: () => {},
  cerrarSesionLocal: () => {},
});

/**
 * Fuente de verdad de "quién soy" en el panel, del lado del cliente. Antes se
 * resolvía decodificando el JWT guardado en localStorage (funciones
 * esAdminSegunToken/tenantIdSegunToken que vivían acá); ahora el JWT viaja en
 * una cookie httpOnly (invisible para JS, ver AuthController) para que un XSS
 * en el panel no pueda robarlo, así que la única forma de saber si hay sesión
 * vigente es preguntarle al backend (GET /auth/me).
 *
 * Envuelve toda la app (ver layout.tsx) y consulta /auth/me una sola vez al
 * cargar. login/logout actualizan el estado local directamente
 * (establecerSesion/cerrarSesionLocal) en vez de esperar un segundo
 * round-trip.
 *
 * Esto sigue siendo puramente para decidir qué mostrar en la UI (botones
 * admin-only, redirección a /login si no hay sesión) - nunca la fuente real
 * de autorización: eso lo sigue validando el backend en cada request (ver
 * PanelAuth del lado del servidor).
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<AuthState>(ESTADO_INICIAL);

  const refrescar = useCallback(async () => {
    try {
      const sesion = await obtenerSesion();
      setEstado({ ...sesion, autenticado: true, cargando: false });
    } catch {
      setEstado({ esAdmin: false, tenantId: null, autenticado: false, cargando: false });
    }
  }, []);

  useEffect(() => {
    // Misma lógica que refrescar(), pero escrita inline (en vez de llamar a
    // la función) con el guard "cancelado" de rigor para un fetch-on-mount -
    // eslint (react-hooks/set-state-in-effect) no puede ver a través de una
    // función externa que a su vez hace el setState de forma asíncrona.
    let cancelado = false;
    obtenerSesion()
      .then((sesion) => {
        if (!cancelado) setEstado({ ...sesion, autenticado: true, cargando: false });
      })
      .catch(() => {
        if (!cancelado) setEstado({ esAdmin: false, tenantId: null, autenticado: false, cargando: false });
      });
    return () => {
      cancelado = true;
    };
  }, []);

  function establecerSesion(sesion: SesionInfo) {
    setEstado({ ...sesion, autenticado: true, cargando: false });
  }

  function cerrarSesionLocal() {
    setEstado({ esAdmin: false, tenantId: null, autenticado: false, cargando: false });
  }

  return (
    <AuthContext.Provider value={{ ...estado, refrescar, establecerSesion, cerrarSesionLocal }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  return useContext(AuthContext);
}
