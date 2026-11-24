import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useOutletContext } from "react-router";
import { api, mensajeLocal } from "../../api/cliente.js";
import { useConfirmar } from "../../componentes/Confirmacion.jsx";
import "./Documentos.css";

const ESTADO = {
  EN_COLA: { texto: "En cola", clase: "chip-aviso" },
  PROCESANDO: { texto: "Indexando…", clase: "chip-aviso" },
  INDEXADO: { texto: "Listo", clase: "chip-exito" },
  ERROR: { texto: "Error", clase: "chip-error" },
};

const kb = (bytes) => (bytes < 1024 * 1024 ? `${Math.round(bytes / 1024)} KB` : `${(bytes / 1048576).toFixed(1)} MB`);

export default function Documentos() {
  const { evento, puedeEditar } = useOutletContext();
  const confirmar = useConfirmar();
  const [documentos, setDocumentos] = useState(null);
  const [error, setError] = useState(null);
  const [subiendo, setSubiendo] = useState(false);
  const entradaArchivo = useRef(null); // referencia al <input type="file"> para limpiarlo

  // useCallback: la MISMA función entre renders (si no, el efecto de abajo se repetiría sin parar)
  const cargar = useCallback(() => {
    return api(`/api/eventos/${evento.id}/documentos`).then(setDocumentos).catch((e) => setError(mensajeLocal(e)));
  }, [evento.id]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  // Mientras alguno se esté indexando, preguntamos cada 3 s. Cuando todos terminan, paramos
  const enProceso = documentos?.some((d) => d.estado === "EN_COLA" || d.estado === "PROCESANDO");
  useEffect(() => {
    if (!enProceso) return;
    const reloj = setInterval(cargar, 3000);
    return () => clearInterval(reloj); // LIMPIEZA: al salir de la página o al terminar, se apaga el reloj
  }, [enProceso, cargar]);

  async function subir(e) {
    const archivo = e.target.files[0];
    if (!archivo) return;
    setError(null);
    setSubiendo(true);
    const formulario = new FormData();
    formulario.append("archivo", archivo); // el mismo nombre que @RequestParam("archivo") del backend
    try {
      await api(`/api/eventos/${evento.id}/documentos`, { metodo: "POST", formulario });
      await cargar();
    } catch (err) {
      setError(mensajeLocal(err)); // 409 duplicado, 413 muy grande, 400 no es PDF...
    } finally {
      setSubiendo(false);
      entradaArchivo.current.value = ""; // para poder volver a elegir el mismo archivo
    }
  }

  async function borrar(doc) {
    // confirm() es un diálogo del navegador: suficiente para una acción destructiva sencilla
    const ok = await confirmar({
      titulo: `¿Borrar «${doc.nombreArchivo}»?`,
      mensaje: "Se borran también sus fragmentos y las propuestas pendientes de Iris. Las tareas ya creadas se quedan.",
      accion: "Borrar",
      peligro: true,
    });
    if (!ok) return;
    setError(null);
    try {
      await api(`/api/eventos/${evento.id}/documentos/${doc.id}`, { metodo: "DELETE" });
      setDocumentos(documentos.filter((d) => d.id !== doc.id));
    } catch (err) {
      setError(mensajeLocal(err)); // 403 si no lo subiste ni eres propietario; 409 si se está indexando
    }
  }

  return (
    <div className="documentos">
      <div className="documentos-cabecera">
        <p className="texto-suave">
          Iris lee estos documentos para responder y proponer tareas. Su texto se envía a Gemini para indexarlo:
          sube solo documentos del evento.
        </p>
        {puedeEditar && (
          <label className="boton boton-primario">
            {subiendo ? "Subiendo…" : "Subir PDF"}
            <input ref={entradaArchivo} type="file" accept="application/pdf" hidden onChange={subir} disabled={subiendo} />
          </label>
        )}
      </div>

      {error && <p className="aviso-error" role="alert">{error}</p>}
      {documentos === null && <p className="cargando">Cargando documentos…</p>}
      {documentos?.length === 0 && (
        <div className="tarjeta documentos-vacio">
          <h2>Sube tu primer documento</h2>
          <p className="texto-suave">Un contrato, un cronograma o un reglamento. Después, pregúntale a Iris.</p>
        </div>
      )}

      <ul className="documentos-lista">
        {documentos?.map((d) => {
          const estado = ESTADO[d.estado] ?? { texto: d.estado, clase: "" };
          return (
            <li key={d.id} className="tarjeta documento">
              <div className="documento-info">
                <strong>{d.nombreArchivo}</strong>
                <span className="texto-suave">
                  {kb(d.tamanoBytes)}
                  {d.paginas != null && ` · ${d.paginas} páginas`}
                </span>
                {d.estado === "ERROR" && d.errorMensaje && <span className="texto-error">{d.errorMensaje}</span>}
              </div>
              <span className={`chip ${estado.clase}`}>{estado.texto}</span>
              {puedeEditar && d.estado === "INDEXADO" && <Link to="../propuestas">Ver propuestas de Iris</Link>}
              {puedeEditar && (
                <button className="boton boton-peligro" onClick={() => borrar(d)}>
                  Borrar
                </button>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
