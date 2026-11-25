import fs from 'node:fs'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

/*
 * HTTPS en desarrollo (requisito). Los mismos certificados de mkcert que usa el backend, en la carpeta
 * certs/ de la raíz del proyecto (Git la ignora: la llave privada es de cada equipo).
 * new URL(..., import.meta.url) = ruta relativa a ESTE archivo, sin importar desde dónde corras npm.
 */
const certificado = new URL('../certs/localhost.pem', import.meta.url)
const llave = new URL('../certs/localhost-key.pem', import.meta.url)

// defineConfig con una FUNCIÓN: "command" dice si es el servidor de desarrollo ('serve') o 'build'.
// Los certificados solo hacen falta para servir: "npm run build" funciona en cualquier equipo
export default defineConfig(({ command }) => {
  if (command !== 'serve') {
    return { plugins: [react()] }
  }
  if (!fs.existsSync(certificado) || !fs.existsSync(llave)) {
    // Mejor un mensaje claro que un error raro de TLS más adelante
    throw new Error('Faltan los certificados HTTPS en certs/. Genéralos con mkcert (ver README, sección HTTPS)')
  }
  return {
    plugins: [react()],
    server: {
      https: {
        cert: fs.readFileSync(certificado),
        key: fs.readFileSync(llave),
      },
    },
  }
})
