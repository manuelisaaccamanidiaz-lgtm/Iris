/*
 * 6.3 Las propuestas de Iris: la pantalla que diferencia a Iris de un chat con documentos.
 * Iris leyó un documento y PROPONE tareas o sesiones, cada una con su FUENTE (documento,
 * páginas y la frase textual). Nada entra a la agenda sin el visto bueno de una persona:
 *   Aceptar              -> tal cual lo propuso Iris
 *   Ajustar y aceptar    -> corrigiendo título, fecha, horas o asignando responsable (Día 4.4)
 *   Descartar            -> queda como historial
 * Solo EDITORES y PROPIETARIOS (decisión de Isaac): un lector ni siquiera ve la pestaña.
 */
import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useOutletContext } from "react-router";
import { api, mensajeLocal } from "../../api/cliente.js";
import { useAvisos } from "../../componentes/Avisos.jsx";
import { useConfirmar } from "../../componentes/Confirmacion.jsx";
import { fecha, hora } from "../../utilidades/fechas.js";
import "./Propuestas.css";

const ESTADOS = [
  { valor: "PENDIENTE", texto: "Por revisar" },
  { valor: "ACEPTADA", texto: "Aceptadas" },
  { valor: "DESCARTADA", texto: "Descartadas" },
];
const TIPO_SESION = { CONFERENCIA: "Conferencia", TALLER: "Taller", PANEL: "Panel", REUNION: "Reunión", LOGISTICA: "Logística", OTRO: "Otro" };

/** Agrupa por documento de origen, conservando el orden en que llegan. */
function porDocumento(propuestas) {
  const grupos = new Map();
  for (const p of propuestas) {
    const clave = p.fuente.documento;
    if (!grupos.has(clave)) grupos.set(clave, []);
    grupos.get(clave).push(p);
  }
  return [...grupos];
}

const paginas = (f) => (f.paginaInicio === f.paginaFin ? `pág. ${f.paginaInicio}` : `págs. ${f.paginaInicio}-${f.paginaFin}`);

/**
 * Guardia de la ruta: un LECTOR que escribe /propuestas en la URL (o hereda la URL de otra sesión)
 * vuelve al chat SIN pedir nada al backend. El backend igual respondería 403: esto es la "comodidad";
 * la "seguridad" sigue en el backend.
 */
export default function PropuestasProtegida() {
  const { puedeEditar } = useOutletContext();
  const avisos = useAvisos();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const yaAvisado = useRef(false); // en desarrollo, StrictMode corre los efectos DOS veces: sin esto, volveríamos 2 pasos

  useEffect(() => {
    if (puedeEditar || yaAvisado.current) return;
    yaAvisado.current = true;
    avisos.error("No tienes permiso", "Las propuestas de Iris son solo para editores y propietarios de este evento.");
    // key "default" = es la PRIMERA página que abriste en Iris (pegaste la URL): no hay "atrás" al que volver
    if (ubicacion.key !== "default") navegar(-1);
    else navegar("../chat", { replace: true });
  }, [puedeEditar, avisos, navegar, ubicacion.key]);

  return puedeEditar ? <Propuestas /> : null;
}

const CADA_MS = 3000; // mientras Iris lee, preguntamos cada 3 s (igual que Documentos al indexar)

function Propuestas() {
  const { evento } = useOutletContext();
  // Si llegaste desde Documentos con "Ver propuestas", trae el documento para dejarlo elegido
  const documentoDeOrigen = useLocation().state?.documentoId;
  const confirmar = useConfirmar();
  const base = `/api/eventos/${evento.id}`;

  const [estado, setEstado] = useState("PENDIENTE");
  const [propuestas, setPropuestas] = useState(null);
  const [documentos, setDocumentos] = useState([]);
  const [miembros, setMiembros] = useState([]);
  const [revisando, setRevisando] = useState(null); // id del documento que Iris está leyendo
  const [documentoElegido, setDocumentoElegido] = useState(documentoDeOrigen ?? "");
  // Lo que Iris está leyendo AHORA: documentos que se indexan (y luego ella revisa sola) o que está revisando
  const [leyendo, setLeyendo] = useState([]); // [{ id, nombre }]
  const [ajustando, setAjustando] = useState(null); // { id, ...campos } de la propuesta que se está ajustando
  const [ocupada, setOcupada] = useState(null);
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);

  const cargar = useCallback(
    () => api(`${base}/propuestas?estado=${estado}`).then(setPropuestas).catch((e) => setError(mensajeLocal(e))),
    [base, estado],
  );
  useEffect(() => {
    setPropuestas(null);
    cargar();
  }, [cargar]);

  /** Documentos listos + qué está leyendo Iris. Las dos preguntas van JUNTAS (Promise.all). */
  const consultarLectura = useCallback(async () => {
    const [docs, enRevision] = await Promise.all([api(`${base}/documentos`), api(`${base}/propuestas/en-revision`)]);
    const listos = docs.filter((d) => d.estado === "INDEXADO");
    setDocumentos(listos);
    setDocumentoElegido((actual) => (listos.some((d) => d.id === actual) ? actual : listos[0]?.id ?? ""));
    // PROCESANDO también cuenta: al terminar de indexar, Iris lo revisa sola
    setLeyendo(docs.filter((d) => d.estado === "PROCESANDO" || enRevision.includes(d.id))
      .map((d) => ({ id: d.id, nombre: d.nombreArchivo })));
  }, [base]);

  useEffect(() => {
    consultarLectura().catch(() => {});
    api(`${base}/miembros`).then(setMiembros).catch(() => {});
  }, [base, consultarLectura]);

  // Polling SOLO mientras Iris lee algo. Cuando termina (de "leyendo algo" a "nada"), recargamos solos
  const estabaLeyendo = useRef(false);
  useEffect(() => {
    const hayLectura = leyendo.length > 0;
    if (estabaLeyendo.current && !hayLectura) {
      setAviso("Iris terminó de leer. Estas son sus propuestas.");
      setEstado("PENDIENTE");
      cargar();
    }
    estabaLeyendo.current = hayLectura;
    if (!hayLectura) return;
    const id = setTimeout(() => consultarLectura().catch(() => {}), CADA_MS);
    return () => clearTimeout(id); // limpieza: si sales de la pestaña, se deja de preguntar
  }, [leyendo, consultarLectura, cargar]);

  const elegidoOcupado = leyendo.some((d) => d.id === documentoElegido);

  function limpiar() {
    setError(null);
    setAviso(null);
  }

  /** "Iris, revisa este documento": tarda unos segundos (la IA lee el documento completo, con tope). */
  async function revisar() {
    limpiar();
    setRevisando(documentoElegido);
    try {
      const nuevas = await api(`${base}/documentos/${documentoElegido}/revisar`, { metodo: "POST" });
      setAviso(nuevas.length
        ? `Listo, leí el documento y te propongo ${nuevas.length} ${nuevas.length === 1 ? "acción" : "acciones"}.`
        : "Leí el documento y no encontré acciones concretas que proponer.");
      setEstado("PENDIENTE");
      cargar();
    } catch (e) {
      if (e.estado === 409) {
        // Ya lo estaba leyendo (la revisión automática): no es un error, solo esperamos con ella
        setAviso(e.message);
        consultarLectura().catch(() => {});
      } else {
        setError(mensajeLocal(e));
      }
    } finally {
      setRevisando(null);
    }
  }

  /** Acepta tal cual (cuerpo vacío) o con los ajustes del formulario. */
  async function aceptar(p, ajustes) {
    limpiar();
    setOcupada(p.id);
    try {
      await api(`${base}/propuestas/${p.id}/aceptar`, ajustes ? { metodo: "POST", cuerpo: ajustes } : { metodo: "POST" });
      setPropuestas((lista) => lista.filter((x) => x.id !== p.id));
      setAjustando(null);
      setAviso(p.tipo === "TAREA" ? `Listo, agregué «${ajustes?.titulo ?? p.titulo}» a las tareas.` : `Listo, agendé «${ajustes?.titulo ?? p.titulo}».`);
    } catch (e) {
      setError(mensajeLocal(e)); // 409 si alguien ya la decidió; 400 si un ajuste es inválido
    } finally {
      setOcupada(null);
    }
  }

  /** v2: una descartada vuelve a "Por revisar". */
  async function recuperar(p) {
    limpiar();
    setOcupada(p.id);
    try {
      await api(`${base}/propuestas/${p.id}/recuperar`, { metodo: "POST" });
      setPropuestas((lista) => lista.filter((x) => x.id !== p.id));
      setAviso(`«${p.titulo}» volvió a Por revisar.`);
    } catch (e) {
      setError(mensajeLocal(e));
    } finally {
      setOcupada(null);
    }
  }

  async function descartar(p) {
    const ok = await confirmar({
      titulo: `¿Descartar «${p.titulo}»?`,
      mensaje: "No se crea nada. Queda en la pestaña Descartadas como historial.",
      accion: "Descartar",
    });
    if (!ok) return;
    limpiar();
    setOcupada(p.id);
    try {
      await api(`${base}/propuestas/${p.id}/descartar`, { metodo: "POST" });
      setPropuestas((lista) => lista.filter((x) => x.id !== p.id));
    } catch (e) {
      setError(mensajeLocal(e));
    } finally {
      setOcupada(null);
    }
  }

  function abrirAjustes(p) {
    setAjustando({
      id: p.id,
      titulo: p.titulo,
      descripcion: p.descripcion ?? "",
      fecha: p.fecha ?? "",
      horaInicio: p.horaInicio?.slice(0, 5) ?? "",
      horaFin: p.horaFin?.slice(0, 5) ?? "",
      sala: p.sala ?? "",
      responsableId: "",
    });
  }

  function enviarAjustes(e, p) {
    e.preventDefault();
    const a = ajustando;
    // Solo mandamos lo que tiene valor: el backend deja como estaba lo que llegue en null
    const ajustes = { titulo: a.titulo || null, descripcion: a.descripcion || null, fecha: a.fecha || null };
    if (p.tipo === "SESION") Object.assign(ajustes, { horaInicio: a.horaInicio || null, horaFin: a.horaFin || null, sala: a.sala || null });
    else ajustes.responsableId = a.responsableId || null;
    aceptar(p, ajustes);
  }

  const cambiar = (campo) => (e) => setAjustando({ ...ajustando, [campo]: e.target.value });

  return (
    <div className="propuestas">
      <div className="propuestas-presentacion">
        <p className="burbuja-iris-grande">
          Leo los documentos del evento y te propongo tareas y sesiones, siempre con la frase exacta de donde las saqué.
          <strong> Nada entra a la agenda sin tu visto bueno.</strong>
        </p>
        <div className="propuestas-revisar tarjeta">
          <label className="etiqueta">
            Pedirle a Iris que revise
            <select className="campo" value={documentoElegido} onChange={(e) => setDocumentoElegido(e.target.value)}
              disabled={!documentos.length || Boolean(revisando)}>
              {!documentos.length && <option value="">No hay documentos listos</option>}
              {documentos.map((d) => <option key={d.id} value={d.id}>{d.nombreArchivo}</option>)}
            </select>
          </label>
          <button className="boton boton-coral" onClick={revisar}
            disabled={!documentoElegido || Boolean(revisando) || elegidoOcupado}>
            {revisando || elegidoOcupado ? "Iris está leyendo…" : "Revisar documento"}
          </button>
          <p className="texto-suave">Al volver a revisar, reemplazo las propuestas pendientes de ese documento. Las que ya decidiste se quedan.</p>
        </div>
      </div>

      <div className="tareas-filtros" role="group" aria-label="Filtrar propuestas">
        {ESTADOS.map((e) => (
          <button key={e.valor} className={`boton ${estado === e.valor ? "filtro-activo" : ""}`}
            aria-pressed={estado === e.valor} onClick={() => setEstado(e.valor)}>
            {e.texto}
          </button>
        ))}
      </div>

      {leyendo.length > 0 && (
        <p className="chip chip-coral propuestas-leyendo" role="status">
          Iris está leyendo {leyendo.map((d) => `«${d.nombre}»`).join(", ")}… Sus propuestas aparecerán aquí solas.
        </p>
      )}
      {error && <p className="aviso-error" role="alert">{error}</p>}
      {aviso && <p className="chip propuestas-aviso" role="status">{aviso}</p>}
      {propuestas === null && <p className="cargando">Cargando propuestas…</p>}
      {propuestas?.length === 0 && (
        <div className="tarjeta propuestas-vacio">
          <h2>{estado === "PENDIENTE" ? "No tienes propuestas por revisar" : "Todavía no hay nada aquí"}</h2>
          <p className="texto-suave">
            {estado === "PENDIENTE"
              ? "Leo cada documento sola apenas termina de indexarse. Si no ves nada, es que no encontré acciones nuevas: no repito lo que ya existe en el evento ni lo que descartaste."
              : ""}
          </p>
        </div>
      )}

      {propuestas && porDocumento(propuestas).map(([documento, lista]) => (
        <section key={documento} className="propuestas-grupo">
          <h2 className="propuestas-documento">{documento} <span className="texto-suave">· {lista.length}</span></h2>
          {lista.map((p) => {
            const esSesion = p.tipo === "SESION";
            const enAjuste = ajustando?.id === p.id;
            return (
              <article key={p.id} className="tarjeta propuesta">
                <div className="propuesta-cabecera">
                  {esSesion ? (
                    <span className={`chip sesion-tipo tipo-${p.tipoSesion ?? "OTRO"}`}>
                      Sesión · {TIPO_SESION[p.tipoSesion] ?? "Otro"}
                    </span>
                  ) : (
                    <span className="chip">Tarea</span>
                  )}
                  <h3>{p.titulo}</h3>
                </div>
                {p.descripcion && <p className="propuesta-descripcion">{p.descripcion}</p>}
                <p className="texto-suave propuesta-cuando">
                  {p.fecha ? (esSesion ? `${fecha(p.fecha)}, ${hora(p.horaInicio)} a ${hora(p.horaFin)}` : `Para el ${fecha(p.fecha)}`) : "Sin fecha en el documento"}
                  {p.sala && ` · ${p.sala}`}
                </p>

                {/* La FUENTE: lo que hace que Iris no sea "una IA que inventa" */}
                <blockquote className="propuesta-cita">
                  <p>«{p.fuente.cita}»</p>
                  <cite>{p.fuente.documento}, {paginas(p.fuente)}</cite>
                </blockquote>

                {estado === "PENDIENTE" && !enAjuste && (
                  <div className="propuesta-acciones">
                    <button className="boton boton-texto-coral" onClick={() => descartar(p)} disabled={ocupada === p.id}>Descartar</button>
                    <button className="boton" onClick={() => abrirAjustes(p)} disabled={ocupada === p.id}>Ajustar y aceptar</button>
                    <button className="boton boton-coral" onClick={() => aceptar(p)} disabled={ocupada === p.id}>
                      {ocupada === p.id ? "Aceptando…" : "Aceptar"}
                    </button>
                  </div>
                )}

                {enAjuste && (
                  <form className="propuesta-ajustes" onSubmit={(e) => enviarAjustes(e, p)}>
                    <label className="etiqueta propuesta-ancho">Título<input className="campo" required value={ajustando.titulo} onChange={cambiar("titulo")} /></label>
                    <label className="etiqueta propuesta-ancho">Detalle<textarea className="campo" rows={2} value={ajustando.descripcion} onChange={cambiar("descripcion")} /></label>
                    <label className="etiqueta">{esSesion ? "Día" : "Fecha límite"}
                      <input className="campo" type="date" required={esSesion} value={ajustando.fecha} onChange={cambiar("fecha")}
                        min={esSesion ? evento.fechaInicio : undefined} max={esSesion ? evento.fechaFin : undefined} />
                    </label>
                    {esSesion ? (
                      <>
                        <label className="etiqueta">Inicio<input className="campo" type="time" required value={ajustando.horaInicio} onChange={cambiar("horaInicio")} /></label>
                        <label className="etiqueta">Fin<input className="campo" type="time" required value={ajustando.horaFin} onChange={cambiar("horaFin")} /></label>
                        <label className="etiqueta">Sala<input className="campo" value={ajustando.sala} onChange={cambiar("sala")} /></label>
                      </>
                    ) : (
                      <label className="etiqueta">Responsable
                        <select className="campo" value={ajustando.responsableId} onChange={cambiar("responsableId")}>
                          <option value="">Sin responsable</option>
                          {miembros.map((m) => <option key={m.usuarioId} value={m.usuarioId}>{m.nombre}</option>)}
                        </select>
                      </label>
                    )}
                    <div className="propuesta-acciones propuesta-ancho">
                      <button type="button" className="boton" onClick={() => setAjustando(null)}>Cancelar</button>
                      <button className="boton boton-coral" disabled={ocupada === p.id}>
                        {esSesion ? "Agendar con estos cambios" : "Crear la tarea"}
                      </button>
                    </div>
                  </form>
                )}

                {estado === "DESCARTADA" && (
                  <div className="propuesta-acciones">
                    <button className="boton" onClick={() => recuperar(p)} disabled={ocupada === p.id}>
                      Recuperar
                    </button>
                  </div>
                )}

                {estado === "ACEPTADA" && (
                  <Link className="propuesta-enlace" to={`../${esSesion ? "agenda" : "tareas"}`}>
                    {esSesion ? "Ver en la agenda" : "Ver en las tareas"}
                  </Link>
                )}
              </article>
            );
          })}
        </section>
      ))}
    </div>
  );
}
