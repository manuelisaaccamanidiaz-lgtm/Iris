/*
 * 6.2 Las tareas del evento.
 *  - Regla de visibilidad (Día 4.1, decisión de Isaac): el PROPIETARIO ve todas; los demás, solo
 *    las suyas. La aplica el BACKEND: aquí solo lo explicamos en pantalla.
 *  - Marcar como hecha: PATCH del estado (también un LECTOR, si la tarea es suya).
 *  - Las que vienen de Iris llevan su marca y conservan el vínculo con el documento que las originó.
 */
import { useCallback, useEffect, useState } from "react";
import { useOutletContext } from "react-router";
import { api, mensajeLocal } from "../../api/cliente.js";
import { useAuth } from "../../auth/AuthContext.jsx";
import { useConfirmar } from "../../componentes/Confirmacion.jsx";
import { fecha, hoyIso, sumarDias } from "../../utilidades/fechas.js";
import "./Tareas.css";

const FILTROS = [
  { valor: "PENDIENTE", texto: "Pendientes" },
  { valor: "HECHA", texto: "Hechas" },
  { valor: "", texto: "Todas" },
];

const vacia = { titulo: "", descripcion: "", fechaLimite: "", responsableId: "" };

/** ¿Qué tan cerca está la fecha límite? Misma lógica que los recordatorios del backend (Día 4.3). */
function urgencia(t) {
  if (t.estado === "HECHA" || !t.fechaLimite) return null;
  const hoy = hoyIso();
  if (t.fechaLimite < hoy) return { texto: "Vencida", clase: "chip-error" };
  if (t.fechaLimite === hoy) return { texto: "Vence hoy", clase: "chip-coral" };
  if (t.fechaLimite === sumarDias(hoy, 1)) return { texto: "Vence mañana", clase: "chip-coral" };
  return null;
}

export default function Tareas() {
  const { evento, puedeEditar } = useOutletContext();
  const { usuario } = useAuth();
  const confirmar = useConfirmar();
  const esPropietario = evento.miRol === "PROPIETARIO";
  const base = `/api/eventos/${evento.id}/tareas`;

  const [tareas, setTareas] = useState(null);
  const [miembros, setMiembros] = useState([]);
  const [estado, setEstado] = useState("PENDIENTE");
  const [mias, setMias] = useState(false);
  const [formulario, setFormulario] = useState(null);
  const [error, setError] = useState(null);

  const cargar = useCallback(() => {
    // URLSearchParams arma "?estado=PENDIENTE&mias=true" escapando bien cada valor
    const parametros = new URLSearchParams();
    if (estado) parametros.set("estado", estado);
    if (mias) parametros.set("mias", "true");
    return api(`${base}?${parametros}`).then(setTareas).catch((e) => setError(mensajeLocal(e)));
  }, [base, estado, mias]);

  useEffect(() => {
    cargar();
  }, [cargar]); // al cambiar un filtro, "cargar" cambia y el efecto vuelve a pedir la lista

  useEffect(() => {
    api(`/api/eventos/${evento.id}/miembros`).then(setMiembros).catch(() => {});
  }, [evento.id]);

  // Un "diccionario" id -> nombre, para mostrar el responsable sin buscar en la lista cada vez
  const nombreDe = Object.fromEntries(miembros.map((m) => [m.usuarioId, m.nombre]));
  const cambiar = (campo) => (e) => setFormulario({ ...formulario, [campo]: e.target.value });

  async function guardar(e) {
    e.preventDefault();
    setError(null);
    const { id, ...datos } = formulario;
    // Un <input> vacío manda "": el backend espera null para "sin fecha" o "sin responsable"
    const cuerpo = { ...datos, fechaLimite: datos.fechaLimite || null, responsableId: datos.responsableId || null };
    try {
      await api(id ? `${base}/${id}` : base, { metodo: id ? "PUT" : "POST", cuerpo });
      setFormulario(null);
      cargar();
    } catch (err) {
      setError(mensajeLocal(err));
    }
  }

  async function alternar(t) {
    setError(null);
    const nuevo = t.estado === "HECHA" ? "PENDIENTE" : "HECHA";
    // ACTUALIZACIÓN OPTIMISTA: la casilla cambia YA; si el backend dice que no, la devolvemos
    setTareas((lista) => lista.map((x) => (x.id === t.id ? { ...x, estado: nuevo } : x)));
    try {
      await api(`${base}/${t.id}/estado`, { metodo: "PATCH", cuerpo: { estado: nuevo } });
    } catch (err) {
      setTareas((lista) => lista.map((x) => (x.id === t.id ? t : x)));
      setError(mensajeLocal(err));
    }
  }

  async function borrar(t) {
    const ok = await confirmar({ titulo: `¿Borrar «${t.titulo}»?`, mensaje: "La tarea desaparece para todo el equipo.", accion: "Borrar", peligro: true });
    if (!ok) return;
    try {
      await api(`${base}/${t.id}`, { metodo: "DELETE" });
      setTareas(tareas.filter((x) => x.id !== t.id));
    } catch (err) {
      setError(mensajeLocal(err));
    }
  }

  const editar = (t) => setFormulario({ id: t.id, titulo: t.titulo, descripcion: t.descripcion ?? "",
    fechaLimite: t.fechaLimite ?? "", responsableId: t.responsableId ?? "" });

  return (
    <div className="tareas">
      <h1>Tareas</h1>
      <div className="tareas-barra">
        <div className="tareas-filtros" role="group" aria-label="Filtrar tareas">
          {FILTROS.map((f) => (
            <button key={f.texto} className={`boton ${estado === f.valor ? "filtro-activo" : ""}`}
              aria-pressed={estado === f.valor} onClick={() => setEstado(f.valor)}>
              {f.texto}
            </button>
          ))}
          {esPropietario && (
            <label className="tareas-mias">
              <input type="checkbox" checked={mias} onChange={(e) => setMias(e.target.checked)} /> Solo las mías
            </label>
          )}
        </div>
        {puedeEditar && !formulario && (
          <button className="boton boton-primario" onClick={() => setFormulario({ ...vacia })}>Nueva tarea</button>
        )}
      </div>

      {!esPropietario && (
        <p className="texto-suave">Ves las tareas que tienes a tu cargo. El propietario del evento ve las de todo el equipo.</p>
      )}
      {error && <p className="aviso-error" role="alert">{error}</p>}

      {formulario && (
        <form className="tarjeta tareas-formulario" onSubmit={guardar}>
          <label className="etiqueta tareas-ancho">Tarea
            <input className="campo" required placeholder="Pagar el anticipo del hotel" value={formulario.titulo} onChange={cambiar("titulo")} />
          </label>
          <label className="etiqueta">Fecha límite<input className="campo" type="date" value={formulario.fechaLimite} onChange={cambiar("fechaLimite")} /></label>
          <label className="etiqueta">Responsable
            <select className="campo" value={formulario.responsableId} onChange={cambiar("responsableId")}>
              <option value="">Sin responsable</option>
              {miembros.map((m) => <option key={m.usuarioId} value={m.usuarioId}>{m.nombre}</option>)}
            </select>
          </label>
          <label className="etiqueta tareas-ancho">Detalle<textarea className="campo" rows={2} value={formulario.descripcion} onChange={cambiar("descripcion")} /></label>
          <div className="tareas-botones tareas-ancho">
            <button type="button" className="boton" onClick={() => setFormulario(null)}>Cancelar</button>
            <button className="boton boton-primario">{formulario.id ? "Guardar cambios" : "Crear tarea"}</button>
          </div>
        </form>
      )}

      {tareas === null && <p className="cargando">Cargando tareas…</p>}
      {tareas?.length === 0 && (
        <div className="tarjeta tareas-vacia">
          <h2>{estado === "HECHA" ? "Todavía no hay tareas hechas" : "Nada pendiente por aquí"}</h2>
          <p className="texto-suave">Crea una tarea, o deja que Iris te proponga las de tus documentos.</p>
        </div>
      )}

      <ul className="tareas-lista">
        {tareas?.map((t) => {
          const u = urgencia(t);
          const hecha = t.estado === "HECHA";
          return (
            <li key={t.id} className={`tarjeta tarea ${hecha ? "tarea-hecha" : ""}`}>
              <input type="checkbox" className="tarea-casilla" checked={hecha} onChange={() => alternar(t)}
                aria-label={hecha ? `Marcar «${t.titulo}» como pendiente` : `Marcar «${t.titulo}» como hecha`} />
              <div className="tarea-info">
                <div className="tarea-titulo">
                  <strong>{t.titulo}</strong>
                  {u && <span className={`chip ${u.clase}`}>{u.texto}</span>}
                  {t.origen === "IRIS" && <span className="chip">Propuesta por Iris</span>}
                </div>
                {t.descripcion && <span className="tarea-descripcion">{t.descripcion}</span>}
                <span className="texto-suave">
                  {t.fechaLimite ? `Para el ${fecha(t.fechaLimite)}` : "Sin fecha"}
                  {" · "}
                  {t.responsableId
                    ? t.responsableId === usuario?.id ? "A tu cargo" : `Responsable: ${nombreDe[t.responsableId] ?? "miembro del equipo"}`
                    : "Sin responsable"}
                </span>
              </div>
              {puedeEditar && (
                <div className="tarea-acciones">
                  <button className="boton" onClick={() => editar(t)}>Editar</button>
                  <button className="boton boton-peligro" onClick={() => borrar(t)}>Borrar</button>
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
