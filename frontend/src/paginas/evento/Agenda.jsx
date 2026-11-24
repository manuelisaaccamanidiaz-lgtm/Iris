/*
 * 6.1 La agenda: las sesiones del evento agrupadas por día, con un color por tipo.
 * EDITORES y PROPIETARIOS crean, editan y borran. Las que vienen de una propuesta aceptada
 * llevan la marca "Propuesta por Iris" (origen IRIS, Día 4.4).
 */
import { useCallback, useEffect, useState } from "react";
import { useOutletContext } from "react-router";
import { api, mensajeLocal } from "../../api/cliente.js";
import { useConfirmar } from "../../componentes/Confirmacion.jsx";
import { diaLargo, hora } from "../../utilidades/fechas.js";
import "./Agenda.css";

const TIPOS = { CONFERENCIA: "Conferencia", TALLER: "Taller", PANEL: "Panel", REUNION: "Reunión", LOGISTICA: "Logística", OTRO: "Otro" };

const vacia = (fecha) => ({ titulo: "", descripcion: "", fecha, horaInicio: "09:00", horaFin: "10:00", sala: "", ponente: "", tipo: "CONFERENCIA" });

/** [{fecha, sesiones}] en orden: la agenda llega ordenada del backend, solo hay que agrupar. */
function porDia(sesiones) {
  const grupos = new Map(); // Map conserva el orden de inserción
  for (const s of sesiones) {
    if (!grupos.has(s.fecha)) grupos.set(s.fecha, []);
    grupos.get(s.fecha).push(s);
  }
  return [...grupos].map(([fecha, lista]) => ({ fecha, sesiones: lista }));
}

export default function Agenda() {
  const { evento, puedeEditar } = useOutletContext();
  const confirmar = useConfirmar();
  const base = `/api/eventos/${evento.id}/sesiones`;
  const [sesiones, setSesiones] = useState(null);
  const [formulario, setFormulario] = useState(null); // null = cerrado; { ...datos, id? } = abierto
  const [error, setError] = useState(null);

  const cargar = useCallback(() => api(base).then(setSesiones).catch((e) => setError(mensajeLocal(e))), [base]);
  useEffect(() => {
    cargar();
  }, [cargar]);

  const cambiar = (campo) => (e) => setFormulario({ ...formulario, [campo]: e.target.value });

  async function guardar(e) {
    e.preventDefault();
    setError(null);
    const { id, ...datos } = formulario; // separar el id del resto (desestructuración)
    try {
      await api(id ? `${base}/${id}` : base, { metodo: id ? "PUT" : "POST", cuerpo: datos });
      setFormulario(null);
      cargar();
    } catch (err) {
      setError(mensajeLocal(err)); // p. ej. 400 "La fecha debe estar dentro del evento"
    }
  }

  async function borrar(s) {
    const ok = await confirmar({ titulo: `¿Borrar «${s.titulo}»?`, mensaje: "Se quita de la agenda del evento.", accion: "Borrar", peligro: true });
    if (!ok) return;
    try {
      await api(`${base}/${s.id}`, { metodo: "DELETE" });
      setSesiones(sesiones.filter((x) => x.id !== s.id));
    } catch (err) {
      setError(mensajeLocal(err));
    }
  }

  // La hora llega como "09:00:00"; el <input type="time"> quiere "09:00"
  const editar = (s) => setFormulario({ ...s, horaInicio: s.horaInicio.slice(0, 5), horaFin: s.horaFin.slice(0, 5),
    sala: s.sala ?? "", ponente: s.ponente ?? "", descripcion: s.descripcion ?? "" });

  return (
    <div className="agenda">
      <div className="agenda-cabecera">
        <p className="texto-suave">Del {diaLargo(evento.fechaInicio)} al {diaLargo(evento.fechaFin)}.</p>
        {puedeEditar && !formulario && (
          <button className="boton boton-primario" onClick={() => setFormulario(vacia(evento.fechaInicio))}>
            Nueva sesión
          </button>
        )}
      </div>

      {error && <p className="aviso-error" role="alert">{error}</p>}

      {formulario && (
        <form className="tarjeta agenda-formulario" onSubmit={guardar}>
          <label className="etiqueta agenda-ancho">Título<input className="campo" required value={formulario.titulo} onChange={cambiar("titulo")} /></label>
          <label className="etiqueta">Día
            {/* min y max: el calendario solo deja elegir días del evento (el backend lo vuelve a validar) */}
            <input className="campo" type="date" required min={evento.fechaInicio} max={evento.fechaFin}
              value={formulario.fecha} onChange={cambiar("fecha")} />
          </label>
          <label className="etiqueta">Inicio<input className="campo" type="time" required value={formulario.horaInicio} onChange={cambiar("horaInicio")} /></label>
          <label className="etiqueta">Fin<input className="campo" type="time" required value={formulario.horaFin} onChange={cambiar("horaFin")} /></label>
          <label className="etiqueta">Tipo
            <select className="campo" value={formulario.tipo} onChange={cambiar("tipo")}>
              {Object.entries(TIPOS).map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
            </select>
          </label>
          <label className="etiqueta">Sala<input className="campo" value={formulario.sala} onChange={cambiar("sala")} /></label>
          <label className="etiqueta">Ponente<input className="campo" value={formulario.ponente} onChange={cambiar("ponente")} /></label>
          <label className="etiqueta agenda-ancho">Descripción<textarea className="campo" rows={2} value={formulario.descripcion} onChange={cambiar("descripcion")} /></label>
          <div className="agenda-botones agenda-ancho">
            <button type="button" className="boton" onClick={() => setFormulario(null)}>Cancelar</button>
            <button className="boton boton-primario">{formulario.id ? "Guardar cambios" : "Agregar a la agenda"}</button>
          </div>
        </form>
      )}

      {sesiones === null && <p className="cargando">Cargando la agenda…</p>}
      {sesiones?.length === 0 && (
        <div className="tarjeta agenda-vacia">
          <h2>La agenda está vacía</h2>
          <p className="texto-suave">Agrega la primera sesión, o sube el cronograma y deja que Iris te la proponga.</p>
        </div>
      )}

      {sesiones && porDia(sesiones).map(({ fecha, sesiones: delDia }) => (
        <section key={fecha} className="agenda-dia">
          <h2 className="agenda-dia-titulo">{diaLargo(fecha)}</h2>
          {delDia.map((s) => (
            <article key={s.id} className={`tarjeta sesion tipo-${s.tipo}`}>
              <div className="sesion-hora">
                <strong>{hora(s.horaInicio)}</strong>
                <span className="texto-suave">{hora(s.horaFin)}</span>
              </div>
              <div className="sesion-info">
                <div className="sesion-titulo">
                  <span className="chip sesion-tipo">{TIPOS[s.tipo] ?? s.tipo}</span>
                  <strong>{s.titulo}</strong>
                  {s.origen === "IRIS" && <span className="chip chip-coral">Propuesta por Iris</span>}
                </div>
                <span className="texto-suave">{[s.sala, s.ponente].filter(Boolean).join(" · ")}</span>
                {s.descripcion && <p className="sesion-descripcion">{s.descripcion}</p>}
              </div>
              {puedeEditar && (
                <div className="sesion-acciones">
                  <button className="boton" onClick={() => editar(s)}>Editar</button>
                  <button className="boton boton-peligro" onClick={() => borrar(s)}>Borrar</button>
                </div>
              )}
            </article>
          ))}
        </section>
      ))}
    </div>
  );
}
