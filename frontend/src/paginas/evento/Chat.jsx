/*
 * "Pregúntale a Iris": el chat con streaming. Cada mensaje del historial es
 *   { id, pregunta, respuesta, fuentes, sinEvidencia, enVivo }
 * Mientras Iris escribe, la ÚLTIMA entrada va creciendo trozo a trozo (enVivo: true).
 */
import { useEffect, useRef, useState } from "react";
import { useOutletContext } from "react-router";
import { api, mensajeLocal } from "../../api/cliente.js";
import { preguntarEnVivo } from "../../api/streaming.js";
import "./Chat.css";
import PanelChat from "./PanelChat.jsx";

const SUGERENCIAS = [
  "¿Cuándo hay que pagar el anticipo del hotel?",
  "¿Qué pasa si cancelamos el evento?",
  "¿Qué deben traer los expositores?",
];

/**
 * 6.4 Las citas [n] DENTRO del texto se vuelven chips: "pagará el 1 de dic [1]" -> "... (1)".
 * split con un grupo de captura: "a [1] b".split(/\[(\d+)\]/) = ["a ", "1", " b"]
 * -> las posiciones IMPARES son los números. Funciona también mientras Iris escribe en vivo.
 */
function conCitas(texto) {
  return texto.split(/\[(\d+)\]/).map((parte, i) =>
    i % 2 === 1 ? <span key={i} className="cita-chip" aria-label={`fuente ${parte}`}>{parte}</span> : parte,
  );
}

export default function Chat() {
  const { evento, puedeEditar } = useOutletContext();
  const [mensajes, setMensajes] = useState([]);
  const [pregunta, setPregunta] = useState("");
  const [escribiendo, setEscribiendo] = useState(false);
  const [error, setError] = useState(null);
  const finDelChat = useRef(null);
  const control = useRef(null); // el AbortController de la respuesta en curso

  // Al entrar: mi historial en este evento (el backend lo da del más nuevo al más viejo)
  useEffect(() => {
    api(`/api/eventos/${evento.id}/consultas`)
      .then((historial) => setMensajes([...historial].reverse()))
      .catch((e) => setError(mensajeLocal(e)));
    return () => control.current?.abort(); // si me voy de la página, corto la respuesta en curso
  }, [evento.id]);

  // Cada vez que cambian los mensajes, bajamos hasta el final
  useEffect(() => {
    finDelChat.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [mensajes]);

  /** Cambia SOLO el último mensaje (el que Iris está escribiendo). */
  const actualizarUltimo = (cambio) =>
    setMensajes((actuales) => {
      if (actuales.length === 0) return actuales;
      const copia = [...actuales];
      copia[copia.length - 1] = { ...copia[copia.length - 1], ...cambio(copia[copia.length - 1]) };
      return copia;
    });

  async function enviar(texto) {
    const q = texto.trim();
    if (!q || escribiendo) return;
    setError(null);
    setPregunta("");
    setEscribiendo(true);
    setMensajes((actuales) => [
      ...actuales,
      { id: `nuevo-${Date.now()}`, pregunta: q, respuesta: "", fuentes: [], sinEvidencia: false, enVivo: true },
    ]);
    control.current = new AbortController();
    try {
      await preguntarEnVivo(
        evento.id,
        q,
        {
          alFuentes: () => {}, // las fuentes definitivas (solo las CITADAS) llegan en "fin"
          alTrozo: (t) => actualizarUltimo((m) => ({ respuesta: m.respuesta + t })),
          // "fin" trae la respuesta YA validada (citas inválidas limpiadas) y las fuentes citadas
          alFin: (final) => actualizarUltimo(() => ({ ...final, enVivo: false })),
          alError: (mensaje) => {
            setError(mensaje);
            actualizarUltimo(() => ({ enVivo: false }));
          },
        },
        control.current.signal,
      );
    } catch (e) {
      setError(mensajeLocal(e));
      setMensajes((actuales) => actuales.slice(0, -1)); // no quedó nada útil: quitamos la burbuja
    } finally {
      setEscribiendo(false);
      // Si se detuvo antes del primer trozo, la burbuja no queda vacía
      actualizarUltimo((m) => ({ enVivo: false, respuesta: m.respuesta || "Detuviste la respuesta." }));
    }
  }

  function alTeclear(e) {
    // Enter envía; Shift+Enter hace un salto de línea
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      enviar(pregunta);
    }
  }

  return (
    <div className="chat-con-panel">
      <div className="chat">
        <header className="chat-cabecera">
          <h1>Pregúntale a Iris</h1>
          <p className="texto-suave">Respondo con lo que dicen los documentos de «{evento.nombre}», y siempre te digo de dónde lo saqué.</p>
        </header>
        <div className="chat-mensajes" aria-live="polite">
          {mensajes.length === 0 && (
            <div className="chat-bienvenida">
              <p className="burbuja burbuja-iris">
                Hola, soy Iris. Pregúntame lo que necesites de los documentos de «{evento.nombre}» y te respondo
                citando de dónde lo saqué.
              </p>
              <div className="chat-sugerencias">
                {SUGERENCIAS.map((s) => (
                  <button key={s} className="boton" onClick={() => enviar(s)}>
                    {s}
                  </button>
                ))}
              </div>
            </div>
          )}

          {mensajes.map((m) => (
            <div key={m.id} className="chat-turno">
              <p className="burbuja burbuja-persona">{m.pregunta}</p>
              <div className="burbuja burbuja-iris">
                {m.respuesta ? conCitas(m.respuesta) : m.enVivo && <span className="texto-suave">Iris está leyendo tus documentos…</span>}
                {m.enVivo && m.respuesta && <span className="cursor" aria-hidden="true">▍</span>}
                {!m.enVivo && m.sinEvidencia && (
                  <p className="chip chip-coral chat-aviso">Sin fuentes en tus documentos</p>
                )}
                {!m.enVivo && m.fuentes?.length > 0 && (
                  <ul className="chat-fuentes">
                    {m.fuentes.map((f) => (
                      <li key={f.numero} className="fuente-etiqueta">
                        <span className="cita-chip">{f.numero}</span> {f.documento} · pág. {f.paginas}
                        {f.seccion && ` · ${f.seccion}`}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          ))}
          <div ref={finDelChat} />
        </div>

        {error && <p className="aviso-error" role="alert">{error}</p>}

        <form
          className="chat-entrada tarjeta"
          onSubmit={(e) => {
            e.preventDefault();
            enviar(pregunta);
          }}
        >
          <textarea
            className="campo"
            rows={2}
            placeholder="¿Qué incluye el contrato del hotel?"
            value={pregunta}
            onChange={(e) => setPregunta(e.target.value)}
            onKeyDown={alTeclear}
            aria-label="Tu pregunta para Iris"
          />
          {escribiendo ? (
            <button type="button" className="boton" onClick={() => control.current?.abort()}>
              Detener
            </button>
          ) : (
            <button className="boton boton-primario" disabled={!pregunta.trim()}>
              Preguntar
            </button>
          )}
        </form>
        <p className="texto-suave chat-nota">Iris es una IA y puede equivocarse: revisa las fuentes citadas.</p>
      </div>
      <PanelChat evento={evento} puedeEditar={puedeEditar} />
    </div>
  );
}
