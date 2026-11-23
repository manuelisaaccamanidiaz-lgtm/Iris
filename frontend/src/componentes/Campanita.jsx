/*
 * La campanita (decisión 3A del Día 4: polling cada 30 s).
 *  - Cada 30 s pregunta SOLO el número de no leídas (/conteo): una consulta barata.
 *  - La lista completa se pide solo al ABRIR el panel.
 *  - Si la pestaña está oculta, no pregunta: no gastamos red ni servidor en una pestaña que nadie mira.
 */
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { api } from "../api/cliente.js";
import { haceCuanto } from "../utilidades/fechas.js";
import "./Campanita.css";

const CADA_MS = 30_000;

/** A dónde lleva cada aviso al hacerle clic. */
function destino(n) {
  const evento = `/eventos/${n.eventoId}`;
  switch (n.tipo) {
    case "INVITACION_RECIBIDA":
      return "/invitaciones";
    case "DOCUMENTO_INDEXADO":
      return `${evento}/documentos`;
    case "PROPUESTAS_NUEVAS":
      return `${evento}/propuestas`;
    case "SESION_NUEVA":
      return `${evento}/agenda`;
    case "MIEMBRO_NUEVO":
      return `${evento}/equipo`;
    default:
      return `${evento}/tareas`; // tareas nuevas, asignadas y recordatorios
  }
}

export default function Campanita() {
  const navegar = useNavigate();
  const [noLeidas, setNoLeidas] = useState(0);
  const [abierta, setAbierta] = useState(false);
  const [lista, setLista] = useState(null);
  const contenedor = useRef(null);

  const contar = useCallback(() => {
    if (document.visibilityState !== "visible") return;
    api("/api/notificaciones/conteo")
      .then((r) => setNoLeidas(r.noLeidas))
      .catch(() => {}); // si falla una vez, lo intenta en 30 s: no molestamos con un error
  }, []);

  // El reloj de 30 s, más una pregunta inmediata al volver a la pestaña
  useEffect(() => {
    contar();
    const reloj = setInterval(contar, CADA_MS);
    document.addEventListener("visibilitychange", contar);
    return () => {
      clearInterval(reloj);
      document.removeEventListener("visibilitychange", contar);
    };
  }, [contar]);

  // Cerrar con un clic afuera o con Escape
  useEffect(() => {
    if (!abierta) return;
    const alClic = (e) => !contenedor.current?.contains(e.target) && setAbierta(false);
    const alTecla = (e) => e.key === "Escape" && setAbierta(false);
    document.addEventListener("mousedown", alClic);
    document.addEventListener("keydown", alTecla);
    return () => {
      document.removeEventListener("mousedown", alClic);
      document.removeEventListener("keydown", alTecla);
    };
  }, [abierta]);

  function alternar() {
    if (!abierta) {
      setLista(null);
      api("/api/notificaciones").then(setLista).catch(() => setLista([]));
    }
    setAbierta(!abierta);
  }

  async function abrir(n) {
    if (!n.leida) {
      api(`/api/notificaciones/${n.id}/leida`, { metodo: "PATCH" }).catch(() => {});
      setNoLeidas((x) => Math.max(0, x - 1));
    }
    setAbierta(false);
    navegar(destino(n));
  }

  async function leerTodas() {
    await api("/api/notificaciones/leidas", { metodo: "PATCH" });
    setNoLeidas(0);
    setLista(lista.map((n) => ({ ...n, leida: true })));
  }

  return (
    <div className="campanita" ref={contenedor}>
      <button
        className="boton campanita-boton"
        onClick={alternar}
        aria-expanded={abierta}
        aria-label={noLeidas ? `Notificaciones, ${noLeidas} sin leer` : "Notificaciones"}
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
          strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M10 5a2 2 0 1 1 4 0a7 7 0 0 1 4 6v3a4 4 0 0 0 2 3h-16a4 4 0 0 0 2 -3v-3a7 7 0 0 1 4 -6" />
          <path d="M9 17v1a3 3 0 0 0 6 0v-1" />
        </svg>
        {noLeidas > 0 && <span className="campanita-contador">{noLeidas > 99 ? "99+" : noLeidas}</span>}
      </button>

      {abierta && (
        <div className="campanita-panel tarjeta" role="dialog" aria-label="Notificaciones">
          <div className="campanita-cabecera">
            <strong>Notificaciones</strong>
            {lista?.some((n) => !n.leida) && (
              <button className="boton" onClick={leerTodas}>
                Marcar todas como leídas
              </button>
            )}
          </div>
          {lista === null && <p className="texto-suave">Cargando…</p>}
          {lista?.length === 0 && <p className="texto-suave">Todo al día. Aquí te avisaré de tareas, invitaciones y documentos.</p>}
          <ul className="campanita-lista">
            {lista?.map((n) => (
              <li key={n.id}>
                <button className={`campanita-aviso ${n.leida ? "" : "no-leida"}`} onClick={() => abrir(n)}>
                  <span>{n.mensaje}</span>
                  <span className="texto-suave">{haceCuanto(n.creadaEn)}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
