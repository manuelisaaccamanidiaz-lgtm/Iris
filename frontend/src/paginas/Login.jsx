import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { useAuth } from "../auth/AuthContext.jsx";
import "./Acceso.css";

export default function Login() {
  const { entrar } = useAuth();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  // Formulario "controlado": React es la fuente de verdad de lo que hay escrito
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function alEnviar(evento) {
    evento.preventDefault(); // sin esto, el navegador recargaría la página
    setError(null);
    setEnviando(true);
    try {
      await entrar(email, password);
      navegar(ubicacion.state?.desde ?? "/", { replace: true }); // volver a donde ibas
    } catch (e) {
      setError(e.message);
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
        <p>Pregúntale a Iris. Leo los documentos de tu evento, te respondo citando la fuente y convierto lo que dicen en tareas para tu equipo.</p>
      </section>
      <section className="acceso-formulario">
        <form className="tarjeta formulario" onSubmit={alEnviar}>
          <h1>Entrar</h1>
          {error && <p className="aviso-error" role="alert">{error}</p>}
          <label className="etiqueta">
            Correo
            <input className="campo" type="email" autoComplete="email" required
              value={email} onChange={(e) => setEmail(e.target.value)} placeholder="ana@empresa.com" />
          </label>
          <label className="etiqueta">
            Contraseña
            <input className="campo" type="password" autoComplete="current-password" required
              value={password} onChange={(e) => setPassword(e.target.value)} />
          </label>
          <button className="boton boton-primario" disabled={enviando}>
            {enviando ? "Entrando…" : "Entrar"}
          </button>
          <p className="texto-suave">
            ¿No tienes cuenta? <Link to="/registro">Crear cuenta</Link>
          </p>
        </form>
      </section>
    </div>
  );
}
