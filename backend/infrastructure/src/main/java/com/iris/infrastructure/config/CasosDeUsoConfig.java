package com.iris.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.iris.application.auth.CrearAdministradorInicial;
import com.iris.application.auth.IniciarSesion;
import com.iris.application.auth.RegistrarUsuario;
import com.iris.application.consulta.BuscarFragmentos;
import com.iris.application.documento.Fragmentador;
import com.iris.application.documento.ListarDocumentos;
import com.iris.application.documento.ProcesarDocumento;
import com.iris.application.documento.SubirDocumento;
import com.iris.application.evento.CrearEvento;
import com.iris.application.evento.ListarEventos;
import com.iris.application.port.out.AlmacenTemporal;
import com.iris.application.port.out.BuscadorFragmentos;
import com.iris.application.port.out.CodificadorPassword;
import com.iris.application.port.out.ColaIndexacion;
import com.iris.application.port.out.ContadorTokens;
import com.iris.application.port.out.ExtractorTexto;
import com.iris.application.port.out.GeneradorEmbeddings;
import com.iris.application.port.out.GeneradorToken;
import com.iris.domain.documento.DocumentoRepositorio;
import com.iris.domain.documento.FragmentoRepositorio;
import com.iris.domain.evento.EventoRepositorio;
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

    @Bean
    CrearEvento crearEvento(EventoRepositorio eventos) {
        return new CrearEvento(eventos);
    }

    @Bean
    ListarEventos listarEventos(EventoRepositorio eventos) {
        return new ListarEventos(eventos);
    }

    @Bean
    SubirDocumento subirDocumento(EventoRepositorio eventos, DocumentoRepositorio documentos,
                                  AlmacenTemporal almacen, ColaIndexacion cola,
                                  DocumentosPropiedades propiedades) {
        return new SubirDocumento(eventos, documentos, almacen, cola, propiedades.tamanoMaximo().toBytes());
    }

    @Bean
    BuscarFragmentos buscarFragmentos(EventoRepositorio eventos, GeneradorEmbeddings embeddings,
                                      BuscadorFragmentos buscador) {
        return new BuscarFragmentos(eventos, embeddings, buscador);
    }

    @Bean
    Fragmentador fragmentador(ContadorTokens contador) {
        return new Fragmentador(contador);
    }

    @Bean
    ProcesarDocumento procesarDocumento(DocumentoRepositorio documentos, FragmentoRepositorio fragmentos,
                                        AlmacenTemporal almacen, ExtractorTexto extractor,
                                        Fragmentador fragmentador, GeneradorEmbeddings embeddings) {
        return new ProcesarDocumento(documentos, fragmentos, almacen, extractor, fragmentador, embeddings);
    }

    @Bean
    ListarDocumentos listarDocumentos(EventoRepositorio eventos, DocumentoRepositorio documentos) {
        return new ListarDocumentos(eventos, documentos);
    }
}
