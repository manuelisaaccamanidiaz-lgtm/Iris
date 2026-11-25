/*
 * El "Api" de PowerShell, pero para React: UNA función para hablar con el backend.
 *  - adjunta el token (si hay sesión)
 *  - manda JSON o un formulario (para subir PDFs)
 *  - convierte los errores del ManejadorErrores (ProblemDetail: title + detail) en ErrorApi
 */
const BASE = import.meta.env.VITE_API_URL ?? "https://localhost:8080";
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
    // GENERAL = no es de un formulario, es de la app: sin conexión (0), sin permiso (403), servidor (5xx)
    this.general = estado === 0 || estado === 403 || estado >= 500;
  }
}

/**
 * El mensaje para el bloque de error DE LA PANTALLA. Los errores generales ya salieron como aviso
 * emergente (componentes/Avisos.jsx): devolver null evita mostrar el mismo error dos veces.
 */
export const mensajeLocal = (e) => (e.general ? null : e.message);

// Qué hacer cuando el token vence (8 h): el AuthContext se suscribe aquí y cierra la sesión
let alExpirar = () => {};
export function cuandoExpire(funcion) {
  alExpirar = funcion;
}

// Qué hacer con un error GENERAL: el AvisosProvider se suscribe aquí y muestra el aviso emergente
let alFallarGeneral = () => {};
export function cuandoFalleGeneral(funcion) {
  alFallarGeneral = funcion;
}

/** Crea el error y, si es general, lo anuncia ANTES de lanzarlo (así nadie tiene que acordarse). */
function fallar(estado, titulo, detalle) {
  const error = new ErrorApi(estado, titulo, detalle);
  if (error.general) alFallarGeneral(error);
  return error;
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
    throw fallar(0, "Sin conexión", "No pude conectarme con Iris. ¿Está encendido el backend?");
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
    throw fallar(
      respuesta.status,
      problema.title ?? "Error",
      problema.detail ?? "Algo salió mal. Inténtalo de nuevo.",
    );
  }
  if (respuesta.status === 204) return null; // "listo, no te devuelvo nada"
  const texto = await respuesta.text();
  return texto ? JSON.parse(texto) : null;
}
