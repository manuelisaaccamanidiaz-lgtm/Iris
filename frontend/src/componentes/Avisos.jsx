/*
 * Avisos EMERGENTES (en inglés se llaman "toasts", como una tostada que salta de la tostadora):
 * un mensaje pequeño en una esquina que aparece, se queda unos segundos y se va solo.
 *
 * Regla de Iris para los errores:
 *  - LOCALES (400 un dato inválido, 409 un conflicto, 404...): en el bloque de error de su pantalla,
 *    al lado del formulario que los causó, porque la persona tiene que corregir algo AHÍ.
 *  - GENERALES (sin conexión, 403 sin permiso, 5xx el servidor o la IA fallaron): aviso emergente,
 *    porque no son de ningún formulario: son de la app entera. Los dispara solo api/cliente.js.
 *
 * Uso en cualquier componente:
 *   const avisos = useAvisos();
 *   avisos.exito("Listo", "Invitación enviada");
 *   avisos.error("Sin permiso", "Las propuestas son solo para editores");
 *
 * Accesibilidad: el contenedor tiene aria-live, así un lector de pantalla LEE el aviso cuando aparece
 * (no es un <span> suelto: es una "región viva"; los errores usan role="alert", que interrumpe).
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { cuandoFalleGeneral } from "../api/cliente.js";
import "./Avisos.css";

const AvisosContext = createContext(null);
const DURACION_MS = 6000;

export function AvisosProvider({ children }) {
  const [lista, setLista] = useState([]);
  const siguienteId = useRef(1);

  const cerrar = useCallback((id) => setLista((actual) => actual.filter((a) => a.id !== id)), []);

  const mostrar = useCallback((tipo, titulo, mensaje) => {
    setLista((actual) => {
      // El mismo aviso ya está en pantalla (p. ej. "sin conexión" de dos peticiones a la vez): no lo repetimos
      if (actual.some((a) => a.titulo === titulo && a.mensaje === mensaje)) return actual;
      const id = siguienteId.current++;
      setTimeout(() => cerrar(id), DURACION_MS);
      return [...actual.slice(-2), { id, tipo, titulo, mensaje }]; // como mucho 3 a la vez
    });
  }, [cerrar]);

  // El cliente de la API nos avisa de los errores GENERALES (mismo truco que cuandoExpire)
  useEffect(() => {
    cuandoFalleGeneral((e) => mostrar("error", e.titulo, e.message));
  }, [mostrar]);

  // useMemo: el objeto no cambia en cada render (si cambiara, los useEffect que lo usan se repetirían)
  const avisos = useMemo(() => ({
    error: (titulo, mensaje) => mostrar("error", titulo, mensaje),
    exito: (titulo, mensaje) => mostrar("exito", titulo, mensaje),
    info: (titulo, mensaje) => mostrar("info", titulo, mensaje),
  }), [mostrar]);

  return (
    <AvisosContext.Provider value={avisos}>
      {children}
      <div className="avisos" aria-live="polite">
        {lista.map((a) => (
          <div key={a.id} className={`aviso aviso-${a.tipo}`} role={a.tipo === "error" ? "alert" : "status"}>
            <div className="aviso-texto">
              <strong>{a.titulo}</strong>
              {a.mensaje && <span>{a.mensaje}</span>}
            </div>
            <button className="aviso-cerrar" onClick={() => cerrar(a.id)} aria-label="Cerrar aviso">×</button>
          </div>
        ))}
      </div>
    </AvisosContext.Provider>
  );
}

export function useAvisos() {
  return useContext(AvisosContext);
}
