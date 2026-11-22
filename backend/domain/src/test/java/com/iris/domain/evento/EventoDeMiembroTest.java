package com.iris.domain.evento;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.iris.domain.compartido.ConflictoException;

class EventoDeMiembroTest {

    private final UUID creador = UUID.randomUUID();
    private final UUID copropietario = UUID.randomUUID();
    private final Evento evento = Evento.crear(creador, "Expo", null, null,
            LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));

    @Test
    void unCopropietarioNoPuedeQuitarAOtroPropietario() {
        // Arrange: yo soy el copropietario; el objetivo es OTRO propietario
        EventoDeMiembro comoCopropietario = new EventoDeMiembro(evento, RolEnEvento.PROPIETARIO);
        MiembroEvento otroPropietario = new MiembroEvento(evento.id(), UUID.randomUUID(), "Luis",
                "luis@mail.com", RolEnEvento.PROPIETARIO, Instant.now(), null);

        // Act + Assert
        assertThrows(ConflictoException.class, () -> comoCopropietario.exigirPuedeGestionar(otroPropietario, copropietario));
    }
}