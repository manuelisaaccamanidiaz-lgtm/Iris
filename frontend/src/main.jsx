import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router";
import App from "./App.jsx";
import { AuthProvider } from "./auth/AuthContext.jsx";
import { ConfirmacionProvider } from "./componentes/Confirmacion.jsx";
import "./index.css";

// Aplicar el tema guardado ANTES de pintar, para que el login no "parpadee" en claro
const temaGuardado = localStorage.getItem("iris.tema");
if (temaGuardado) document.documentElement.dataset.theme = temaGuardado;

createRoot(document.getElementById("root")).render(
  <StrictMode>
    {/* BrowserRouter: escucha la URL. AuthProvider: comparte la sesión con toda la app */}
    <BrowserRouter>
      <AuthProvider>
        <ConfirmacionProvider>
          <App />
        </ConfirmacionProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
