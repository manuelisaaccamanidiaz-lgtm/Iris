package com.iris.application.evento;

import java.util.UUID;

import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.evento.EventoDeMiembro;
import com.iris.domain.evento.MiembroEvento;
import com.iris.domain.evento.MiembroRepositorio;
import com.iris.domain.evento.RolEnEvento;

/** Caso de uso: un propietario cambia el rol de otro miembro (ascender a PROPIETARIO: solo el creador). */
public class CambiarRolMiembro {

    private final AccesoEvento acceso;
    private final MiembroRepositorio miembros;

    public CambiarRolMiembro(AccesoEvento acceso, MiembroRepositorio miembros) {
        this.acceso = acceso;
        this.miembros = miembros;
    }

    public MiembroEvento ejecutar(UUID yo, UUID eventoId, UUID usuarioId, RolEnEvento nuevoRol) {
        if (nuevoRol == null) {
            throw new ReglaDeNegocioException("El rol es obligatorio (EDITOR, LECTOR o PROPIETARIO)");
        }
        EventoDeMiembro evento = acceso.comoMiembro(eventoId, yo);
        // Primero el permiso y DESPUÉS buscar al objetivo: un LECTOR recibe 403,
        // no un 404 que le permitiría "tantear" quién está en el equipo
        evento.exigirAdministracion();
        MiembroEvento objetivo = miembros.buscar(eventoId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Miembro no encontrado"));
        evento.exigirPuedeCambiarRol(objetivo, nuevoRol, yo);

        miembros.cambiarRol(eventoId, usuarioId, nuevoRol);
        return new MiembroEvento(objetivo.eventoId(), objetivo.usuarioId(), objetivo.nombre(),
                objetivo.email(), nuevoRol, objetivo.unidoEn(), objetivo.archivadoEn());
    }
}
