/*
 * 6.3 Las propuestas de Iris: la pantalla que diferencia a Iris de un chat con documentos.
 * Iris leyó un documento y PROPONE tareas o sesiones, cada una con su FUENTE (documento,
 * páginas y la frase textual). Nada entra a la agenda sin el visto bueno de una persona:
 *   Aceptar              -> tal cual lo propuso Iris
 *   Ajustar y aceptar    -> corrigiendo título, fecha, horas o asignando responsable (Día 4.4)
 *   Descartar            -> queda como historial
 * Solo EDITORES y PROPIETARIOS (decisión de Isaac): un lector ni siquiera ve la pestaña.
 */
import { useCallback, useEffect, useState } from "react";
import { Link, useOutletContext } from "react-router";
import { api } from "../../api/cliente.js";
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

export default function Propuestas() {
  const { evento } = useOutletContext();
  const confirmar = useConfirmar();
  const base = `/api/eventos/${evento.id}`;

  const [estado, setEstado] = useState("PENDIENTE");
  const [propuestas, setPropuestas] = useState(null);
  const [documentos, setDocumentos] = useState([]);
  const [miembros, setMiembros] = useState([]);
  const [revisando, setRevisando] = useState(null); // id del documento que Iris está leyendo
  const [documentoElegido, setDocumentoElegido] = useState("");
  const [ajustando, setAjustando] = useState(null); // { id, ...campos } de la propuesta que se está ajustando
  const [ocupada, setOcupada] = useState(null);
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);

  const cargar = useCallback(
    () => api(`${base}/propuestas?estado=${estado}`).then(setPropuestas).catch((e) => setError(e.message)),
    [base, estado],
  );
  useEffect(() => {
    setPropuestas(null);
    cargar();
  }, [cargar]);

  useEffect(() => {
    api(`${base}/documentos`)
      .then((docs) => {
        const listos = docs.filter((d) => d.estado === "INDEXADO");
        setDocumentos(listos);
        if (listos.length) setDocumentoElegido(listos[0].id);
      })
      .catch(() => {});
    api(`${base}/miembros`).then(setMiembros).catch(() => {});
  }, [base]);

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
      setError(e.message);
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
      setError(e.message); // 409 si alguien ya la decidió; 400 si un ajuste es inválido
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
      setError(e.message);
    } finally {
      setOcupada(null);
    }
  }

  function abrirAjustes(p) {
    setAjustando({
      id: p.id,
      titulo: p.titulo,
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
    const ajustes = { titulo: a.titulo || null, fecha: a.fecha || null };
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
              disabled={!documentos.length || revisando}>
              {!documentos.length && <option value="">No hay documentos listos</option>}
              {documentos.map((d) => <option key={d.id} value={d.id}>{d.nombreArchivo}</option>)}
            </select>
          </label>
          <button className="boton boton-coral" onClick={revisar} disabled={!documentoElegido || revisando}>
            {revisando ? "Iris está leyendo…" : "Revisar documento"}
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

      {error && <p className="aviso-error" role="alert">{error}</p>}
      {aviso && <p className="chip propuestas-aviso" role="status">{aviso}</p>}
      {propuestas === null && <p className="cargando">Cargando propuestas…</p>}
      {propuestas?.length === 0 && (
        <div className="tarjeta propuestas-vacio">
          <h2>{estado === "PENDIENTE" ? "No tienes propuestas por revisar" : "Todavía no hay nada aquí"}</h2>
          <p className="texto-suave">
            {estado === "PENDIENTE" ? "Sube un contrato o un cronograma y lo leeré apenas termine de indexarse, o pídemelo arriba." : ""}
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
                    <button className="boton" onClick={() => descartar(p)} disabled={ocupada === p.id}>Descartar</button>
                    <button className="boton" onClick={() => abrirAjustes(p)} disabled={ocupada === p.id}>Ajustar y aceptar</button>
                    <button className="boton boton-coral" onClick={() => aceptar(p)} disabled={ocupada === p.id}>
                      {ocupada === p.id ? "Aceptando…" : "Aceptar"}
                    </button>
                  </div>
                )}

                {enAjuste && (
                  <form className="propuesta-ajustes" onSubmit={(e) => enviarAjustes(e, p)}>
                    <label className="etiqueta propuesta-ancho">Título<input className="campo" required value={ajustando.titulo} onChange={cambiar("titulo")} /></label>
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
