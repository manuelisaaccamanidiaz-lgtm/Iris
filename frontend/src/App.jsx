/*
 * Las RUTAS de la app: qué componente se muestra para cada URL.
 * Las que van dentro de <RutaPrivada><Layout/></RutaPrivada> exigen sesión y comparten la barra superior.
 */
import { Navigate, Route, Routes } from "react-router";
import RutaPrivada from "./auth/RutaPrivada.jsx";
import Layout from "./componentes/Layout.jsx";
import EventoDetalle from "./paginas/EventoDetalle.jsx";
import Eventos from "./paginas/Eventos.jsx";
import Login from "./paginas/Login.jsx";
import Registro from "./paginas/Registro.jsx";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/registro" element={<Registro />} />
      <Route element={<RutaPrivada><Layout /></RutaPrivada>}>
        <Route index element={<Eventos />} />
        <Route path="eventos/:eventoId" element={<EventoDetalle />} />
      </Route>
      {/* Cualquier otra URL: a la página principal */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
