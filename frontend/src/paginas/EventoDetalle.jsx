/*
 * La página de UN evento: cabecera + pestañas (Chat, Documentos...). Cada pestaña es una RUTA HIJA
 * (/eventos/:id/chat, /eventos/:id/documentos): así se puede compartir el enlace o recargar sin perderla.
 * <Outlet context={...}> le pasa el evento a la pestaña, que lo lee con useOutletContext().
 */
import { useEffect, useState } from "react";
import { NavLink, Outlet, useParams } from "react-router";
import { api } from "../api/cliente.js";
import "./EventoDetalle.css";

export default function EventoDetalle() {
  const { eventoId } = useParams();
  const [evento, setEvento] = useState(null);
  const [error, setError] = useState(null);

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

  if (error) return <p className="aviso-error" role="alert">{error}</p>;
  if (!evento) return <p className="cargando">Cargando el evento…</p>;

  const puedeEditar = evento.miRol === "PROPIETARIO" || evento.miRol === "EDITOR";

  return (
    <section className="evento">
      <header className="evento-cabecera">
        <h1>{evento.nombre}</h1>
        <p className="texto-suave">
          {evento.fechaInicio} al {evento.fechaFin}
          {evento.lugar && ` · ${evento.lugar}`}
        </p>
      </header>
      <nav className="pestanas">
        <NavLink to="chat">Pregúntale a Iris</NavLink>
        <NavLink to="documentos">Documentos</NavLink>
        <NavLink to="agenda">Agenda</NavLink>
        <NavLink to="tareas">Tareas</NavLink>
        {/* Solo editores y propietarios ven las propuestas (el backend responde 403 a un lector) */}
        {puedeEditar && <NavLink to="propuestas">Propuestas de Iris</NavLink>}
        <NavLink to="equipo">Equipo</NavLink>
      </nav>
      <Outlet context={{ evento, puedeEditar }} />
    </section>
  );
}
