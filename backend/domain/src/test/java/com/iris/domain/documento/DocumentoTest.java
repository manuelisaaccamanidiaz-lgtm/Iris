package com.iris.domain.documento;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.iris.domain.compartido.AccesoDenegadoException;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.evento.RolEnEvento;

class DocumentoTest {

    private final UUID autora = UUID.randomUUID();
    private final UUID otro = UUID.randomUUID();

    private Documento documento(EstadoDocumento estado) {
        return new Documento(UUID.randomUUID(), UUID.randomUUID(), autora, "Contrato.pdf", 1024, "abc123",
                4, estado, null, null, null, null, Instant.now(), Instant.now(), null);
    }

    @Test
    void quienLoSubioPuedeBorrarlo() {
        assertDoesNotThrow(() -> documento(EstadoDocumento.INDEXADO).exigirQueSePuedaBorrar(autora, RolEnEvento.EDITOR));
    }

    @Test
    void unPropietarioPuedeBorrarElDeOtro() {
        assertDoesNotThrow(() -> documento(EstadoDocumento.INDEXADO).exigirQueSePuedaBorrar(otro, RolEnEvento.PROPIETARIO));
    }

    @Test
    void unEditorNoPuedeBorrarElDeOtro() {
        assertThrows(AccesoDenegadoException.class,
                () -> documento(EstadoDocumento.INDEXADO).exigirQueSePuedaBorrar(otro, RolEnEvento.EDITOR));
    }

    @Test
    void noSeBorraMientrasSeIndexa() {
        assertThrows(ConflictoException.class,
                () -> documento(EstadoDocumento.PROCESANDO).exigirQueSePuedaBorrar(autora, RolEnEvento.PROPIETARIO));
    }
}
