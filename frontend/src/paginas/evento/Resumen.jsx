/*
 * 6.4 "Resumen" (idea de Isaac): en celular y tablet NO hay panel al lado del chat, así que lo pendiente,
 * lo de hoy y los documentos viven en su propia sección del menú. Es el MISMO componente del panel:
 * cero código repetido, solo otro lugar donde mostrarlo. Sin la zona de subida: en el celular sobra.
 */
import { useOutletContext } from "react-router";
import PanelChat from "./PanelChat.jsx";

export default function Resumen() {
  const { evento, puedeEditar } = useOutletContext();
  return (
    <div className="resumen">
      <h1>Resumen</h1>
      <PanelChat evento={evento} puedeEditar={puedeEditar} conSubida={false} />
    </div>
  );
}
