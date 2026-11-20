package com.iris.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/** El "portero" de Iris: qué rutas son públicas, cuáles piden token y cómo se valida. */
@Configuration
@EnableConfigurationProperties({JwtPropiedades.class, CorsPropiedades.class})
public class SecurityConfig {

    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http
            // CORS: usa el bean corsConfigurationSource() de abajo. Va primero para que
            // las peticiones "preflight" (OPTIONS) del navegador no choquen con la seguridad
            .cors(Customizer.withDefaults())
            // CSRF protege sesiones basadas en cookies. Nosotros usamos token en un header
            // que el navegador NO envía solo, así que aquí no aplica.
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless: el servidor no crea sesiones; cada petición trae su token
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rutas -> rutas
                // Públicas: registro, login y la ruta de salud
                .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/salud").permitAll()
                // Autorización por rol: solo ADMIN (lo usaremos más adelante)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Todo lo demás: con token válido
                .anyRequest().authenticated())
            // Activa el filtro que lee "Authorization: Bearer <token>" y lo valida con el JwtDecoder
            .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorDeRoles())));

        return http.build();
    }

    /** Traduce el claim "rol": "ADMIN" del token a la autoridad "ROLE_ADMIN" que entiende hasRole(). */
    private JwtAuthenticationConverter convertidorDeRoles() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(roles);
        return convertidor;
    }

    /** Reglas CORS: qué frontends (orígenes) pueden llamar a la API desde un navegador. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsPropiedades propiedades) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(propiedades.origenes());   // lista exacta, nunca "*"
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setAllowCredentials(false);                  // usamos token en header, no cookies
        cors.setMaxAge(Duration.ofHours(1));              // el navegador recuerda el permiso 1 h

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);
        return fuente;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // costo por defecto: 10 (2^10 rondas)
    }

    @Bean
    SecretKey claveJwt(JwtPropiedades propiedades) {
        byte[] bytes = propiedades.secreto().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Firma tokens (lo usa el login). */
    @Bean
    JwtEncoder jwtEncoder(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    /** Verifica tokens (lo usa el filtro en cada petición). Misma clave, mismo algoritmo. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey claveJwt) {
        return NimbusJwtDecoder.withSecretKey(claveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
