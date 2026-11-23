import { useParams } from "react-router";

/** Esqueleto: en los siguientes pasos aquí van Documentos, Chat, Agenda, Tareas y Propuestas. */
export default function EventoDetalle() {
  const { eventoId } = useParams(); // el ":eventoId" de la ruta
  return (
    <section className="tarjeta">
      <h1>Evento</h1>
      <p className="texto-suave">Id: {eventoId}. Aquí vendrán los documentos y el chat con Iris (paso 5.2).</p>
    </section>
  );
}
