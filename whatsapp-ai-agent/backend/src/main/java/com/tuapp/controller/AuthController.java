package com.tuapp.controller;

import com.tuapp.security.JwtService;
import com.tuapp.security.PanelAuth;
import com.tuapp.security.PanelUserDetails;
import com.tuapp.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Login del panel (Next.js): valida usuario/clave y deja el JWT en una
 * cookie httpOnly (ver JwtService.COOKIE_NAME) en vez de devolverlo en el
 * cuerpo de la respuesta - así ningún script del lado del cliente puede
 * leerlo, ni al hacer login ni después, aunque en algún momento se
 * introdujera un XSS en el panel. El backend también sigue aceptando
 * "Authorization: Bearer &lt;token&gt;" (ver JwtAuthFilter) para no cerrarle
 * la puerta a otros clientes (scripts, integraciones futuras) que sí
 * manejen el token explícitamente - el panel web en sí ya no lo hace.
 *
 * Valida contra PanelUserDetailsService: el admin (PANEL_USERNAME/PANEL_PASSWORD,
 * ve todos los negocios) o el login propio de un tenant (Tenant.panelUsername,
 * ve solo el suyo - ver TenantService.fijarCredencialesPanel).
 *
 * Rate limiting (mismo RateLimiter que ya usaba RegistroController): dos
 * límites independientes, por IP y por username, ambos se registran en cada
 * intento - por IP frena a un atacante golpeando el endpoint desde un mismo
 * origen, por username frena un ataque distribuido contra UNA cuenta puntual.
 *
 * Nota de despliegue: panel y backend hoy corren como dos servicios Railway
 * en subdominios *.up.railway.app distintos - eso es "cross-site" para
 * cookies, así que por defecto la cookie se emite con SameSite=None (ver
 * cookieDeSesion). Configurable vía app.security.cookie-same-site
 * (COOKIE_SAME_SITE en el .env) precisamente para no tener que tocar código
 * cuando panel y backend queden bajo un mismo dominio raíz (ver DEPLOY.md,
 * sección 5): ahí alcanza con cargar COOKIE_SAME_SITE=Lax, más estricto y ya
 * no depende de la mitigación CSRF descrita en SecurityConfig.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final int MAX_INTENTOS_POR_IP = 20;
    private static final int MAX_INTENTOS_POR_USUARIO = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RateLimiter rateLimiter;
    private final String cookieSameSite;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RateLimiter rateLimiter,
            @Value("${app.security.cookie-same-site:None}") String cookieSameSite) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        // Ambas llamadas deben ejecutarse siempre (cada intento cuenta para
        // los dos límites), por eso se evalúan por separado en vez de con
        // "||" (que cortocircuitaría la segunda si la primera ya da true).
        boolean dentroDeLimiteIp = rateLimiter.permitir("login:ip:" + ip, MAX_INTENTOS_POR_IP, VENTANA);
        boolean dentroDeLimiteUsuario = rateLimiter.permitir("login:usuario:" + request.username(), MAX_INTENTOS_POR_USUARIO, VENTANA);
        if (!dentroDeLimiteIp || !dentroDeLimiteUsuario) {
            log.warn("Rate limit excedido en /auth/login (ip={}, usuario={})", ip, request.username());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Demasiados intentos. Probá de nuevo más tarde."));
        }

        Authentication resultado;
        try {
            resultado = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            // Cubre tanto clave incorrecta como usuario inexistente - no hay
            // que distinguirlos en la respuesta (evita filtrar qué usuarios existen).
            return ResponseEntity.status(401).build();
        }

        PanelUserDetails principal = (PanelUserDetails) resultado.getPrincipal();
        Long tenantId = principal.getTenantId();
        // Token de tenant lleva tokenVersion (revocable, ver JwtAuthFilter);
        // el de admin no tiene fila en base contra la cual revalidarlo.
        String token = tenantId != null
                ? jwtService.generarTokenTenant(request.username(), tenantId, principal.getTokenVersion())
                : jwtService.generarToken(request.username(), null);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieDeSesion(token).toString())
                .body(new SesionResponse(tenantId == null, tenantId));
    }

    /**
     * El panel lo llama al cargar cualquier página para saber si hay sesión
     * vigente y si es admin o el dueño de un tenant puntual - reemplaza la
     * decodificación del JWT que antes hacía el frontend directamente (ya no
     * puede: el token vive en una cookie httpOnly, invisible para JS).
     * Requiere estar autenticado (no está en la lista de permitAll de
     * SecurityConfig), así que sin cookie válida esto ya responde 401 antes
     * de llegar acá - el frontend usa justo ese 401 como señal para mandar a
     * /login.
     */
    @GetMapping("/me")
    public ResponseEntity<SesionResponse> me() {
        return ResponseEntity.ok(new SesionResponse(PanelAuth.esAdmin(), PanelAuth.tenantIdActual()));
    }

    /**
     * Limpia la cookie de sesión. Público a propósito (ver SecurityConfig):
     * cerrar sesión tiene que funcionar aunque la cookie ya esté vencida o
     * sea inválida, no puede depender de tener una sesión válida para
     * completarse.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie limpia = ResponseCookie.from(JwtService.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, limpia.toString()).build();
    }

    private ResponseCookie cookieDeSesion(String token) {
        return ResponseCookie.from(JwtService.COOKIE_NAME, token)
                .httpOnly(true)
                // Secure: los navegadores modernos exceptúan a http://localhost
                // de este requisito, así que el dev local sigue funcionando.
                .secure(true)
                // Default "None" (ver constructor/Javadoc de la clase): panel y
                // backend hoy son servicios Railway en subdominios distintos
                // (cross-site para cookies) - sin SameSite=None el navegador
                // directamente no manda la cookie entre esos dos orígenes.
                // Configurable a "Lax" vía app.security.cookie-same-site una
                // vez que compartan dominio raíz (ver DEPLOY.md, sección 5).
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofMinutes(jwtService.getExpirationMinutes()))
                .build();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    /** esAdmin=true y tenantId=null para el admin; esAdmin=false + el id del negocio para el dueño de un tenant. */
    public record SesionResponse(boolean esAdmin, Long tenantId) {
    }

    public record ErrorResponse(String mensaje) {
    }
}
