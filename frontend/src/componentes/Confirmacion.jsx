/*
 * Reemplazo de window.confirm() con el diseño de Iris.
 *
 * Uso, en cualquier componente:
 *   const confirmar = useConfirmar();
 *   if (!(await confirmar({ titulo: "¿Quitar a Carla?", mensaje: "...", accion: "Quitar", peligro: true }))) return;
 *
 * Por dentro: un <dialog> de HTML con showModal(). El navegador ya le da lo difícil de un modal:
 * fondo oscurecido, el foco atrapado dentro, y Escape para cerrar (accesible sin escribir nada extra).
 * La "magia" es una PROMESA: confirmar() devuelve una promesa que se resuelve true o false
 * cuando la persona hace clic. Por eso se puede usar con await, igual que window.confirm.
 */
import { createContext, useCallback, useContext, useRef, useState } from "react";
import "./Confirmacion.css";

const ConfirmacionContext = createContext(null);

export function ConfirmacionProvider({ children }) {
  const dialogo = useRef(null);
  const resolver = useRef(null); // la función que "contesta" la promesa pendiente
  const [opciones, setOpciones] = useState({ titulo: "", mensaje: "", accion: "Aceptar", peligro: false });

  const confirmar = useCallback((nuevas) => {
    setOpciones({ accion: "Aceptar", peligro: false, ...nuevas });
    dialogo.current.showModal();
    return new Promise((resolve) => {
      resolver.current = resolve;
    });
  }, []);

  function responder(valor) {
    dialogo.current.close();
    resolver.current?.(valor);
    resolver.current = null;
  }

  return (
    <ConfirmacionContext.Provider value={confirmar}>
      {children}
      {/* onCancel: la tecla Escape cuenta como "Cancelar" */}
      <dialog ref={dialogo} className="confirmacion tarjeta" onCancel={() => responder(false)}
        aria-labelledby="confirmacion-titulo">
        <h2 id="confirmacion-titulo">{opciones.titulo}</h2>
        {opciones.mensaje && <p>{opciones.mensaje}</p>}
        <div className="confirmacion-botones">
          <button className="boton" onClick={() => responder(false)} autoFocus>
            Cancelar
          </button>
          <button className={`boton ${opciones.peligro ? "boton-peligro-lleno" : "boton-primario"}`}
            onClick={() => responder(true)}>
            {opciones.accion}
          </button>
        </div>
      </dialog>
    </ConfirmacionContext.Provider>
  );
}

export function useConfirmar() {
  return useContext(ConfirmacionContext);
}
