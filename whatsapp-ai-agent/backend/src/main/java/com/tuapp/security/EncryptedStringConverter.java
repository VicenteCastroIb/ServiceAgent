package com.tuapp.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra en reposo las credenciales de terceros que carga cada tenant en su
 * panel: Flow (flowApiKey/flowSecretKey), WooCommerce (wooCommerceConsumerKey/
 * wooCommerceConsumerSecret) e Instagram (instagramAccessToken) - ver Tenant.
 * Antes se guardaban en texto plano en la base; @JsonIgnore ya las sacaba de
 * las respuestas de la API, pero eso no protegía nada si la base (o un
 * backup/snapshot) se filtraba: cualquiera con acceso a esas filas podía
 * cobrar/reembolsar en la cuenta Flow real del negocio, leer/escribir su
 * tienda WooCommerce, o postear como su cuenta de Instagram.
 *
 * AES-256-GCM con un IV aleatorio de 12 bytes en CADA valor cifrado - nunca se
 * reusa un IV con la misma clave (reusar un IV en GCM rompe por completo la
 * confidencialidad). Columna guardada como base64(iv || ciphertext+tag).
 *
 * La clave sale de app.encryption-key (ver application.properties) - sin
 * default, si falta la app no arranca: guardar estas credenciales sin cifrar
 * no es aceptable ni siquiera en desarrollo con datos reales de Flow/Meta.
 * Generarla con: openssl rand -base64 32 (debe decodificar a 32 bytes exactos,
 * AES-256).
 *
 * Aplicado explícitamente con @Convert(converter = EncryptedStringConverter.class)
 * en cada campo sensible (a propósito NO autoApply: la mayoría de los String
 * del modelo no deben cifrarse - ej. businessName, o columnas @Column(unique
 * = true)/buscadas por repositorio como whatsappNumber o instagramAccountId,
 * que con IV aleatorio dejarían de poder compararse/buscarse por igualdad).
 *
 * IMPORTANTE al desplegar: si ya existieran filas con estos campos en texto
 * plano de antes de este cambio, hay que migrarlas (re-guardarlas para que
 * pasen por este converter) ANTES de desplegar esta versión - si no,
 * convertToEntityAttribute va a fallar al leerlas (ver Javadoc de ese método).
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public EncryptedStringConverter(@Value("${app.encryption-key}") String base64Key) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.encryption-key no es base64 válido.", e);
        }
        if (decoded.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "app.encryption-key debe decodificar a exactamente 32 bytes (AES-256). Generarla con: openssl rand -base64 32");
        }
        this.key = new SecretKeySpec(decoded, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String valorPlano) {
        if (valorPlano == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cifrado = cipher.doFinal(valorPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cifrar un valor para guardarlo en base.", e);
        }
    }

    /**
     * @throws IllegalStateException si el valor guardado no es un base64+IV
     *         válido para esta clave - típicamente porque app.encryption-key
     *         cambió, o porque la fila se guardó en texto plano antes de que
     *         este converter existiera (ver nota de migración en el Javadoc
     *         de la clase).
     */
    @Override
    public String convertToEntityAttribute(String valorEnBase) {
        if (valorEnBase == null) {
            return null;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(valorEnBase);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(datos, 0, iv, 0, IV_LENGTH_BYTES);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] descifrado = cipher.doFinal(datos, IV_LENGTH_BYTES, datos.length - IV_LENGTH_BYTES);
            return new String(descifrado, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo descifrar un valor leído de base (¿cambió app.encryption-key, o el dato se guardó en texto plano antes de cifrar este campo?)", e);
        }
    }
}
