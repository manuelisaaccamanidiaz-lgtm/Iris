/*
 * Panel de ADMINISTRACIÓN (rol global ADMIN). Solo usa los endpoints que ya existían en /api/admin/**:
 * es una herramienta de OPERACIÓN (¿funciona todo?), no un superusuario de los eventos.
 * Decisión de diseño: el administrador NO ve los eventos ni los documentos de nadie (son confidenciales).
 */
import { useEffect, useState } from "react";
import { api, mensajeLocal } from "../api/cliente.js";
import { fecha } from "../utilidades/fechas.js";
import "./Admin.css";

/** Una tarjeta con su estado: cargando, error o resultado. Evita repetir lo mismo 4 veces. */
function useAccion() {
  const [estado, setEstado] = useState({ cargando: false, error: null, datos: null });
  async function ejecutar(promesa) {
    setEstado({ cargando: true, error: null, datos: null });
    try {
      setEstado({ cargando: false, error: null, datos: await promesa });
    } catch (e) {
      setEstado({ cargando: false, error: mensajeLocal(e) ?? "No se pudo completar (mira el aviso)", datos: null });
    }
  }
  return [estado, ejecutar];
}

export default function Admin() {
  const [salud, revisarSalud] = useAccion();
  const [despertador, despertar] = useAccion();
  const [chat, probarChat] = useAccion();
  const [similitud, medir] = useAccion();
  const [mensaje, setMensaje] = useState("Hola, ¿estás funcionando?");
  const [textoA, setTextoA] = useState("Pagar el anticipo del hotel");
  const [textoB, setTextoB] = useState("Realizar el pago del 30 % al hotel");
  const [msSalud, setMsSalud] = useState(null);

  async function revisar() {
    const inicio = performance.now();
    await revisarSalud(api("/api/salud"));
    setMsSalud(Math.round(performance.now() - inicio));
  }

  useEffect(() => {
    revisar(); // al entrar, el estado del servidor
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const porcentaje = similitud.datos ? Math.round(similitud.datos.similitudCoseno * 100) : 0;

  return (
    <section className="admin">
      <header className="admin-cabecera">
        <div>
          <span className="chip chip-coral">Administrador</span>
          <h1>Administración</h1>
          <p className="texto-suave">
            Herramientas de operación para comprobar que Iris funciona. Por privacidad, el administrador no ve los
            eventos ni los documentos de los equipos.
          </p>
        </div>
      </header>

      <div className="admin-rejilla">
        <article className="tarjeta admin-tarjeta">
          <h2>Estado del servidor</h2>
          <p className="texto-suave">Responde la API de Iris (<code>GET /api/salud</code>).</p>
          {salud.datos && (
            <p className="admin-resultado">
              <span className="chip chip-exito">En línea</span> servicio «{salud.datos.servicio}» · {msSalud} ms
            </p>
          )}
          {salud.error && <p className="aviso-error">{salud.error}</p>}
          <button className="boton" onClick={revisar} disabled={salud.cargando}>
            {salud.cargando ? "Revisando…" : "Volver a revisar"}
          </button>
        </article>

        <article className="tarjeta admin-tarjeta">
          <h2>Recordatorios del día</h2>
          <p className="texto-suave">
            Iris los envía sola todos los días a las 6:00 a. m. Este botón lo hace ya (para la demo). No duplica avisos:
            lo que ya se envió hoy no se vuelve a enviar.
          </p>
          {despertador.datos && (
            <ul className="admin-lista">
              <li>Fecha: <strong>{fecha(despertador.datos.fecha)}</strong></li>
              <li>Recordatorios nuevos: <strong>{despertador.datos.recordatoriosEnviados}</strong></li>
              <li>Invitaciones vencidas: <strong>{despertador.datos.invitacionesVencidas}</strong></li>
            </ul>
          )}
          {despertador.error && <p className="aviso-error">{despertador.error}</p>}
          <button className="boton boton-primario" disabled={despertador.cargando}
            onClick={() => despertar(api("/api/admin/recordatorios/ejecutar", { metodo: "POST" }))}>
            {despertador.cargando ? "Ejecutando…" : "Ejecutar ahora"}
          </button>
        </article>

        <article className="tarjeta admin-tarjeta">
          <h2>Modelo de lenguaje (Groq)</h2>
          <p className="texto-suave">Prueba la clave y el modelo con un mensaje, sin documentos de por medio.</p>
          <form className="admin-form" onSubmit={(e) => {
            e.preventDefault();
            probarChat(api(`/api/admin/diagnostico/chat?mensaje=${encodeURIComponent(mensaje)}`));
          }}>
            <input className="campo" value={mensaje} onChange={(e) => setMensaje(e.target.value)}
              aria-label="Mensaje de prueba" required />
            <button className="boton boton-primario" disabled={chat.cargando || !mensaje.trim()}>
              {chat.cargando ? "Preguntando…" : "Probar"}
            </button>
          </form>
          {chat.datos && (
            <div className="admin-resultado">
              <p className="texto-suave">{chat.datos.modelo} · {chat.datos.milisegundos} ms</p>
              {chat.datos.respuesta && <p className="burbuja burbuja-iris">{chat.datos.respuesta}</p>}
              {chat.datos.error && (
                <p className="aviso-error">{chat.datos.error}<br />Causa: {chat.datos.causaRaiz}</p>
              )}
            </div>
          )}
          {chat.error && <p className="aviso-error">{chat.error}</p>}
        </article>

        <article className="tarjeta admin-tarjeta">
          <h2>Embeddings (Gemini)</h2>
          <p className="texto-suave">
            Qué tan parecidos son dos textos para Iris (coseno de 0 a 1). Es la misma medida que usa para buscar
            fragmentos y para no repetir propuestas.
          </p>
          <form className="admin-form admin-form-columna" onSubmit={(e) => {
            e.preventDefault();
            medir(api(`/api/admin/diagnostico/similitud?a=${encodeURIComponent(textoA)}&b=${encodeURIComponent(textoB)}`));
          }}>
            <input className="campo" value={textoA} onChange={(e) => setTextoA(e.target.value)} aria-label="Texto A" required />
            <input className="campo" value={textoB} onChange={(e) => setTextoB(e.target.value)} aria-label="Texto B" required />
            <button className="boton boton-primario" disabled={similitud.cargando}>
              {similitud.cargando ? "Midiendo…" : "Comparar"}
            </button>
          </form>
          {similitud.datos && (
            <div className="admin-resultado">
              <p>
                Similitud: <strong>{similitud.datos.similitudCoseno}</strong>
                {similitud.datos.similitudCoseno >= 0.85 && " · Iris las tomaría como la misma acción"}
              </p>
              {/* La barra es un apoyo visual: el NÚMERO va escrito al lado (no dependemos solo del color) */}
              <div className="admin-medidor" role="img" aria-label={`Similitud de ${porcentaje} %`}>
                <span style={{ width: `${porcentaje}%` }} />
              </div>
              <p className="texto-suave">{similitud.datos.modelo} · {similitud.datos.dimensiones} dimensiones</p>
            </div>
          )}
          {similitud.error && <p className="aviso-error">{similitud.error}</p>}
        </article>
      </div>
    </section>
  );
}
