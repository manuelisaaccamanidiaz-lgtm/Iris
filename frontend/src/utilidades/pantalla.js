/*
 * ¿La pantalla es pequeña? Un hook que ESCUCHA una media query de CSS desde JavaScript.
 * Casi todo el responsive es CSS puro (@media). Esto es solo para lo que CSS no puede decidir:
 * por ejemplo, montar la Campanita en UN solo lugar (si la montáramos dos veces y ocultáramos una
 * con CSS, las dos seguirían preguntando al backend cada 30 s).
 */
import { useEffect, useState } from "react";

/** El MISMO corte que usa EventoDetalle.css: por debajo, la barra lateral se vuelve un menú. */
export const CORTE_MENU = "(max-width: 899px)";

/** Desde aquí el panel va AL LADO del chat (Chat.css); por debajo, existe la sección "Resumen". */
export const CORTE_PANEL = "(min-width: 1100px)";

export function useMedia(consulta) {
  const [coincide, setCoincide] = useState(() => window.matchMedia(consulta).matches);

  useEffect(() => {
    const mq = window.matchMedia(consulta);
    const alCambiar = (e) => setCoincide(e.matches);
    mq.addEventListener("change", alCambiar); // al girar el celular o cambiar el tamaño de la ventana
    return () => mq.removeEventListener("change", alCambiar);
  }, [consulta]);

  return coincide;
}
