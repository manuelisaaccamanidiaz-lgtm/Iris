/*
 * Fechas para mostrar. Dos tipos llegan del backend:
 *  - LocalDate "2026-12-09"        (fechas de eventos, tareas, sesiones): SIN hora ni zona
 *  - Instant  "2026-11-23T15:04:05Z" (cuándo pasó algo): un instante en UTC
 */
const ES = "es-CO";

/** "2026-12-09" -> "9 dic 2026". OJO: new Date("2026-12-09") se lee en UTC y en Bogotá daría el día 8 */
export function fecha(iso) {
  if (!iso) return "";
  const [a, m, d] = iso.split("-").map(Number);
  return new Date(a, m - 1, d).toLocaleDateString(ES, { day: "numeric", month: "short", year: "numeric" });
}

/** Un instante UTC en la hora local del navegador: "23 nov, 3:04 p. m." */
export function fechaHora(instante) {
  if (!instante) return "";
  return new Date(instante).toLocaleString(ES, { day: "numeric", month: "short", hour: "numeric", minute: "2-digit" });
}

/** "2026-12-09" -> "9 dic" (sin año: para espacios chicos, cuando el año se entiende) */
export function fechaCorta(iso) {
  if (!iso) return "";
  const [a, m, d] = iso.split("-").map(Number);
  return new Date(a, m - 1, d).toLocaleDateString(ES, { day: "numeric", month: "short" });
}

/** "hace 5 min", "hace 2 h", "hace 3 d" (para la campanita) */
export function haceCuanto(instante) {
  const minutos = Math.round((Date.now() - new Date(instante).getTime()) / 60000);
  if (minutos < 1) return "ahora";
  if (minutos < 60) return `hace ${minutos} min`;
  if (minutos < 1440) return `hace ${Math.round(minutos / 60)} h`;
  return `hace ${Math.round(minutos / 1440)} d`;
}

/** Hoy en la zona del navegador como "AAAA-MM-DD" (toISOString() daría el día en UTC: mañana desde las 7 p. m.) */
export function hoyIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

/** Suma días a una fecha "AAAA-MM-DD". */
export function sumarDias(iso, dias) {
  const [a, m, d] = iso.split("-").map(Number);
  const f = new Date(a, m - 1, d + dias);
  return `${f.getFullYear()}-${String(f.getMonth() + 1).padStart(2, "0")}-${String(f.getDate()).padStart(2, "0")}`;
}

/** "09:00:00" -> "9:00 a. m." */
export function hora(texto) {
  if (!texto) return "";
  const [h, m] = texto.split(":").map(Number);
  return new Date(2000, 0, 1, h, m).toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" });
}

/** "2026-12-09" -> "martes 9 de diciembre" (cabeceras de la agenda) */
export function diaLargo(iso) {
  const [a, m, d] = iso.split("-").map(Number);
  return new Date(a, m - 1, d).toLocaleDateString("es-CO", { weekday: "long", day: "numeric", month: "long" });
}
