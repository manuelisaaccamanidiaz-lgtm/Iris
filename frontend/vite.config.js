import fs from 'node:fs'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

/*
 * HTTPS en desarrollo (requisito). Los mismos certificados de mkcert que usa el backend, en la carpeta
 * certs/ de la raíz del proyecto (Git la ignora: la llave privada es de cada equipo).
 * new URL(..., import.meta.url) = ruta relativa a ESTE archivo, sin importar desde dónde corras npm.
 */
const certificado = new URL('../certs/localhost.pem', import.meta.url)
const llave = new URL('../certs/localhost-key.pem', import.meta.url)

/*
 * PWA (decisión de Isaac: opción A, vite-plugin-pwa). Genera las dos piezas de una app instalable:
 *  - el MANIFIESTO: nombre, íconos y colores; "standalone" = su propia ventana, sin la barra del navegador
 *  - el SERVICE WORKER (con Workbox): guarda la "cáscara" de la app (HTML, JS, CSS, íconos) para que abra
 *    rápido y aun sin red. La API está en OTRO origen (https://localhost:8080): el service worker no la
 *    guarda, así que las respuestas de Iris siempre son frescas (nunca una respuesta vieja de caché)
 */
const pwa = VitePWA({
  registerType: 'autoUpdate', // cuando hay una versión nueva, se actualiza sola (sin preguntar)
  devOptions: { enabled: true }, // también en "npm run dev": así se puede instalar en la demo
  includeAssets: ['favicon.svg', 'apple-touch-icon.png'],
  manifest: {
    name: 'Iris', // decisión de Isaac: solo "Iris" (es el nombre de la ventana y del menú Inicio)
    short_name: 'Iris',
    description: 'Asistente para organizadores de eventos: responde con tus documentos y siempre cita la fuente.',
    lang: 'es',
    start_url: '/',
    display: 'standalone',
    theme_color: '#0f3b39', // la barra de la ventana, del color de la barra lateral
    background_color: '#e6efec', // el fondo de la pantalla de carga, el mismo de la app
    icons: [
      { src: 'pwa-192.png', sizes: '192x192', type: 'image/png' },
      { src: 'pwa-512.png', sizes: '512x512', type: 'image/png' },
      // "maskable": versión con margen de seguridad, para sistemas que recortan el ícono en círculo
      { src: 'pwa-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
    ],
  },
})

// defineConfig con una FUNCIÓN: "command" dice si es el servidor de desarrollo ('serve') o 'build'.
// Los certificados solo hacen falta para servir: "npm run build" funciona en cualquier equipo
export default defineConfig(({ command }) => {
  if (command !== 'serve') {
    return { plugins: [react(), pwa] }
  }
  if (!fs.existsSync(certificado) || !fs.existsSync(llave)) {
    // Mejor un mensaje claro que un error raro de TLS más adelante
    throw new Error('Faltan los certificados HTTPS en certs/. Genéralos con mkcert (ver README, sección HTTPS)')
  }
  return {
    plugins: [react(), pwa],
    server: {
      https: {
        cert: fs.readFileSync(certificado),
        key: fs.readFileSync(llave),
      },
    },
  }
})
