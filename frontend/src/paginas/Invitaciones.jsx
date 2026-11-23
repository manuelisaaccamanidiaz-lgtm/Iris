/*
 * Mi bandeja de invitaciones: las PENDIENTES dirigidas a mi email (aunque me hayan invitado
 * antes de crear mi cuenta). Aceptar me vuelve miembro y me lleva al evento.
 */
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/cliente.js";
import { fecha, fechaHora } from "../utilidades/fechas.js";
import "./Invitaciones.css";

const ROL = { PROPIETARIO: "propietario", EDITOR: "editor", LECTOR: "lector" };

export default function Invitaciones() {
  const navegar = useNavigate();
  const [invitaciones, setInvitaciones] = useState(null);
  const [error, setError] = useState(null);
  const [ocupada, setOcupada] = useState(null); // id de la que se está respondiendo (evita el doble clic)

  useEffect(() => {
    api("/api/invitaciones").then(setInvitaciones).catch((e) => setError(e.message));
  }, []);

  async function aceptar(inv) {
    setError(null);
    setOcupada(inv.id);
    try {
      await api(`/api/invitaciones/${inv.id}/aceptar`, { metodo: "POST" });
      navegar(`/eventos/${inv.eventoId}`);
    } catch (e) {
      setError(e.message); // 409 si venció o ya la habías respondido
      setOcupada(null);
    }
  }

  async function rechazar(inv) {
    if (!window.confirm(`¿Rechazar la invitación a «${inv.nombreEvento}»?`)) return;
    setError(null);
    setOcupada(inv.id);
    try {
      await api(`/api/invitaciones/${inv.id}/rechazar`, { metodo: "POST" });
      setInvitaciones(invitaciones.filter((i) => i.id !== inv.id));
    } catch (e) {
      setError(e.message);
    } finally {
      setOcupada(null);
    }
  }

  return (
    <section className="invitaciones">
      <div>
        <h1>Invitaciones</h1>
        <p className="texto-suave">Equipos que quieren que te unas a su evento.</p>
      </div>
      {error && <p className="aviso-error" role="alert">{error}</p>}
      {invitaciones === null && !error && <p className="cargando">Cargando…</p>}
      {invitaciones?.length === 0 && (
        <div className="tarjeta invitaciones-vacio">
          <h2>No tienes invitaciones pendientes</h2>
          <p className="texto-suave">Cuando alguien te invite a su evento, aparecerá aquí y te avisaré en la campanita.</p>
        </div>
      )}
      {invitaciones?.map((inv) => (
        <article key={inv.id} className="tarjeta invitacion">
          <div className="invitacion-info">
            <h2>{inv.nombreEvento}</h2>
            <p className="texto-suave">
              {fecha(inv.fechaInicio)} al {fecha(inv.fechaFin)}
              {inv.invitadoPor && ` · te invitó ${inv.invitadoPor}`}
            </p>
            <p>
              Entrarías como <span className="chip">{ROL[inv.rol] ?? inv.rol}</span>
              <span className="texto-suave"> · vence el {fechaHora(inv.expiraEn)}</span>
            </p>
          </div>
          <div className="invitacion-acciones">
            <button className="boton" onClick={() => rechazar(inv)} disabled={ocupada === inv.id}>
              Rechazar
            </button>
            <button className="boton boton-primario" onClick={() => aceptar(inv)} disabled={ocupada === inv.id}>
              Aceptar
            </button>
          </div>
        </article>
      ))}
    </section>
  );
}
