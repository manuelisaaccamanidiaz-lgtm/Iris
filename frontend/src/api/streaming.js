/*
 * El chat EN VIVO: leer Server-Sent Events (SSE) con fetch + reader.
 *
 * ¿Por qué no EventSource (lo "normal" del navegador para SSE)? Porque EventSource solo hace GET
 * y NO deja mandar el header Authorization. Nuestro endpoint es POST /consultas/stream con JWT.
 * Así que leemos la respuesta a mano, trozo a trozo, igual que tu "EnVivo" de PowerShell.
 *
 * Formato SSE que manda Spring (un evento = líneas "event:" y "data:", y una línea vacía al final):
 *   event:trozo
 *   data:{"t":"El anticipo"}
 *
 * Orden: fuentes -> trozo, trozo, trozo... -> fin   (o "error" si algo falla a mitad)
 */
import { ErrorApi, token, url } from "./cliente.js";

export async function preguntarEnVivo(eventoId, pregunta, oyente, senal) {
  let respuesta;
  try {
    respuesta = await fetch(url(`/api/eventos/${eventoId}/consultas/stream`), {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token.leer()}`,
        "Content-Type": "application/json",
        Accept: "text/event-stream",
      },
      body: JSON.stringify({ pregunta }),
      signal: senal, // AbortController: el botón "Detener" corta la conexión
    });
  } catch (e) {
    if (e.name === "AbortError") return;
    throw new ErrorApi(0, "Sin conexión", "No pude conectarme con Iris. ¿Está encendido el backend?");
  }

  // Los errores de ANTES de empezar (pregunta vacía 400, evento ajeno 404) llegan como un JSON normal
  if (!respuesta.ok) {
    let p = {};
    try {
      p = await respuesta.json();
    } catch {
      // sin cuerpo
    }
    throw new ErrorApi(respuesta.status, p.title ?? "Error", p.detail ?? "Iris no pudo responder.");
  }

  const lector = respuesta.body.getReader();
  const decodificador = new TextDecoder("utf-8");
  let pendiente = ""; // lo que llegó pero aún no forma un evento completo

  try {
    for (;;) {
      const { value, done } = await lector.read();
      if (done) break;
      // stream: true -> si una tilde llega partida entre dos trozos, la junta bien
      pendiente += decodificador.decode(value, { stream: true }).replaceAll("\r\n", "\n");
      let corte;
      // Un evento termina con una línea vacía ("\n\n"): procesamos todos los completos
      while ((corte = pendiente.indexOf("\n\n")) >= 0) {
        despachar(pendiente.slice(0, corte), oyente);
        pendiente = pendiente.slice(corte + 2);
      }
    }
  } catch (e) {
    if (e.name !== "AbortError") throw e;
  }
}

/** Convierte un bloque "event:...\ndata:..." en una llamada al oyente correspondiente. */
function despachar(bloque, oyente) {
  let nombre = "message";
  const datos = [];
  for (const linea of bloque.split("\n")) {
    if (linea.startsWith("event:")) nombre = linea.slice(6).trim();
    else if (linea.startsWith("data:")) datos.push(linea.slice(5).replace(/^ /, ""));
  }
  if (datos.length === 0) return;
  const json = JSON.parse(datos.join("\n"));
  if (nombre === "fuentes") oyente.alFuentes?.(json);
  else if (nombre === "trozo") oyente.alTrozo?.(json.t);
  else if (nombre === "fin") oyente.alFin?.(json);
  else if (nombre === "error") oyente.alError?.(json.mensaje);
}
