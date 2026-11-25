/*
 * Botón de pantalla completa (decisión de Isaac: opción C), con la Fullscreen API del navegador.
 * Por seguridad, el navegador SOLO permite entrar a pantalla completa como respuesta a un clic:
 * una página no puede apoderarse de toda la pantalla por su cuenta (imagina un sitio falso que imita
 * a Windows). Por eso es un botón y no algo automático al abrir la app.
 */
import { useEffect, useState } from "react";
import Icono from "./Iconos.jsx";

export default function PantallaCompleta({ className = "boton" }) {
  const [activa, setActiva] = useState(() => Boolean(document.fullscreenElement));

  // Si sale con Escape o F11, el botón se entera por el evento "fullscreenchange" y cambia su texto
  useEffect(() => {
    const alCambiar = () => setActiva(Boolean(document.fullscreenElement));
    document.addEventListener("fullscreenchange", alCambiar);
    return () => document.removeEventListener("fullscreenchange", alCambiar);
  }, []);

  // Algunos navegadores (Safari del iPhone) no la soportan: ahí el botón ni aparece
  if (!document.fullscreenEnabled) return null;

  function alternar() {
    if (document.fullscreenElement) document.exitFullscreen();
    else document.documentElement.requestFullscreen().catch(() => {}); // si el navegador lo niega, no pasa nada
  }

  return (
    <button className={className} onClick={alternar} aria-pressed={activa}
      title={activa ? "Salir de pantalla completa (Esc)" : "Pantalla completa"}>
      <Icono nombre={activa ? "contraer" : "expandir"} tamano={16} />
      <span>{activa ? "Salir" : "Pantalla completa"}</span>
    </button>
  );
}
