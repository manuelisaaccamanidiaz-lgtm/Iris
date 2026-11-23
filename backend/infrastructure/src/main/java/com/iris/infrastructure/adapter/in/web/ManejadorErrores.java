package com.iris.infrastructure.adapter.in.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.iris.application.auth.CredencialesInvalidasException;
import com.iris.domain.compartido.AccesoDenegadoException;
import com.iris.domain.compartido.RecursoNoEncontradoException;
import com.iris.domain.compartido.ConflictoException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.documento.DocumentoDuplicadoException;
import com.iris.domain.evento.EventoDuplicadoException;
import com.iris.domain.usuario.EmailYaRegistradoException;

/**
 * Convierte excepciones en respuestas HTTP con formato estándar ProblemDetail (RFC 9457):
 * {"status": 400, "title": "...", "detail": "..."}. Spring elige el handler más específico.
 */
@RestControllerAdvice
class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(EmailYaRegistradoException.class)
    ProblemDetail emailDuplicado(EmailYaRegistradoException e) {
        return problema(HttpStatus.CONFLICT, "Email ya registrado", e.getMessage()); // 409
    }

    @ExceptionHandler(DocumentoDuplicadoException.class)
    ProblemDetail documentoDuplicado(DocumentoDuplicadoException e) {
        return problema(HttpStatus.CONFLICT, "Documento duplicado", e.getMessage()); // 409
    }

    /** Llegó la petición, pero sin el archivo en el campo "archivo" (o llegó como texto, no como archivo) */
    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    ProblemDetail faltaArchivo(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "Falta el archivo",
                "Envía el PDF como archivo en el campo 'archivo' (multipart/form-data)");
    }

    @ExceptionHandler(EventoDuplicadoException.class)
    ProblemDetail eventoDuplicado(EventoDuplicadoException e) {
        return problema(HttpStatus.CONFLICT, "Evento duplicado", e.getMessage()); // 409
    }

    /** Spring corta el archivo antes de llegar al controlador si supera spring.servlet.multipart.max-file-size */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail archivoMuyGrande(MaxUploadSizeExceededException e) {
        return problema(HttpStatus.CONTENT_TOO_LARGE, "Archivo muy grande", "El PDF supera el tamaño máximo permitido"); // 413
    }

    /** Red de seguridad: si dos peticiones simultáneas violan un UNIQUE, la BD gana y respondemos 409 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflictoDeDatos(DataIntegrityViolationException e) {
        // OJO: Spring también usa esta excepción para errores de DATOS de PostgreSQL (códigos 22xxx),
        // no solo para UNIQUE. Siempre registramos la causa real: un error que no se ve no se puede arreglar
        log.warn("Error de integridad/datos en la BD: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de datos", "La operación choca con datos existentes o inválidos");
    }

    /** El cuerpo de la petición no es un JSON válido (o le falta) -> 400, no 500 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail jsonInvalido(HttpMessageNotReadableException e) {
        return problema(HttpStatus.BAD_REQUEST, "JSON inválido", "El cuerpo de la petición no es un JSON válido");
    }

    /** Un parámetro con formato incorrecto, por ejemplo un id que no es UUID en la URL -> 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parámetro inválido", "El parámetro '" + e.getName() + "' tiene un formato inválido");
    }

    /** Cualquier otro error de base de datos (SQL mal formado, conexión caída...): 500 y al log */
    @ExceptionHandler(DataAccessException.class)
    ProblemDetail errorBaseDeDatos(DataAccessException e) {
        log.error("Error de base de datos", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error consultando la base de datos");
    }

    /** Última red: lo que nadie más atrapó. Nunca enviamos detalles internos al cliente, pero SÍ al log */
    @ExceptionHandler(Exception.class)
    ProblemDetail errorInesperado(Exception e) {
        // Los errores propios de Spring MVC (ruta inexistente 404, método no permitido 405...)
        // ya traen su código y su ProblemDetail: los devolvemos tal cual
        if (e instanceof ErrorResponse respuestaSpring) {
            return respuestaSpring.getBody();
        }
        log.error("Error inesperado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado");
    }

    /**
     * 409 para todo conflicto del dominio (ya es miembro, invitación ya respondida...).
     * ConflictoException HEREDA de ReglaDeNegocioException: Spring elige el handler de la clase
     * más cercana, así que este gana al de 400 sin tener que tocar el de abajo.
     */
    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException e) {
        return problema(HttpStatus.CONFLICT, "Conflicto", e.getMessage()); // 409
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    ProblemDetail reglaDeNegocio(ReglaDeNegocioException e) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos", e.getMessage()); // 400
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", e.getMessage()); // 404
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ProblemDetail accesoDenegado(AccesoDenegadoException e) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", e.getMessage()); // 403
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credenciales(CredencialesInvalidasException e) {
        return problema(HttpStatus.UNAUTHORIZED, "No autorizado", e.getMessage()); // 401
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException e) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "Faltan campos obligatorios o tienen formato inválido");
    }

    private ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setTitle(titulo);
        return p;
    }
}
