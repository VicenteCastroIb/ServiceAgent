package com.tuapp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de JwtService: en particular, que generarToken (admin/"state" de
 * InstagramOAuthService) NUNCA lleva el claim tokenVersion, mientras que
 * generarTokenTenant siempre lo lleva - JwtAuthFilter.tokenVigente depende
 * exactamente de esa diferencia para no dejar autenticar un "state" de
 * Instagram como si fuera un login real del panel (ver su Javadoc).
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Clave de 256 bits (32 bytes) en base64, solo para el test.
        String secretDePrueba = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
        jwtService = new JwtService(secretDePrueba, 60);
    }

    @Test
    void generarTokenAdmin_sinTenantIdNiTokenVersion() {
        String token = jwtService.generarToken("admin", null);

        assertThat(jwtService.esValido(token)).isTrue();
        assertThat(jwtService.extraerUsername(token)).isEqualTo("admin");
        assertThat(jwtService.extraerTenantId(token)).isNull();
        assertThat(jwtService.extraerTokenVersion(token)).isNull();
    }

    @Test
    void generarToken_conTenantIdPeroSinTokenVersion() {
        // Es el caso del "state" de InstagramOAuthService: lleva tenantId
        // pero nunca debe llevar tokenVersion (ver Javadoc de la clase).
        String token = jwtService.generarToken("instagram-oauth-state", 42L);

        assertThat(jwtService.extraerTenantId(token)).isEqualTo(42L);
        assertThat(jwtService.extraerTokenVersion(token)).isNull();
    }

    @Test
    void generarTokenTenant_llevaTenantIdYTokenVersion() {
        String token = jwtService.generarTokenTenant("dueno1", 7L, 3);

        assertThat(jwtService.esValido(token)).isTrue();
        assertThat(jwtService.extraerUsername(token)).isEqualTo("dueno1");
        assertThat(jwtService.extraerTenantId(token)).isEqualTo(7L);
        assertThat(jwtService.extraerTokenVersion(token)).isEqualTo(3);
    }

    @Test
    void esValido_devuelveFalseParaTokenMalformado() {
        assertThat(jwtService.esValido("esto-no-es-un-jwt")).isFalse();
    }
}
