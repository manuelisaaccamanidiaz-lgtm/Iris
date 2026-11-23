import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { useAuth } from "../auth/AuthContext.jsx";
import "./Acceso.css";

export default function Registro() {
  const { registrar } = useAuth();
  const navegar = useNavigate();
  // Un solo estado para todo el formulario: un objeto con tres campos
  const [datos, setDatos] = useState({ nombre: "", email: "", password: "" });
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  // Actualiza SOLO el campo que cambió; "...datos" copia los demás (objetos inmutables, como en Java)
  const cambiar = (campo) => (e) => setDatos({ ...datos, [campo]: e.target.value });

  async function alEnviar(evento) {
    evento.preventDefault();
    setError(null);
    setEnviando(true);
    try {
      await registrar(datos.nombre, datos.email, datos.password);
      navegar("/", { replace: true });
    } catch (e) {
      setError(e.message); // p. ej. 409 "Ese email ya está registrado" del backend
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="acceso">
      <section className="acceso-bienvenida">
        <span className="logo">
          Iris<span>.</span>
        </span>
        <p>Crea tu cuenta y organiza tu primer evento. Si alguien ya te invitó, verás la invitación al entrar.</p>
      </section>
      <section className="acceso-formulario">
        <form className="tarjeta formulario" onSubmit={alEnviar}>
          <h1>Crear cuenta</h1>
          {error && <p className="aviso-error" role="alert">{error}</p>}
          <label className="etiqueta">
            Nombre
            <input className="campo" required autoComplete="name" value={datos.nombre} onChange={cambiar("nombre")} />
          </label>
          <label className="etiqueta">
            Correo
            <input className="campo" type="email" required autoComplete="email" value={datos.email} onChange={cambiar("email")} />
          </label>
          <label className="etiqueta">
            Contraseña
            <input className="campo" type="password" required minLength={8} autoComplete="new-password"
              value={datos.password} onChange={cambiar("password")} />
            <span className="texto-suave">Mínimo 8 caracteres.</span>
          </label>
          <button className="boton boton-primario" disabled={enviando}>
            {enviando ? "Creando…" : "Crear cuenta"}
          </button>
          <p className="texto-suave">
            ¿Ya tienes cuenta? <Link to="/login">Entrar</Link>
          </p>
        </form>
      </section>
    </div>
  );
}
