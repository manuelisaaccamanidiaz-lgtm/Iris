package com.iris.domain.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.iris.domain.compartido.ReglaDeNegocioException;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void quitaEspaciosYNormalizaEnMinusculas() {
        // Arrange: el dato de entrada
        String escrito = "  Ana@Iris.com";

        // Act: lo que queremos probar
        Email email = new Email(escrito);

        // Assert: assertEquals(esperado, real)
        assertEquals("Ana@Iris.com", email.valor());
        assertEquals("ana@iris.com", email.normalizado());
    }

    @Test
    void emailSinArroba() {
        String escrito = "ana.iris.com";

        assertThrows(ReglaDeNegocioException.class, () -> new Email(escrito));
    }
}
