import { useEffect, useState } from "react";
import { Link } from "react-router";
import { api } from "../api/cliente.js";
import { fecha } from "../utilidades/fechas.js";
import "./Eventos.css";

const ROL = { PROPIETARIO: "Propietario", EDITOR: "Editor", LECTOR: "Lector" };

export default function Eventos() {
  const [eventos, setEventos] = useState(null); // null = cargando; [] = no hay
  const [error, setError] = useState(null);
  const [creando, setCreando] = useState(false);
  const [nuevo, setNuevo] = useState({ nombre: "", lugar: "", fechaInicio: "", fechaFin: "" });

  // useEffect con [] = "al aparecer la página, una sola vez"
  useEffect(() => {
    api("/api/eventos").then(setEventos).catch((e) => setError(e.message));
  }, []);

  const cambiar = (campo) => (e) => setNuevo({ ...nuevo, [campo]: e.target.value });

  // AVISO SUAVE (decisión de Isaac): ¿las fechas nuevas se cruzan con otro evento mío?
  // Dos rangos se cruzan si cada uno empieza antes de que termine el otro.
  // Las fechas "AAAA-MM-DD" se pueden comparar como texto: el orden alfabético es el cronológico
  const cruces =
    nuevo.fechaInicio && nuevo.fechaFin
      ? (eventos ?? []).filter((e) => nuevo.fechaInicio <= e.fechaFin && nuevo.fechaFin >= e.fechaInicio)
      : [];

  async function crear(evento) {
    evento.preventDefault();
    setError(null);
    try {
      const creado = await api("/api/eventos", { metodo: "POST", cuerpo: nuevo });
      setEventos([...eventos, creado]); // nuevo arreglo: React solo re-pinta si la referencia cambia
      setNuevo({ nombre: "", lugar: "", fechaInicio: "", fechaFin: "" });
      setCreando(false);
    } catch (e) {
      setError(e.message); // p. ej. 409 "Ya tienes un evento con ese nombre..."
    }
  }

  if (eventos === null && !error) return <p className="cargando">Cargando tus eventos…</p>;

  return (
    <section className="eventos">
      <div className="eventos-cabecera">
        <div>
          <h1>Mis eventos</h1>
          <p className="texto-suave">Los eventos donde eres parte del equipo.</p>
        </div>
        <button className="boton boton-primario" onClick={() => setCreando(!creando)}>
          {creando ? "Cancelar" : "Nuevo evento"}
        </button>
      </div>

      {error && <p className="aviso-error" role="alert">{error}</p>}

      {creando && (
        <form className="tarjeta formulario eventos-formulario" onSubmit={crear}>
          <label className="etiqueta">Nombre<input className="campo" required value={nuevo.nombre} onChange={cambiar("nombre")} /></label>
          <label className="etiqueta">Lugar<input className="campo" value={nuevo.lugar} onChange={cambiar("lugar")} /></label>
          <label className="etiqueta">Inicio<input className="campo" type="date" required value={nuevo.fechaInicio} onChange={cambiar("fechaInicio")} /></label>
          <label className="etiqueta">Fin<input className="campo" type="date" required value={nuevo.fechaFin} onChange={cambiar("fechaFin")} /></label>
          <button className="boton boton-primario">Crear evento</button>
          {cruces.length > 0 && (
            <p className="chip chip-coral eventos-cruce">
              Ojo: estas fechas se cruzan con {cruces.map((e) => `«${e.nombre}»`).join(", ")}. Puedes crearlo igual.
            </p>
          )}
        </form>
      )}

      {eventos?.length === 0 && (
        <div className="tarjeta eventos-vacio">
          <h2>Crea tu primer evento</h2>
          <p className="texto-suave">Después sube sus documentos y pregúntale a Iris.</p>
        </div>
      )}

      <div className="eventos-lista">
        {/* key: React necesita un id ESTABLE para saber qué tarjeta es cuál al re-pintar */}
        {eventos?.map((e) => (
          <Link key={e.id} to={`/eventos/${e.id}`} className="tarjeta evento-tarjeta">
            <span className="chip">{ROL[e.miRol] ?? e.miRol}</span>
            <h2>{e.nombre}</h2>
            <p className="texto-suave">
              {fecha(e.fechaInicio)} – {fecha(e.fechaFin)}
              {e.lugar && ` · ${e.lugar}`}
            </p>
          </Link>
        ))}
      </div>
    </section>
  );
}
