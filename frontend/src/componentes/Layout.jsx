/*
 * El "marco" de todas las páginas con sesión: barra superior + el contenido de la ruta actual.
 * <Outlet /> es el hueco donde React Router pinta la página hija (Eventos, Documentos...).
 */
import { useEffect, useState } from "react";
import { Link, NavLink, Outlet } from "react-router";
import { useAuth } from "../auth/AuthContext.jsx";
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

  // Efecto: cada vez que cambia "tema", lo aplicamos al <html> y lo recordamos
  useEffect(() => {
    document.documentElement.dataset.theme = tema;
    localStorage.setItem("iris.tema", tema);
  }, [tema]);

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
        </nav>
        <div className="barra-derecha">
          <button
            className="boton"
            onClick={() => setTema(tema === "dark" ? "light" : "dark")}
            aria-label="Cambiar entre modo claro y oscuro"
          >
            {tema === "dark" ? "Modo claro" : "Modo oscuro"}
          </button>
          <span className="texto-suave">{usuario?.email}</span>
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
