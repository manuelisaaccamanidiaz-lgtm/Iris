package com.iris.application.port.out;

import java.util.function.Supplier;

/**
 * Puerto de salida: "haz este trabajo TODO o NADA".
 *
 * <p>Hasta ahora cada transacción vivía dentro de UN repositorio (crearConPropietario).
 * Aceptar una invitación toca DOS repositorios (invitaciones y miembros), así que el caso
 * de uso necesita abrir la transacción él mismo. Como application es Java puro (sin Spring),
 * no puede usar @Transactional: pide este puerto, y infrastructure lo implementa con
 * el TransactionTemplate de Spring.
 *
 * <p>Si el trabajo lanza una excepción, se hace ROLLBACK de todo lo que se escribió dentro.
 */
public interface Transaccion {

    /** Ejecuta el trabajo en una transacción y devuelve su resultado. */
    <T> T ejecutar(Supplier<T> trabajo);

    /** Igual, para trabajos que no devuelven nada. */
    default void ejecutarSinResultado(Runnable trabajo) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        });
    }
}
