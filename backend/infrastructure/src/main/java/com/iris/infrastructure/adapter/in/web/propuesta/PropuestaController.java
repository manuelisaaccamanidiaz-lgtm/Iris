package com.iris.infrastructure.adapter.in.web.propuesta;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.propuesta.AceptarPropuesta;
import com.iris.application.propuesta.DescartarPropuesta;
import com.iris.application.propuesta.ListarPropuestas;
import com.iris.application.propuesta.RecuperarPropuesta;
import com.iris.application.propuesta.RevisarDocumento;
import com.iris.domain.propuesta.EstadoPropuesta;
import com.iris.domain.propuesta.Propuesta;

/** Las propuestas de Iris de un evento: revisar un documento, ver, aceptar (con cambios) y descartar. */
@RestController
@RequestMapping("/api/eventos/{eventoId}")
class PropuestaController {

    private final RevisarDocumento revisar;
    private final ListarPropuestas listar;
    private final AceptarPropuesta aceptar;
    private final DescartarPropuesta descartar;
    private final RecuperarPropuesta recuperar;

    PropuestaController(RevisarDocumento revisar, ListarPropuestas listar, AceptarPropuesta aceptar,
                        DescartarPropuesta descartar, RecuperarPropuesta recuperar) {
        this.revisar = revisar;
        this.listar = listar;
        this.aceptar = aceptar;
        this.descartar = descartar;
        this.recuperar = recuperar;
    }

    /** "Iris, revisa este documento". Tarda unos segundos: la IA lee el documento completo (con tope). */
    @PostMapping("/documentos/{documentoId}/revisar")
    List<RespuestaPropuesta> revisar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                                     @PathVariable UUID documentoId) {
        return revisar.ejecutar(usuario(jwt), eventoId, documentoId).stream().map(RespuestaPropuesta::de).toList();
    }

    /** ?estado=PENDIENTE (por defecto), ACEPTADA o DESCARTADA. */
    @GetMapping("/propuestas")
    List<RespuestaPropuesta> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                                    @RequestParam(defaultValue = "PENDIENTE") EstadoPropuesta estado) {
        return listar.ejecutar(usuario(jwt), eventoId, estado).stream().map(RespuestaPropuesta::de).toList();
    }

    /**
     * El cuerpo es OPCIONAL: sin cuerpo se acepta tal cual; con cuerpo, cada campo que venga
     * reemplaza al de Iris. Ejemplo: {"fecha": "2026-12-01", "responsableId": "..."}
     */
    @PostMapping("/propuestas/{propuestaId}/aceptar")
    @ResponseStatus(HttpStatus.CREATED) // se CREA una tarea o una sesión
    RespuestaPropuesta aceptar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                               @PathVariable UUID propuestaId,
                               @RequestBody(required = false) SolicitudAceptar s) {
        Propuesta.Ajustes ajustes = s == null ? null
                : new Propuesta.Ajustes(s.titulo(), s.descripcion(), s.fecha(), s.horaInicio(), s.horaFin(), s.sala());
        return RespuestaPropuesta.de(aceptar.ejecutar(usuario(jwt), eventoId, propuestaId, ajustes,
                s == null ? null : s.responsableId()));
    }

    @PostMapping("/propuestas/{propuestaId}/descartar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void descartar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId, @PathVariable UUID propuestaId) {
        descartar.ejecutar(usuario(jwt), eventoId, propuestaId);
    }

    /** v2: una descartada vuelve a "Por revisar". */
    @PostMapping("/propuestas/{propuestaId}/recuperar")
    RespuestaPropuesta recuperar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                                 @PathVariable UUID propuestaId) {
        return RespuestaPropuesta.de(recuperar.ejecutar(usuario(jwt), eventoId, propuestaId));
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    record SolicitudAceptar(String titulo, String descripcion, LocalDate fecha, LocalTime horaInicio,
                            LocalTime horaFin, String sala, UUID responsableId) {
    }

    /** Fíjate en "fuente": toda propuesta dice de qué documento, qué páginas y qué frase la justifica. */
    record RespuestaPropuesta(UUID id, String tipo, String estado, String titulo, String descripcion,
                              LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String sala,
                              String tipoSesion, Fuente fuente, Instant creadaEn, UUID resultadoId) {

        record Fuente(UUID documentoId, String documento, int paginaInicio, int paginaFin, String cita) {
        }

        static RespuestaPropuesta de(Propuesta p) {
            return new RespuestaPropuesta(p.id(), p.tipo().name(), p.estado().name(), p.titulo(), p.descripcion(),
                    p.fecha(), p.horaInicio(), p.horaFin(), p.sala(),
                    p.tipoSesion() == null ? null : p.tipoSesion().name(),
                    new Fuente(p.documentoId(), p.documentoNombre(), p.paginaInicio(), p.paginaFin(), p.cita()),
                    p.creadaEn(), p.resultadoId());
        }
    }
}
