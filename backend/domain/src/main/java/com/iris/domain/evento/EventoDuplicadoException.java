package com.iris.domain.evento;

import com.iris.domain.compartido.ReglaDeNegocioException;

/** Ya creaste un evento con ese nombre que empieza ese mismo día. */
public class EventoDuplicadoException extends ReglaDeNegocioException {

    public EventoDuplicadoException() {
        super("Ya tienes un evento con ese nombre que empieza en la misma fecha");
    }
}
