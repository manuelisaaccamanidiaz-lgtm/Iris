package com.iris.infrastructure.adapter.in.web.documento;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.iris.application.documento.ListarDocumentos;
import com.iris.application.documento.SubirDocumento;
import com.iris.domain.documento.Documento;

@RestController
@RequestMapping("/api/eventos/{eventoId}/documentos")
class DocumentoController {

    private final SubirDocumento subirDocumento;
    private final ListarDocumentos listarDocumentos;

    DocumentoController(SubirDocumento subirDocumento, ListarDocumentos listarDocumentos) {
        this.subirDocumento = subirDocumento;
        this.listarDocumentos = listarDocumentos;
    }

    /**
     * multipart/form-data: el formato estándar del navegador para enviar archivos.
     * 202 Accepted = "lo recibí, pero el trabajo (indexar) sigue después". No es 201 porque
     * el documento aún no está listo para consultarse.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    RespuestaDocumento subir(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId,
                             @RequestParam("archivo") MultipartFile archivo) throws IOException {
        UUID usuarioId = UUID.fromString(jwt.getSubject());
        Documento d = subirDocumento.ejecutar(usuarioId, eventoId, archivo.getOriginalFilename(), archivo.getBytes());
        return RespuestaDocumento.de(d);
    }

    @GetMapping
    List<RespuestaDocumento> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventoId) {
        UUID usuarioId = UUID.fromString(jwt.getSubject());
        return listarDocumentos.ejecutar(usuarioId, eventoId).stream().map(RespuestaDocumento::de).toList();
    }

    /** Sin hash ni ruta temporal: son detalles internos que el cliente no necesita. */
    record RespuestaDocumento(UUID id, String nombreArchivo, long tamanoBytes, Integer paginas,
                              String estado, String errorMensaje, Instant creadoEn, Instant indexadoEn) {

        static RespuestaDocumento de(Documento d) {
            return new RespuestaDocumento(d.id(), d.nombreArchivo(), d.tamanoBytes(), d.paginas(),
                    d.estado().name(), d.errorMensaje(), d.creadoEn(), d.indexadoEn());
        }
    }
}
