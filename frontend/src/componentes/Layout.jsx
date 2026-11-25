/*
 * El "marco" de todas las páginas con sesión: barra superior + el contenido de la ruta actual.
 * <Outlet /> es el hueco donde React Router pinta la página hija (Eventos, Documentos...).
 *
 * 6.4: DENTRO de un evento no hay barra superior: EventoDetalle dibuja su propia barra LATERAL
 * (la idea del diseño original). El tema se comparte con ella por el contexto del Outlet.
 */
import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useMatch } from "react-router";
import { useAuth } from "../auth/AuthContext.jsx";
import Campanita from "./Campanita.jsx";
import PantallaCompleta from "./PantallaCompleta.jsx";
import "./Layout.css";

/** Tema guardado (en localStorage: es una PREFERENCIA, no un dato sensible) o el del sistema. */
function temaInicial() {
  const guardado = localStorage.getItem("iris.tema");
  if (guardado) return guardado;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export default function Layout() {
  const { usuario, salir } = useAuth();
  const [tema, setTema] = useState(temaInicial);
  const alternarTema = () => setTema((t) => (t === "dark" ? "light" : "dark"));
  // useMatch: ¿la URL actual es /eventos/algo/...? (null si no)
  const dentroDeEvento = useMatch("/eventos/:eventoId/*");

  // Efecto: cada vez que cambia "tema", lo aplicamos al <html> y lo recordamos
  useEffect(() => {
    document.documentElement.dataset.theme = tema;
    localStorage.setItem("iris.tema", tema);
  }, [tema]);

  if (dentroDeEvento) {
    return <Outlet context={{ tema, alternarTema }} />;
  }

  return (
    <div className="marco">
      <header className="barra">
        <Link to="/" className="logo">
          Iris<span>.</span>
        </Link>
        <nav className="barra-nav">
          <NavLink to="/" end>
            Mis eventos
          </NavLink>
          <NavLink to="/invitaciones">Invitaciones</NavLink>
        </nav>
        <div className="barra-derecha">
          <Campanita />
          <PantallaCompleta className="boton barra-completa" />
          <button className="boton" onClick={alternarTema} aria-label="Cambiar entre modo claro y oscuro">
            {tema === "dark" ? "Claro" : "Oscuro"}
          </button>
          <span className="texto-suave barra-email">{usuario?.email}</span>
          <button className="boton" onClick={salir}>
            Salir
          </button>
        </div>
      </header>
      <main className="contenido">
        <Outlet />
      </main>
    </div>
  );
}
