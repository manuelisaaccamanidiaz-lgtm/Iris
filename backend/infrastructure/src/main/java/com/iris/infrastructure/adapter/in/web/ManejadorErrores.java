package com.iris.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.iris.application.auth.CredencialesInvalidasException;
import com.iris.domain.compartido.ReglaDeNegocioException;
import com.iris.domain.usuario.EmailYaRegistradoException;

/**
 * Convierte excepciones en respuestas HTTP con formato estándar ProblemDetail (RFC 9457):
 * {"status": 400, "title": "...", "detail": "..."}. Spring elige el handler más específico.
 */
@RestControllerAdvice
class ManejadorErrores {

    @ExceptionHandler(EmailYaRegistradoException.class)
    ProblemDetail emailDuplicado(EmailYaRegistradoException e) {
        return problema(HttpStatus.CONFLICT, "Email ya registrado", e.getMessage()); // 409
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    ProblemDetail reglaDeNegocio(ReglaDeNegocioException e) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos", e.getMessage()); // 400
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credenciales(CredencialesInvalidasException e) {
        return problema(HttpStatus.UNAUTHORIZED, "No autorizado", e.getMessage()); // 401
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException e) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos", "Faltan campos obligatorios");
    }

    private ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(estado, detalle);
        p.setTitle(titulo);
        return p;
    }
}
