package com.iris.domain.usuario;

import com.iris.domain.compartido.ReglaDeNegocioException;

/** Regla de negocio: no pueden existir dos cuentas con el mismo email (ignorando mayúsculas). */
public class EmailYaRegistradoException extends ReglaDeNegocioException {

    public EmailYaRegistradoException() {
        super("Ya existe una cuenta con ese email");
    }
}
