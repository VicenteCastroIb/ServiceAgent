// Este archivo guardaba getToken/setToken/clearToken/esAdminSegunToken/
// tenantIdSegunToken cuando el JWT del panel vivía en localStorage. Se migró
// a una cookie httpOnly (ver AuthController en el backend) para que un XSS
// en el panel no pueda leer el token - la implementación real ahora vive en
// auth-context.tsx (AuthProvider/useAuth). Se mantiene este re-export para
// no romper algún import residual a "@/lib/auth".
export { AuthProvider, useAuth } from "./auth-context";
export type { SesionInfo } from "./api";
