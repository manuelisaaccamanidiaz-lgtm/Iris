/*
 * Íconos propios en SVG (sin librerías: son 7 y así no sumamos una dependencia).
 * stroke="currentColor": toman el color del texto que los rodea, así sirven en claro y en oscuro.
 * aria-hidden: son decoración; el TEXTO al lado es lo que lee el lector de pantalla.
 */
const TRAZOS = {
  chat: <path d="M4 5h16v11H9l-5 4V5z" />,
  documentos: <><path d="M7 3h7l5 5v13H7z" /><path d="M14 3v5h5M10 13h6M10 17h6" /></>,
  agenda: <><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M4 10h16M9 3v4M15 3v4" /></>,
  tareas: <><rect x="4" y="4" width="16" height="16" rx="3" /><path d="M8.5 12l2.5 2.5 4.5-5" /></>,
  propuestas: <path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM18 16l.8 2.2L21 19l-2.2.8L18 22l-.8-2.2L15 19l2.2-.8z" />,
  equipo: <><circle cx="9" cy="8" r="3.5" /><path d="M2.5 20c.8-3.5 3.4-5.5 6.5-5.5s5.7 2 6.5 5.5" /><path d="M16 4.8a3.5 3.5 0 010 6.4M18 14.8c1.8.8 3 2.6 3.5 5.2" /></>,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  resumen: <><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4.5V3h6v1.5M9 10h6M9 14h6M9 18h3" /></>,
  volver: <path d="M15 5l-7 7 7 7" />,
  expandir: <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" />,
  contraer: <path d="M9 4v5H4M15 4v5h5M9 20v-5H4M15 20v-5h5" />,
};

export default function Icono({ nombre, tamano = 20 }) {
  return (
    <svg width={tamano} height={tamano} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
      {TRAZOS[nombre]}
    </svg>
  );
}
