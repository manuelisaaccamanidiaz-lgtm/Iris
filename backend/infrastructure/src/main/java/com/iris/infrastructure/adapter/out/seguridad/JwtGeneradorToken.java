package com.iris.infrastructure.adapter.out.seguridad;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.iris.application.port.out.GeneradorToken;
import com.iris.domain.usuario.Usuario;
import com.iris.infrastructure.config.JwtPropiedades;

/** Adaptador: implementa el puerto GeneradorToken emitiendo un JWT firmado con HS256. */
@Component
class JwtGeneradorToken implements GeneradorToken {

    private final JwtEncoder encoder;
    private final JwtPropiedades propiedades;

    JwtGeneradorToken(JwtEncoder encoder, JwtPropiedades propiedades) {
        this.encoder = encoder;
        this.propiedades = propiedades;
    }

    @Override
    public TokenEmitido generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(propiedades.duracion());

        // El payload: SOLO datos no sensibles (recuerda: cualquiera puede leerlo)
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("iris")                          // quién lo emitió
                .subject(usuario.id().toString())        // a quién pertenece: el "quién" del multi-tenant
                .issuedAt(ahora)
                .expiresAt(expira)                       // el filtro rechaza tokens vencidos solo
                .claim("email", usuario.email().valor())
                .claim("rol", usuario.rol().name())
                .build();

        // Hay que decir el algoritmo explícitamente: por defecto Spring asume RS256 (clave pública)
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, expira);
    }
}
