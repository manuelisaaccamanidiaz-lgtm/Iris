package com.iris.domain.documento;

/** Ciclo de vida de un documento. Coincide con el CHECK ck_documentos_estado. */
public enum EstadoDocumento {
    EN_COLA,
    PROCESANDO,
    INDEXADO,
    ERROR
}
