/*
 * 6.4 El panel derecho del chat (decisiones de Isaac: 1A solo en el chat, 2B con "lo que necesita tu atención").
 * Lo que haría una buena secretaria al lado del teléfono: QUÉ está pendiente, QUÉ pasa hoy y CON QUÉ documentos
 * cuenta Iris para responder. Todo sale de endpoints que ya existían: el panel no pidió nada nuevo al backend.
 *
 *  1. Necesita tu atención: propuestas por revisar (solo editores) + tareas vencidas o que vencen en 3 días
 *  2. Hoy: las sesiones de hoy con su color de tipo (o la próxima, si hoy no hay nada)
 *  3. Documentos: su estado, y una zona para SOLTAR un PDF (arrastrar y soltar)
 */
import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { api, mensajeLocal } from "../../api/cliente.js";
import { useAvisos } from "../../componentes/Avisos.jsx";
import { diaLargo, fecha, fechaCorta, hora, hoyIso, sumarDias } from "../../utilidades/fechas.js";
import "./PanelChat.css";

const ESTADO_DOC = {
  EN_COLA: { texto: "En cola", clase: "chip-aviso" },
  PROCESANDO: { texto: "Indexando…", clase: "chip-aviso" },
  INDEXADO: { texto: "Listo", clase: "chip-exito" },
  ERROR: { texto: "Error", clase: "chip-error" },
};

function urgencia(t, hoy) {
  if (t.fechaLimite < hoy) return { texto: "Vencida", clase: "chip-error" };
  if (t.fechaLimite === hoy) return { texto: "Vence hoy", clase: "chip-coral" };
  if (t.fechaLimite === sumarDias(hoy, 1)) return { texto: "Vence mañana", clase: "chip-coral" };
  return { texto: `Vence ${fechaCorta(t.fechaLimite)}`, clase: "chip-coral" };
}

/**
 * @param conSubida la zona para soltar un PDF. Es un ATAJO de escritorio (arrastrar desde el explorador
 *                  mientras chateas); en la sección Resumen del celular sobra (decisión de Isaac): para
 *                  subir está Documentos.
 */
export default function PanelChat({ evento, puedeEditar, conSubida = true }) {
  const avisos = useAvisos();
  const base = `/api/eventos/${evento.id}`;
  const hoy = hoyIso();

  const [documentos, setDocumentos] = useState(null);
  const [sesiones, setSesiones] = useState([]);
  const [tareas, setTareas] = useState([]);
  const [porRevisar, setPorRevisar] = useState(0);
  const [arrastrando, setArrastrando] = useState(false);
  const [subiendo, setSubiendo] = useState(false);
  const entrada = useRef(null);

  const cargarDocumentos = useCallback(() => api(`${base}/documentos`).then(setDocumentos).catch(() => setDocumentos([])), [base]);

  useEffect(() => {
    cargarDocumentos();
    api(`${base}/sesiones`).then(setSesiones).catch(() => {});
    api(`${base}/tareas?estado=PENDIENTE`).then(setTareas).catch(() => {});
    if (puedeEditar) api(`${base}/propuestas?estado=PENDIENTE`).then((l) => setPorRevisar(l.length)).catch(() => {});
  }, [base, puedeEditar, cargarDocumentos]);

  // Igual que en Documentos: mientras alguno se indexa, preguntamos cada 3 s
  const enProceso = documentos?.some((d) => d.estado === "EN_COLA" || d.estado === "PROCESANDO");
  useEffect(() => {
    if (!enProceso) return;
    const reloj = setInterval(cargarDocumentos, 3000);
    return () => clearInterval(reloj);
  }, [enProceso, cargarDocumentos]);

  async function subir(archivo) {
    if (!archivo || subiendo) return;
    if (archivo.type !== "application/pdf") {
      avisos.error("Solo PDF", `«${archivo.name}» no es un PDF.`);
      return;
    }
    setSubiendo(true);
    const formulario = new FormData();
    formulario.append("archivo", archivo);
    try {
      await api(`${base}/documentos`, { metodo: "POST", formulario });
      avisos.exito("Documento recibido", `Estoy indexando «${archivo.name}». Luego lo leeré para proponerte acciones.`);
      await cargarDocumentos();
    } catch (e) {
      // 409 duplicado, 413 muy grande: aquí no hay bloque de error propio, así que va como aviso emergente
      const local = mensajeLocal(e);
      if (local) avisos.error("No pude subirlo", local);
    } finally {
      setSubiendo(false);
      if (entrada.current) entrada.current.value = "";
    }
  }

  // Arrastrar y soltar: preventDefault en "dragover" es OBLIGATORIO; sin él, el navegador abre el PDF
  const zona = {
    onDragOver: (e) => { e.preventDefault(); setArrastrando(true); },
    onDragLeave: () => setArrastrando(false),
    onDrop: (e) => { e.preventDefault(); setArrastrando(false); subir(e.dataTransfer.files[0]); },
  };

  const urgentes = tareas
    .filter((t) => t.fechaLimite && t.fechaLimite <= sumarDias(hoy, 3))
    .sort((a, b) => a.fechaLimite.localeCompare(b.fechaLimite))
    .slice(0, 4);
  const deHoy = sesiones.filter((s) => s.fecha === hoy).sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
  const proxima = deHoy.length ? null : sesiones
    .filter((s) => s.fecha > hoy)
    .sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))[0];
  // Idea de Isaac: una tarea sin responsable es una tarea que nadie va a hacer
  const sinResponsable = tareas.filter((t) => !t.responsableId).length;
  const nadaUrgente = urgentes.length === 0 && porRevisar === 0 && sinResponsable === 0;

  return (
    <aside className="panel-chat" aria-label="Resumen del evento">
      <section className="panel-bloque">
        <h2 className="panel-titulo">Necesita tu atención</h2>
        {nadaUrgente && <p className="texto-suave panel-nada">Todo al día: nada vence en 3 días y cada tarea tiene responsable.</p>}
        {porRevisar > 0 && (
          <Link to="../propuestas" className="panel-atencion panel-atencion-iris">
            <span className="lateral-contador">{porRevisar}</span>
            {porRevisar === 1 ? "propuesta de Iris por revisar" : "propuestas de Iris por revisar"}
          </Link>
        )}
        {sinResponsable > 0 && (
          <Link to="../tareas" className="panel-atencion">
            <span className="panel-atencion-titulo">
              {sinResponsable === 1 ? "1 tarea pendiente sin responsable" : `${sinResponsable} tareas pendientes sin responsable`}
            </span>
            <span className="chip chip-aviso">Asignar</span>
          </Link>
        )}
        {urgentes.map((t) => {
          const u = urgencia(t, hoy);
          return (
            <Link key={t.id} to="../tareas" className="panel-atencion">
              <span className="panel-atencion-titulo">{t.titulo}</span>
              <span className={`chip ${u.clase}`}>{u.texto}</span>
            </Link>
          );
        })}
      </section>

      <section className="panel-bloque">
        <h2 className="panel-titulo">Hoy <span className="texto-suave">· {diaLargo(hoy)}</span></h2>
        {deHoy.length === 0 && (
          <p className="texto-suave panel-nada">
            Nada agendado para hoy.
            {proxima && <> La próxima: <strong>{proxima.titulo}</strong>, {fecha(proxima.fecha)} a las {hora(proxima.horaInicio)}.</>}
          </p>
        )}
        <ul className="panel-lista">
          {deHoy.map((s) => (
            <li key={s.id} className={`panel-sesion tipo-${s.tipo}`}>
              <span className="panel-sesion-hora">{hora(s.horaInicio)}</span>
              <span className="panel-sesion-info">
                <strong>{s.titulo}</strong>
                {s.sala && <span className="texto-suave">{s.sala}</span>}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section className="panel-bloque">
        <h2 className="panel-titulo">Documentos</h2>
        {documentos?.length === 0 && <p className="texto-suave panel-nada">Iris todavía no tiene documentos para responder.</p>}
        <ul className="panel-lista">
          {documentos?.map((d) => {
            const e = ESTADO_DOC[d.estado] ?? { texto: d.estado, clase: "" };
            return (
              <li key={d.id} className="panel-documento">
                <span className="panel-documento-nombre" title={d.nombreArchivo}>{d.nombreArchivo}</span>
                <span className={`chip ${e.clase}`}>{e.texto}</span>
              </li>
            );
          })}
        </ul>
        {puedeEditar && conSubida && (
          // Un <label> que envuelve el input: hacer clic (o Enter con el teclado) abre el selector de archivos
          <label className={`panel-soltar ${arrastrando ? "arrastrando" : ""}`} {...zona}>
            <input ref={entrada} type="file" accept="application/pdf" className="solo-lectores"
              onChange={(e) => subir(e.target.files[0])} disabled={subiendo} />
            {subiendo ? "Subiendo…" : arrastrando ? "Suéltalo aquí" : "Arrastra un PDF aquí o haz clic para elegirlo"}
          </label>
        )}
      </section>
    </aside>
  );
}
