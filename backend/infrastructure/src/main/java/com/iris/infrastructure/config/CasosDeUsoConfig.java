package com.iris.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.iris.application.agenda.CambiarEstadoTarea;
import com.iris.application.agenda.CrearSesion;
import com.iris.application.agenda.CrearTarea;
import com.iris.application.agenda.EditarSesion;
import com.iris.application.agenda.EditarTarea;
import com.iris.application.agenda.EliminarSesion;
import com.iris.application.agenda.EliminarTarea;
import com.iris.application.agenda.ListarSesiones;
import com.iris.application.agenda.ListarTareas;
import com.iris.application.auth.CrearAdministradorInicial;
import com.iris.application.auth.IniciarSesion;
import com.iris.application.auth.RegistrarUsuario;
import com.iris.application.consulta.BuscarFragmentos;
import com.iris.application.consulta.ConsultarIris;
import com.iris.application.consulta.HistorialConsultas;
import com.iris.application.documento.Fragmentador;
import com.iris.application.documento.ListarDocumentos;
import com.iris.application.documento.ProcesarDocumento;
import com.iris.application.documento.SubirDocumento;
import com.iris.application.evento.AccesoEvento;
import com.iris.application.evento.CrearEvento;
import com.iris.application.evento.ListarEventos;
import com.iris.application.port.out.AlmacenTemporal;
import com.iris.application.port.out.BuscadorFragmentos;
import com.iris.application.port.out.CodificadorPassword;
import com.iris.application.port.out.ColaIndexacion;
import com.iris.application.port.out.ContadorTokens;
import com.iris.application.port.out.ExtractorTexto;
import com.iris.application.port.out.GeneradorEmbeddings;
import com.iris.application.port.out.RedactorRespuestas;
import com.iris.application.port.out.GeneradorToken;
import com.iris.domain.agenda.SesionRepositorio;
import com.iris.domain.agenda.TareaRepositorio;
import com.iris.domain.consulta.ConsultaRepositorio;
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
    ConsultarIris consultarIris(BuscarFragmentos buscarFragmentos, RedactorRespuestas redactor,
                                ConsultaRepositorio consultas, ConsultaPropiedades propiedades) {
        return new ConsultarIris(buscarFragmentos, redactor, consultas, propiedades.umbralSimilitud());
    }

    @Bean
    HistorialConsultas historialConsultas(EventoRepositorio eventos, ConsultaRepositorio consultas) {
        return new HistorialConsultas(eventos, consultas);
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

    // ---------- Agenda (Día 4.1) ----------

    @Bean
    AccesoEvento accesoEvento(EventoRepositorio eventos) {
        return new AccesoEvento(eventos);
    }

    @Bean
    CrearSesion crearSesion(AccesoEvento acceso, SesionRepositorio sesiones) {
        return new CrearSesion(acceso, sesiones);
    }

    @Bean
    ListarSesiones listarSesiones(AccesoEvento acceso, SesionRepositorio sesiones) {
        return new ListarSesiones(acceso, sesiones);
    }

    @Bean
    EditarSesion editarSesion(AccesoEvento acceso, SesionRepositorio sesiones) {
        return new EditarSesion(acceso, sesiones);
    }

    @Bean
    EliminarSesion eliminarSesion(AccesoEvento acceso, SesionRepositorio sesiones) {
        return new EliminarSesion(acceso, sesiones);
    }

    @Bean
    CrearTarea crearTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        return new CrearTarea(acceso, tareas);
    }

    @Bean
    ListarTareas listarTareas(AccesoEvento acceso, TareaRepositorio tareas) {
        return new ListarTareas(acceso, tareas);
    }

    @Bean
    EditarTarea editarTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        return new EditarTarea(acceso, tareas);
    }

    @Bean
    CambiarEstadoTarea cambiarEstadoTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        return new CambiarEstadoTarea(acceso, tareas);
    }

    @Bean
    EliminarTarea eliminarTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        return new EliminarTarea(acceso, tareas);
    }
}
