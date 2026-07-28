package com.tuapp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de EncryptedStringConverter: cifra en reposo las credenciales de
 * terceros de cada tenant (Flow/WooCommerce/Instagram, ver Tenant). Cubre lo
 * esencial de una implementación AES-GCM propia: que el round-trip funcione,
 * que null se maneje sin reventar, que dos cifrados del mismo valor NO sean
 * iguales (IV aleatorio - si lo fueran, sería una señal de IV fijo/reusado,
 * que rompe la seguridad de GCM), y que una clave de largo incorrecto falle
 * rápido al construir el converter en vez de fallar más tarde y de forma
 * confusa en el primer insert/select.
 */
class EncryptedStringConverterTest {

    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        String claveDePrueba = Base64.getEncoder().encodeToString(new byte[32]);
        converter = new EncryptedStringConverter(claveDePrueba);
    }

    @Test
    void roundTrip_devuelveElValorOriginal() {
        String original = "sk_live_flow_secret_key_de_prueba";

        String cifrado = converter.convertToDatabaseColumn(original);
        String descifrado = converter.convertToEntityAttribute(cifrado);

        assertThat(cifrado).isNotEqualTo(original);
        assertThat(descifrado).isEqualTo(original);
    }

    @Test
    void convertToDatabaseColumn_nullDevuelveNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_nullDevuelveNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void mismoValorCifradoDosVeces_daResultadosDistintos() {
        String original = "mismo-secreto";

        String cifrado1 = converter.convertToDatabaseColumn(original);
        String cifrado2 = converter.convertToDatabaseColumn(original);

        // IV aleatorio por valor (ver Javadoc de la clase) - si esto fallara,
        // el IV estaría fijo o reusado, lo que rompe GCM por completo.
        assertThat(cifrado1).isNotEqualTo(cifrado2);
        assertThat(converter.convertToEntityAttribute(cifrado1)).isEqualTo(original);
        assertThat(converter.convertToEntityAttribute(cifrado2)).isEqualTo(original);
    }

    @Test
    void claveDeLargoIncorrecto_fallaAlConstruirElConverter() {
        String claveCorta = Base64.getEncoder().encodeToString(new byte[16]); // AES-128, no 256

        assertThatThrownBy(() -> new EncryptedStringConverter(claveCorta))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void valorCifradoConOtraClave_fallaAlDescifrar() {
        String otraClave = Base64.getEncoder().encodeToString(new byte[32]);
        // Distinta de la clave de setUp (todos ceros) - forzamos bytes no nulos.
        byte[] bytes = Base64.getDecoder().decode(otraClave);
        bytes[0] = 1;
        EncryptedStringConverter otroConverter = new EncryptedStringConverter(Base64.getEncoder().encodeToString(bytes));

        String cifrado = converter.convertToDatabaseColumn("secreto");

        assertThatThrownBy(() -> otroConverter.convertToEntityAttribute(cifrado))
                .isInstanceOf(IllegalStateException.class);
    }
}
