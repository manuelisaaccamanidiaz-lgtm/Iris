package com.iris.domain.propuesta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para las propuestas de Iris. Siempre dentro de un evento. */
public interface PropuestaRepositorio {

    /**
     * Inserta si id es null. Si no, actualiza SOLO si sigue PENDIENTE (doble clic: el segundo
     * no encuentra nada que actualizar y recibe un 409, igual que las invitaciones).
     */
    Propuesta guardar(Propuesta propuesta);

    Optional<Propuesta> buscarEnEvento(UUID propuestaId, UUID eventoId);

    /** @param estado null = todas. Las más nuevas primero. */
    List<Propuesta> listarPorEvento(UUID eventoId, EstadoPropuesta estado);

    /**
     * Borra las PENDIENTES de ese documento y guarda las nuevas, en UNA transacción
     * (decisión de Isaac: al volver a revisar, se reemplazan; las aceptadas y descartadas se quedan).
     */
    List<Propuesta> reemplazarPendientesDelDocumento(UUID documentoId, List<Propuesta> nuevas);

    /**
     * v2 (decisión de Isaac): una DESCARTADA vuelve a PENDIENTE. Una sola sentencia condicional
     * (WHERE estado = 'DESCARTADA'), así no se puede "recuperar" una aceptada ni recuperar dos veces.
     * @return false si no estaba descartada (o no existe en ese evento)
     */
    boolean recuperarDescartada(UUID propuestaId, UUID eventoId);
}
