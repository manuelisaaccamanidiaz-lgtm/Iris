/*
 * ¿Quién está logueado? Un Context de React: cualquier componente lo pregunta con useAuth(),
 * sin pasar el usuario de padre a hijo por props (eso se llama "prop drilling").
 */
import { createContext, useContext, useEffect, useState } from "react";
import { api, cuandoExpire, token } from "../api/cliente.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  // Si ya hay un token guardado (recargaste la página), primero hay que validarlo con /api/yo
  const [cargando, setCargando] = useState(() => Boolean(token.leer()));

  useEffect(() => {
    cuandoExpire(() => setUsuario(null));
    if (!token.leer()) return;
    api("/api/yo")
      .then(setUsuario)
      .catch(() => token.borrar()) // el token ya no sirve: como si no hubiera sesión
      .finally(() => setCargando(false));
  }, []);

  async function entrar(email, password) {
    const respuesta = await api("/api/auth/login", { metodo: "POST", cuerpo: { email, password } });
    token.guardar(respuesta.token);
    setUsuario(await api("/api/yo"));
  }

  async function registrar(nombre, email, password) {
    await api("/api/auth/registro", { metodo: "POST", cuerpo: { nombre, email, password } });
    await entrar(email, password); // registrarse y entrar de una vez
  }

  function salir() {
    token.borrar();
    setUsuario(null);
  }

  return (
    <AuthContext.Provider value={{ usuario, cargando, entrar, registrar, salir }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
