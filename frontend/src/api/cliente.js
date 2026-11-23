/*
 * El "Api" de PowerShell, pero para React: UNA función para hablar con el backend.
 *  - adjunta el token (si hay sesión)
 *  - manda JSON o un formulario (para subir PDFs)
 *  - convierte los errores del ManejadorErrores (ProblemDetail: title + detail) en ErrorApi
 */
const BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
const CLAVE_TOKEN = "iris.token";

/** El "gafete": en sessionStorage, se borra al cerrar la pestaña (decisión de Isaac). */
export const token = {
  leer: () => sessionStorage.getItem(CLAVE_TOKEN),
  guardar: (valor) => sessionStorage.setItem(CLAVE_TOKEN, valor),
  borrar: () => sessionStorage.removeItem(CLAVE_TOKEN),
};

/** Un error de la API con su código HTTP y el mensaje que escribió el backend. */
export class ErrorApi extends Error {
  constructor(estado, titulo, detalle) {
    super(detalle);
    this.estado = estado;
    this.titulo = titulo;
  }
}

// Qué hacer cuando el token vence (8 h): el AuthContext se suscribe aquí y cierra la sesión
let alExpirar = () => {};
export function cuandoExpire(funcion) {
  alExpirar = funcion;
}

/** URL completa de una ruta del backend (la usa el chat en streaming, que necesita fetch directo). */
export const url = (ruta) => BASE + ruta;

/**
 * @param ruta        "/api/eventos"
 * @param metodo      "GET", "POST", "PUT", "PATCH", "DELETE"
 * @param cuerpo      un objeto que se envía como JSON
 * @param formulario  un FormData (archivos); excluye a "cuerpo"
 */
export async function api(ruta, { metodo = "GET", cuerpo, formulario } = {}) {
  const cabeceras = {};
  const t = token.leer();
  if (t) cabeceras.Authorization = `Bearer ${t}`;

  let body;
  if (formulario) {
    body = formulario; // el navegador pone solo el Content-Type multipart con su "boundary"
  } else if (cuerpo !== undefined) {
    cabeceras["Content-Type"] = "application/json";
    body = JSON.stringify(cuerpo);
  }

  let respuesta;
  try {
    respuesta = await fetch(BASE + ruta, { method: metodo, headers: cabeceras, body });
  } catch {
    // fetch solo lanza si NO hubo respuesta: backend apagado, sin red, CORS bloqueado
    throw new ErrorApi(0, "Sin conexión", "No pude conectarme con Iris. ¿Está encendido el backend?");
  }

  // 401 CON token = el token venció o es inválido: cerramos la sesión
  if (respuesta.status === 401 && t) {
    token.borrar();
    alExpirar();
  }
  if (!respuesta.ok) {
    let problema = {};
    try {
      problema = await respuesta.json();
    } catch {
      // la respuesta de error no traía JSON
    }
    throw new ErrorApi(
      respuesta.status,
      problema.title ?? "Error",
      problema.detail ?? "Algo salió mal. Inténtalo de nuevo.",
    );
  }
  if (respuesta.status === 204) return null; // "listo, no te devuelvo nada"
  const texto = await respuesta.text();
  return texto ? JSON.parse(texto) : null;
}
