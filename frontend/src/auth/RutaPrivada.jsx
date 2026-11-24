/*
 * El "portero" del frontend: si no hay sesión, te manda a /login y recuerda a dónde ibas.
 * OJO: esto es comodidad, NO seguridad. La seguridad real está en el backend (JWT + guardas):
 * cualquiera puede saltarse un if de JavaScript, pero no puede falsificar un token firmado.
 */
import { Navigate, useLocation } from "react-router";
import { useAuth } from "./AuthContext.jsx";

export default function RutaPrivada({ children }) {
  const { usuario, cargando } = useAuth();
  const ubicacion = useLocation();

  if (cargando) return <p className="cargando">Cargando…</p>;
  if (!usuario) return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />;
  return children;
}
