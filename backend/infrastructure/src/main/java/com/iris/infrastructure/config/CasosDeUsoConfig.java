package com.iris.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.iris.application.auth.CrearAdministradorInicial;
import com.iris.application.auth.IniciarSesion;
import com.iris.application.auth.RegistrarUsuario;
import com.iris.application.port.out.CodificadorPassword;
import com.iris.application.port.out.GeneradorToken;
import com.iris.domain.usuario.UsuarioRepositorio;

/**
 * Aquí "armamos" los casos de uso. Como application no tiene Spring (sin @Service),
 * infrastructure los crea con new y les inyecta los adaptadores que implementan sus puertos.
 */
@Configuration
public class CasosDeUsoConfig {

    @Bean
    RegistrarUsuario registrarUsuario(UsuarioRepositorio usuarios, CodificadorPassword codificador) {
        return new RegistrarUsuario(usuarios, codificador);
    }

    @Bean
    IniciarSesion iniciarSesion(UsuarioRepositorio usuarios, CodificadorPassword codificador,
                                GeneradorToken generadorToken) {
        return new IniciarSesion(usuarios, codificador, generadorToken);
    }

    @Bean
    CrearAdministradorInicial crearAdministradorInicial(UsuarioRepositorio usuarios,
                                                        CodificadorPassword codificador) {
        return new CrearAdministradorInicial(usuarios, codificador);
    }
}
