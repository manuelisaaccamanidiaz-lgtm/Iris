/*
 * El equipo del evento. Todos ven a los miembros; solo los PROPIETARIOS invitan, cambian roles,
 * quitan gente y ven las invitaciones. Las reglas finas (solo el creador nombra propietarios,
 * nadie toca al creador) NO se repiten aquí: las decide el backend y mostramos su mensaje.
 */
import { useCallback, useEffect, useState } from "react";
import { useNavigate, useOutletContext } from "react-router";
import { api } from "../../api/cliente.js";
import { useAuth } from "../../auth/AuthContext.jsx";
import { fecha, fechaHora } from "../../utilidades/fechas.js";
import "./Equipo.css";

const ROLES = ["LECTOR", "EDITOR", "PROPIETARIO"];
const NOMBRE_ROL = { PROPIETARIO: "Propietario", EDITOR: "Editor", LECTOR: "Lector" };
const ESTADO = {
  PENDIENTE: "chip-aviso",
  ACEPTADA: "chip-exito",
  RECHAZADA: "chip-error",
  CANCELADA: "",
  VENCIDA: "",
};

export default function Equipo() {
  const { evento } = useOutletContext();
  const { usuario } = useAuth();
  const navegar = useNavigate();
  const esPropietario = evento.miRol === "PROPIETARIO";
  const base = `/api/eventos/${evento.id}`;

  const [miembros, setMiembros] = useState(null);
  const [invitaciones, setInvitaciones] = useState([]);
  const [nueva, setNueva] = useState({ email: "", rol: "EDITOR" });
  const [error, setError] = useState(null);
  const [aviso, setAviso] = useState(null);

  const cargar = useCallback(() => {
    api(`${base}/miembros`).then(setMiembros).catch((e) => setError(e.message));
    if (esPropietario) api(`${base}/invitaciones`).then(setInvitaciones).catch(() => {});
  }, [base, esPropietario]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  /** Envuelve cada acción: limpia mensajes, ejecuta, recarga y muestra el error del backend si lo hay. */
  async function hacer(accion, mensajeOk) {
    setError(null);
    setAviso(null);
    try {
      await accion();
      if (mensajeOk) setAviso(mensajeOk);
      cargar();
    } catch (e) {
      setError(e.message);
    }
  }

  function invitar(e) {
    e.preventDefault();
    hacer(async () => {
      await api(`${base}/invitaciones`, { metodo: "POST", cuerpo: nueva });
      setNueva({ email: "", rol: "EDITOR" });
    }, `Listo, invité a ${nueva.email}. Le llegará un aviso y un correo.`);
  }

  const cambiarRol = (m, rol) =>
    hacer(() => api(`${base}/miembros/${m.usuarioId}`, { metodo: "PATCH", cuerpo: { rol } }), `${m.nombre} ahora es ${NOMBRE_ROL[rol].toLowerCase()}.`);

  const quitar = (m) =>
    window.confirm(`¿Quitar a ${m.nombre} del evento? Sus tareas quedarán sin responsable.`) &&
    hacer(() => api(`${base}/miembros/${m.usuarioId}`, { metodo: "DELETE" }), `${m.nombre} ya no está en el equipo.`);

  const cancelar = (inv) =>
    hacer(() => api(`${base}/invitaciones/${inv.id}`, { metodo: "DELETE" }), "Invitación cancelada.");

  async function salir() {
    if (!window.confirm(`¿Salir de «${evento.nombre}»?`)) return;
    setError(null);
    try {
      await api(`${base}/miembros/yo`, { metodo: "DELETE" });
      navegar("/", { replace: true });
    } catch (e) {
      setError(e.message); // p. ej. el creador antes de que el evento termine
    }
  }

  return (
    <div className="equipo">
      {error && <p className="aviso-error" role="alert">{error}</p>}
      {aviso && <p className="chip equipo-aviso" role="status">{aviso}</p>}

      {esPropietario && (
        <form className="tarjeta equipo-invitar" onSubmit={invitar}>
          <label className="etiqueta">
            Invitar por correo
            <input className="campo" type="email" required placeholder="carla@empresa.com"
              value={nueva.email} onChange={(e) => setNueva({ ...nueva, email: e.target.value })} />
          </label>
          <label className="etiqueta">
            Como
            <select className="campo" value={nueva.rol} onChange={(e) => setNueva({ ...nueva, rol: e.target.value })}>
              {ROLES.map((r) => (
                <option key={r} value={r}>{NOMBRE_ROL[r]}</option>
              ))}
            </select>
          </label>
          <button className="boton boton-primario">Invitar</button>
          <p className="texto-suave equipo-ayuda">
            Editor: sube documentos y crea tareas. Lector: consulta y pregunta a Iris. Propietario: todo
            (solo quien creó el evento puede nombrar propietarios). La invitación vence en 7 días.
          </p>
        </form>
      )}

      <div className="tarjeta">
        <h2>Equipo</h2>
        {miembros === null && <p className="texto-suave">Cargando…</p>}
        <ul className="equipo-lista">
          {miembros?.map((m) => {
            const soyYo = m.usuarioId === usuario?.id;
            return (
              <li key={m.usuarioId} className="miembro">
                <div className="miembro-info">
                  <strong>{m.nombre}{soyYo && " (tú)"}</strong>
                  <span className="texto-suave">{m.email} · desde el {fecha(m.unidoEn?.slice(0, 10))}</span>
                </div>
                {esPropietario && !soyYo ? (
                  <>
                    <select className="campo" value={m.rol} onChange={(e) => cambiarRol(m, e.target.value)}
                      aria-label={`Rol de ${m.nombre}`}>
                      {ROLES.map((r) => (
                        <option key={r} value={r}>{NOMBRE_ROL[r]}</option>
                      ))}
                    </select>
                    <button className="boton boton-peligro" onClick={() => quitar(m)}>Quitar</button>
                  </>
                ) : (
                  <span className="chip">{NOMBRE_ROL[m.rol]}</span>
                )}
              </li>
            );
          })}
        </ul>
        <button className="boton boton-peligro equipo-salir" onClick={salir}>
          Salir del evento
        </button>
      </div>

      {esPropietario && invitaciones.length > 0 && (
        <div className="tarjeta">
          <h2>Invitaciones enviadas</h2>
          <ul className="equipo-lista">
            {invitaciones.map((inv) => (
              <li key={inv.id} className="miembro">
                <div className="miembro-info">
                  <strong>{inv.email}</strong>
                  <span className="texto-suave">
                    {NOMBRE_ROL[inv.rol]} · enviada el {fechaHora(inv.creadaEn)}
                  </span>
                </div>
                <span className={`chip ${ESTADO[inv.estado] ?? ""}`}>{inv.estado.toLowerCase()}</span>
                {inv.estado === "PENDIENTE" && (
                  <button className="boton" onClick={() => cancelar(inv)}>Cancelar</button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
