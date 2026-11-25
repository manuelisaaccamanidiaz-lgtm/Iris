/*
 * La página de UN evento (6.4: rediseño con BARRA LATERAL, la idea del diseño original de Isaac).
 *
 *  Escritorio (>= 900 px)          Celular / tablet (< 900 px)
 *  ┌────────┬───────────────┐      ┌──────────────────────┐
 *  │ lateral│  pestaña      │      │ ☰  Nombre   🔔       │  <- barra corta
 *  │ (fija) │  activa       │      ├──────────────────────┤
 *  │        │  (Outlet)     │      │  pestaña activa      │
 *  └────────┴───────────────┘      └──────────────────────┘
 *                                   ☰ abre la lateral como un MENÚ que se desliza encima (decisión 3B)
 *
 * Cada sección sigue siendo una RUTA HIJA (/eventos/:id/chat...): se puede recargar o compartir el enlace.
 * <Outlet context={...}> le pasa el evento a la pestaña, que lo lee con useOutletContext().
 */
import { useCallback, useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation, useOutletContext, useParams } from "react-router";
import { api } from "../api/cliente.js";
import { useAuth } from "../auth/AuthContext.jsx";
import Campanita from "../componentes/Campanita.jsx";
import Icono from "../componentes/Iconos.jsx";
import PantallaCompleta from "../componentes/PantallaCompleta.jsx";
import { fecha, fechaCorta } from "../utilidades/fechas.js";
import { CORTE_MENU, CORTE_PANEL, useMedia } from "../utilidades/pantalla.js";
import "./EventoDetalle.css";

const ROL = { PROPIETARIO: "Propietario", EDITOR: "Editor", LECTOR: "Lector" };

export default function EventoDetalle() {
  const { eventoId } = useParams();
  const { tema, alternarTema } = useOutletContext(); // viene del Layout
  const { usuario, salir } = useAuth();
  const { pathname } = useLocation();
  const esMovil = useMedia(CORTE_MENU);
  const panelAlLado = useMedia(CORTE_PANEL); // sin panel al lado del chat, aparece la sección Resumen

  const [evento, setEvento] = useState(null);
  const [error, setError] = useState(null);
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [pendientes, setPendientes] = useState(0); // el contador coral de "Propuestas"

  useEffect(() => {
    // El backend no tiene "GET /api/eventos/{id}" a propósito: buscamos en MIS eventos.
    // Si no está, para mí "no existe" (la misma regla del 404 del backend)
    api("/api/eventos")
      .then((lista) => {
        const encontrado = lista.find((e) => e.id === eventoId);
        if (encontrado) setEvento(encontrado);
        else setError("Este evento no existe o no eres parte de su equipo.");
      })
      .catch((e) => setError(e.message));
  }, [eventoId]);

  const puedeEditar = evento?.miRol === "PROPIETARIO" || evento?.miRol === "EDITOR";

  // El contador se pide al entrar y al cambiar de sección; la pestaña Propuestas lo actualiza al instante
  const contarPendientes = useCallback(() => {
    if (!puedeEditar) return;
    api(`/api/eventos/${eventoId}/propuestas?estado=PENDIENTE`).then((l) => setPendientes(l.length)).catch(() => {});
  }, [eventoId, puedeEditar]);
  useEffect(contarPendientes, [contarPendientes, pathname]);

  // Al navegar, el menú del celular se cierra solo (si no, taparía la sección que acabas de elegir)
  useEffect(() => setMenuAbierto(false), [pathname]);

  // Escape cierra el menú (accesibilidad de teclado)
  useEffect(() => {
    if (!menuAbierto) return;
    const alTecla = (e) => e.key === "Escape" && setMenuAbierto(false);
    document.addEventListener("keydown", alTecla);
    return () => document.removeEventListener("keydown", alTecla);
  }, [menuAbierto]);

  if (error) {
    return (
      <div className="evento-error">
        <p className="aviso-error" role="alert">{error}</p>
        <Link to="/">Volver a mis eventos</Link>
      </div>
    );
  }
  if (!evento) return <p className="cargando">Cargando el evento…</p>;

  const inicial = (usuario?.email ?? "?").charAt(0).toUpperCase();

  return (
    <div className={`evento-marco ${menuAbierto ? "menu-abierto" : ""}`}>
      {esMovil && (
        <header className="evento-movil">
          <button className="boton evento-movil-menu" onClick={() => setMenuAbierto(true)}
            aria-label="Abrir el menú del evento" aria-expanded={menuAbierto} aria-controls="evento-lateral">
            <Icono nombre="menu" />
          </button>
          <strong className="evento-movil-nombre">{evento.nombre}</strong>
          <Campanita />
        </header>
      )}

      {/* inert: con el menú CERRADO en el celular, la lateral está fuera de pantalla y Tab no debe entrar en ella */}
      <aside id="evento-lateral" className="lateral" aria-label="Secciones del evento" inert={esMovil && !menuAbierto}>
        <div className="lateral-arriba">
          <Link to="/" className="lateral-volver">
            <Icono nombre="volver" tamano={16} /> Mis eventos
          </Link>
          {!esMovil && <Campanita />}
        </div>

        <div className="lateral-evento">
          <span className="logo">Iris<span>.</span></span>
          <h1 className="lateral-nombre">{evento.nombre}</h1>
          <p className="lateral-datos">
            {fechaCorta(evento.fechaInicio)} – {fecha(evento.fechaFin)}
            {evento.lugar && <><br />{evento.lugar}</>}
          </p>
        </div>

        <nav className="lateral-nav">
          <NavLink to="chat"><Icono nombre="chat" /> Pregúntale a Iris</NavLink>
          {!panelAlLado && <NavLink to="resumen"><Icono nombre="resumen" /> Resumen</NavLink>}
          <NavLink to="documentos"><Icono nombre="documentos" /> Documentos</NavLink>
          <NavLink to="agenda"><Icono nombre="agenda" /> Agenda</NavLink>
          <NavLink to="tareas"><Icono nombre="tareas" /> Tareas</NavLink>
          {/* Solo editores y propietarios ven las propuestas (el backend responde 403 a un lector) */}
          {puedeEditar && (
            <NavLink to="propuestas">
              <Icono nombre="propuestas" /> Propuestas
              {pendientes > 0 && (
                <span className="lateral-contador" aria-label={`${pendientes} por revisar`}>{pendientes}</span>
              )}
            </NavLink>
          )}
          <NavLink to="equipo"><Icono nombre="equipo" /> Equipo</NavLink>
        </nav>

        <div className="lateral-pie">
          <PantallaCompleta className="boton lateral-completa" />
          {/* Interruptor de dos opciones: aria-pressed dice cuál está activa */}
          <div className="lateral-tema" role="group" aria-label="Tema de la aplicación">
            <button className={tema !== "dark" ? "activo" : ""} aria-pressed={tema !== "dark"}
              onClick={() => tema === "dark" && alternarTema()}>Claro</button>
            <button className={tema === "dark" ? "activo" : ""} aria-pressed={tema === "dark"}
              onClick={() => tema !== "dark" && alternarTema()}>Oscuro</button>
          </div>
          <div className="lateral-usuario">
            <span className="lateral-avatar" aria-hidden="true">{inicial}</span>
            <div className="lateral-usuario-texto">
              <span className="lateral-email" title={usuario?.email}>{usuario?.email}</span>
              <span className="lateral-rol">{ROL[evento.miRol] ?? evento.miRol}</span>
            </div>
            <button className="boton lateral-salir" onClick={salir}>Salir</button>
          </div>
        </div>
      </aside>

      {/* El fondo oscuro detrás del menú en el celular: tocarlo lo cierra */}
      {esMovil && menuAbierto && <div className="lateral-fondo" onClick={() => setMenuAbierto(false)} />}

      <main className="evento-contenido">
        <Outlet context={{ evento, puedeEditar, alCambiarPendientes: setPendientes }} />
      </main>
    </div>
  );
}
