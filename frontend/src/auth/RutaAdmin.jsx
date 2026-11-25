/*
 * Portero de las pantallas de ADMINISTRADOR. Igual que RutaPrivada: es COMODIDAD, no seguridad.
 * La seguridad real sigue en el backend: SecurityConfig exige hasRole("ADMIN") en /api/admin/**,
 * y el rol viaja FIRMADO dentro del token (nadie puede cambiarlo desde el navegador).
 */
import { useEffect, useRef } from "react";
import { Navigate } from "react-router";
import { useAvisos } from "../componentes/Avisos.jsx";
import { useAuth } from "./AuthContext.jsx";

export default function RutaAdmin({ children }) {
  const { usuario } = useAuth();
  const avisos = useAvisos();
  const esAdmin = usuario?.rol === "ADMIN";
  const yaAvisado = useRef(false); // StrictMode corre los efectos dos veces en desarrollo

  useEffect(() => {
    if (esAdmin || yaAvisado.current) return;
    yaAvisado.current = true;
    avisos.error("No tienes permiso", "La administración es solo para la cuenta de administrador.");
  }, [esAdmin, avisos]);

  return esAdmin ? children : <Navigate to="/" replace />;
}
