package com.iris.infrastructure.adapter.in.web.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.iris.application.auth.IniciarSesion;
import com.iris.application.auth.RegistrarUsuario;
import com.iris.application.port.out.GeneradorToken.TokenEmitido;
import com.iris.domain.usuario.Usuario;
import com.iris.infrastructure.adapter.in.web.auth.AuthDtos.RespuestaToken;
import com.iris.infrastructure.adapter.in.web.auth.AuthDtos.RespuestaUsuario;
import com.iris.infrastructure.adapter.in.web.auth.AuthDtos.SolicitudLogin;
import com.iris.infrastructure.adapter.in.web.auth.AuthDtos.SolicitudRegistro;

import jakarta.validation.Valid;

/** Adaptador de entrada: traduce HTTP <-> casos de uso. No tiene lógica de negocio. */
@RestController
class AuthController {

    private final RegistrarUsuario registrarUsuario;
    private final IniciarSesion iniciarSesion;

    AuthController(RegistrarUsuario registrarUsuario, IniciarSesion iniciarSesion) {
        this.registrarUsuario = registrarUsuario;
        this.iniciarSesion = iniciarSesion;
    }

    @PostMapping("/api/auth/registro")
    @ResponseStatus(HttpStatus.CREATED) // 201: se creó un recurso
    RespuestaUsuario registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
        Usuario usuario = registrarUsuario.ejecutar(
                new RegistrarUsuario.Comando(solicitud.nombre(), solicitud.email(), solicitud.password()));
        return new RespuestaUsuario(usuario.id(), usuario.nombre(), usuario.email().valor(), usuario.rol().name());
    }

    @PostMapping("/api/auth/login")
    RespuestaToken login(@Valid @RequestBody SolicitudLogin solicitud) {
        TokenEmitido token = iniciarSesion.ejecutar(solicitud.email(), solicitud.password());
        return new RespuestaToken(token.valor(), "Bearer", token.expiraEn());
    }

    /**
     * Ruta protegida de prueba. @AuthenticationPrincipal entrega el JWT YA validado por el filtro:
     * si llegamos aquí, la firma era correcta y no estaba vencido.
     */
    @GetMapping("/api/yo")
    Map<String, Object> yo(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "id", jwt.getSubject(),
                "email", jwt.getClaimAsString("email"),
                "rol", jwt.getClaimAsString("rol"),
                "expiraEn", jwt.getExpiresAt());
    }
}
