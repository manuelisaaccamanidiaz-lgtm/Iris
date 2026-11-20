package com.iris.infrastructure.adapter.out.persistence.evento;

import com.iris.domain.evento.RolEnEvento;

/** Una "fila" de resultado: el evento y el rol del miembro (lo arma la consulta JPQL con "new"). */
public record EventoConRolFila(EventoEntity evento, RolEnEvento rol) {
}
