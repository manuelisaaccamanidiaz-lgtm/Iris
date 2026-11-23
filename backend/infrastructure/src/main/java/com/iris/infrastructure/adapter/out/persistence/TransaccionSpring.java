package com.iris.infrastructure.adapter.out.persistence;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.iris.application.port.out.Transaccion;

/**
 * Adaptador del puerto Transaccion con el TransactionTemplate de Spring (Spring Boot lo crea solo).
 * Es la versión "programática" de @Transactional: abre la transacción, ejecuta el trabajo,
 * hace COMMIT si termina bien y ROLLBACK si lanza una excepción (que sigue subiendo tal cual).
 */
@Component
class TransaccionSpring implements Transaccion {

    private final TransactionTemplate plantilla;

    TransaccionSpring(TransactionTemplate plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        return plantilla.execute(estado -> trabajo.get());
    }
}
