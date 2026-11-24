package com.iris.infrastructure.adapter.in.web.evento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.evento.CambiarRolMiembro;
import com.iris.application.evento.ListarMiembros;
import com.iris.application.evento.QuitarMiembro;
import com.iris.application.evento.SalirDelEvento;
import com.iris.domain.evento.MiembroEvento;
import com.iris.domain.evento.RolEnEvento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** El equipo del evento: ver, cambiar roles, quitar personas y salirse. */
@RestController
@RequestMapping("/api/eventos/{eventoId}/miembros")
class MiembroController {

    private final ListarMiembros listar;
    private final CambiarRolMiembro cambiarRol;
    private final QuitarMiembro quitar;
    private final SalirDelEvento salir;

    MiembroController(ListarMiembros listar, CambiarRolMiembro cambiarRol, QuitarMiembro quitar,
                      SalirDelEvento salir) {
        this.listar = listar;
        this.cambiarRol = cambiarRol;
        this.quitar = quitar;
        this.salir = salir;
    }

    @GetMapping
    List<RespuestaMiembro> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId) {
        return listar.ejecutar(usuario(jwt), eventoId).stream().map(RespuestaMiembro::de).toList();
    }

    /** {"rol": "EDITOR"} */
    @PatchMapping("/{usuarioId}")
    RespuestaMiembro cambiarRol(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                                @PathVariable UUID usuarioId, @Valid @RequestBody SolicitudRol s) {
        return RespuestaMiembro.de(cambiarRol.ejecutar(usuario(jwt), eventoId, usuarioId, s.rol()));
    }

    /**
     * Salirme yo. Spring prefiere la ruta LITERAL "/yo" sobre el patrón "/{usuarioId}",
     * así que "yo" nunca se intenta convertir en UUID.
     */
    @DeleteMapping("/yo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void salir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId) {
        salir.ejecutar(usuario(jwt), eventoId);
    }

    @DeleteMapping("/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void quitar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId, @PathVariable UUID usuarioId) {
        quitar.ejecutar(usuario(jwt), eventoId, usuarioId);
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    record SolicitudRol(@NotNull RolEnEvento rol) {
    }

    record RespuestaMiembro(UUID usuarioId, String nombre, String email, String rol, Instant unidoEn) {

        static RespuestaMiembro de(MiembroEvento m) {
            return new RespuestaMiembro(m.usuarioId(), m.nombre(), m.email(), m.rol().name(), m.unidoEn());
        }
    }
}
