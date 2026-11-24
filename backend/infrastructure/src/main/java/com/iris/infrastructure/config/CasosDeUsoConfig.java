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
import com.iris.application.documento.EliminarDocumento;
import com.iris.application.documento.Fragmentador;
import com.iris.application.documento.ListarDocumentos;
import com.iris.application.documento.ProcesarDocumento;
import com.iris.application.documento.SubirDocumento;
import com.iris.application.evento.AccesoEvento;
import com.iris.application.evento.CambiarRolMiembro;
import com.iris.application.evento.ListarMiembros;
import com.iris.application.evento.QuitarMiembro;
import com.iris.application.evento.SalirDelEvento;
import com.iris.application.invitacion.AceptarInvitacion;
import com.iris.application.invitacion.CancelarInvitacion;
import com.iris.application.invitacion.InvitacionesDelUsuario;
import com.iris.application.invitacion.InvitarAlEvento;
import com.iris.application.invitacion.ListarInvitacionesDelEvento;
import com.iris.application.invitacion.MisInvitaciones;
import com.iris.application.invitacion.RechazarInvitacion;
import com.iris.application.notificacion.Avisos;
import com.iris.application.notificacion.EnviarRecordatorios;
import com.iris.application.notificacion.MisNotificaciones;
import com.iris.application.notificacion.VencerInvitaciones;
import com.iris.application.port.out.Notificador;
import com.iris.application.port.out.Transaccion;
import com.iris.application.propuesta.AceptarPropuesta;
import com.iris.application.propuesta.DescartarPropuesta;
import com.iris.application.propuesta.GenerarPropuestas;
import com.iris.application.propuesta.ListarPropuestas;
import com.iris.application.propuesta.RecuperarPropuesta;
import com.iris.application.propuesta.RevisarDocumento;
import com.iris.application.propuesta.RevisionesEnCurso;
import com.iris.application.evento.CrearEvento;
import com.iris.application.evento.ListarEventos;
import com.iris.application.port.out.AlmacenTemporal;
import com.iris.application.port.out.AnalizadorDocumentos;
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
import com.iris.domain.evento.MiembroRepositorio;
import com.iris.domain.invitacion.InvitacionRepositorio;
import com.iris.domain.notificacion.NotificacionRepositorio;
import com.iris.domain.notificacion.RecordatorioRepositorio;
import com.iris.domain.propuesta.PropuestaRepositorio;
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
                                        Fragmentador fragmentador, GeneradorEmbeddings embeddings, Avisos avisos) {
        return new ProcesarDocumento(documentos, fragmentos, almacen, extractor, fragmentador, embeddings, avisos);
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
    CrearSesion crearSesion(AccesoEvento acceso, SesionRepositorio sesiones, Avisos avisos) {
        return new CrearSesion(acceso, sesiones, avisos);
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
    CrearTarea crearTarea(AccesoEvento acceso, TareaRepositorio tareas, Avisos avisos) {
        return new CrearTarea(acceso, tareas, avisos);
    }

    @Bean
    ListarTareas listarTareas(AccesoEvento acceso, TareaRepositorio tareas) {
        return new ListarTareas(acceso, tareas);
    }

    @Bean
    EditarTarea editarTarea(AccesoEvento acceso, TareaRepositorio tareas, Avisos avisos) {
        return new EditarTarea(acceso, tareas, avisos);
    }

    @Bean
    CambiarEstadoTarea cambiarEstadoTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        return new CambiarEstadoTarea(acceso, tareas);
    }

    @Bean
    EliminarTarea eliminarTarea(AccesoEvento acceso, TareaRepositorio tareas) {
        return new EliminarTarea(acceso, tareas);
    }

    // --- Día 4.2: invitaciones y equipo ---

    @Bean
    InvitacionesDelUsuario invitacionesDelUsuario(UsuarioRepositorio usuarios, InvitacionRepositorio invitaciones) {
        return new InvitacionesDelUsuario(usuarios, invitaciones);
    }

    @Bean
    InvitarAlEvento invitarAlEvento(AccesoEvento acceso, MiembroRepositorio miembros,
                                    InvitacionRepositorio invitaciones, Transaccion transaccion,
                                    UsuarioRepositorio usuarios, Avisos avisos) {
        return new InvitarAlEvento(acceso, miembros, invitaciones, transaccion, usuarios, avisos);
    }

    @Bean
    ListarInvitacionesDelEvento listarInvitacionesDelEvento(AccesoEvento acceso, InvitacionRepositorio invitaciones) {
        return new ListarInvitacionesDelEvento(acceso, invitaciones);
    }

    @Bean
    CancelarInvitacion cancelarInvitacion(AccesoEvento acceso, InvitacionRepositorio invitaciones) {
        return new CancelarInvitacion(acceso, invitaciones);
    }

    @Bean
    MisInvitaciones misInvitaciones(InvitacionesDelUsuario delUsuario, InvitacionRepositorio invitaciones) {
        return new MisInvitaciones(delUsuario, invitaciones);
    }

    @Bean
    AceptarInvitacion aceptarInvitacion(InvitacionesDelUsuario delUsuario, InvitacionRepositorio invitaciones,
                                        MiembroRepositorio miembros, Transaccion transaccion, Avisos avisos) {
        return new AceptarInvitacion(delUsuario, invitaciones, miembros, transaccion, avisos);
    }

    @Bean
    RechazarInvitacion rechazarInvitacion(InvitacionesDelUsuario delUsuario, InvitacionRepositorio invitaciones) {
        return new RechazarInvitacion(delUsuario, invitaciones);
    }

    @Bean
    ListarMiembros listarMiembros(AccesoEvento acceso, MiembroRepositorio miembros) {
        return new ListarMiembros(acceso, miembros);
    }

    @Bean
    CambiarRolMiembro cambiarRolMiembro(AccesoEvento acceso, MiembroRepositorio miembros) {
        return new CambiarRolMiembro(acceso, miembros);
    }

    @Bean
    QuitarMiembro quitarMiembro(AccesoEvento acceso, MiembroRepositorio miembros) {
        return new QuitarMiembro(acceso, miembros);
    }

    @Bean
    SalirDelEvento salirDelEvento(AccesoEvento acceso, MiembroRepositorio miembros) {
        return new SalirDelEvento(acceso, miembros);
    }

    // --- Día 4.3: notificaciones y recordatorios ---

    @Bean
    Avisos avisos(MiembroRepositorio miembros, Notificador notificador) {
        return new Avisos(miembros, notificador);
    }

    @Bean
    EnviarRecordatorios enviarRecordatorios(TareaRepositorio tareas, RecordatorioRepositorio recordatorios,
                                            Avisos avisos) {
        return new EnviarRecordatorios(tareas, recordatorios, avisos);
    }

    @Bean
    VencerInvitaciones vencerInvitaciones(InvitacionRepositorio invitaciones) {
        return new VencerInvitaciones(invitaciones);
    }

    @Bean
    MisNotificaciones misNotificaciones(NotificacionRepositorio notificaciones) {
        return new MisNotificaciones(notificaciones);
    }

    // --- Día 4.4: propuestas de Iris ---

    @Bean
    GenerarPropuestas generarPropuestas(FragmentoRepositorio fragmentos, ContadorTokens contador,
                                        AnalizadorDocumentos analizador, PropuestaRepositorio propuestas,
                                        DocumentoRepositorio documentos, EventoRepositorio eventos, Avisos avisos,
                                        TareaRepositorio tareas, SesionRepositorio sesiones) {
        return new GenerarPropuestas(fragmentos, contador, analizador, propuestas, documentos, eventos, avisos,
                tareas, sesiones);
    }

    @Bean
    RevisarDocumento revisarDocumento(AccesoEvento acceso, DocumentoRepositorio documentos,
                                      GenerarPropuestas generar) {
        return new RevisarDocumento(acceso, documentos, generar);
    }

    @Bean
    ListarPropuestas listarPropuestas(AccesoEvento acceso, PropuestaRepositorio propuestas) {
        return new ListarPropuestas(acceso, propuestas);
    }

    @Bean
    RevisionesEnCurso revisionesEnCurso(AccesoEvento acceso, GenerarPropuestas generar) {
        return new RevisionesEnCurso(acceso, generar);
    }

    @Bean
    AceptarPropuesta aceptarPropuesta(AccesoEvento acceso, PropuestaRepositorio propuestas, TareaRepositorio tareas,
                                      SesionRepositorio sesiones, Transaccion transaccion, Avisos avisos) {
        return new AceptarPropuesta(acceso, propuestas, tareas, sesiones, transaccion, avisos);
    }

    @Bean
    DescartarPropuesta descartarPropuesta(AccesoEvento acceso, PropuestaRepositorio propuestas) {
        return new DescartarPropuesta(acceso, propuestas);
    }

    @Bean
    RecuperarPropuesta recuperarPropuesta(AccesoEvento acceso, PropuestaRepositorio propuestas) {
        return new RecuperarPropuesta(acceso, propuestas);
    }

    // --- Día 4.5 ---

    @Bean
    EliminarDocumento eliminarDocumento(AccesoEvento acceso, DocumentoRepositorio documentos,
                                        PropuestaRepositorio propuestas, AlmacenTemporal almacen,
                                        Transaccion transaccion) {
        return new EliminarDocumento(acceso, documentos, propuestas, almacen, transaccion);
    }
}
