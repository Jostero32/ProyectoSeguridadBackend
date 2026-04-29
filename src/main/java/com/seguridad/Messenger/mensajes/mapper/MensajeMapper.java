package com.seguridad.Messenger.mensajes.mapper;

import com.seguridad.Messenger.mensajes.dto.ArchivoMultimediaResponse;
import com.seguridad.Messenger.mensajes.dto.EstadoMensajeResponse;
import com.seguridad.Messenger.mensajes.dto.ForwardResponse;
import com.seguridad.Messenger.mensajes.dto.MensajePayload;
import com.seguridad.Messenger.mensajes.dto.MensajeResponse;
import com.seguridad.Messenger.mensajes.dto.MultimediaPayload;
import com.seguridad.Messenger.mensajes.dto.RepliedMessageResponse;
import com.seguridad.Messenger.mensajes.dto.ResumenReaccionesResponse;
import com.seguridad.Messenger.mensajes.dto.TextoPayload;
import com.seguridad.Messenger.mensajes.dto.UbicacionPayload;
import com.seguridad.Messenger.mensajes.dto.UbicacionResponse;
import com.seguridad.Messenger.mensajes.model.ArchivoMultimedia;
import com.seguridad.Messenger.mensajes.model.Mensaje;
import com.seguridad.Messenger.mensajes.model.Reaccion;
import com.seguridad.Messenger.mensajes.model.UbicacionMensaje;
import com.seguridad.Messenger.shared.enums.TipoMensaje;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MensajeMapper {

    public MensajeResponse toResponse(Mensaje mensaje) {
        return toResponse(mensaje, null, null, null, null);
    }

    public MensajeResponse toResponse(Mensaje mensaje, UUID usuarioId) {
        return toResponse(mensaje, usuarioId, null, null, null);
    }

    public MensajeResponse toResponse(
            Mensaje mensaje,
            UUID usuarioId,
            UUID conversacionId,
            List<EstadoMensajeResponse> estados,
            List<ResumenReaccionesResponse> reacciones) {
        UUID conversacionIdFinal = conversacionId != null
                ? conversacionId
                : (mensaje.getConversacion() != null ? mensaje.getConversacion().getId() : null);
        boolean eliminado = mensaje.getEliminadoEn() != null;
        boolean eliminadoParaTodos = mensaje.isEliminadoParaTodos();
        TipoMensaje tipo = mensaje.getTipo();

        RepliedMessageResponse respuesta = mapRespuesta(mensaje.getRespuestaMensaje());
        List<ResumenReaccionesResponse> reaccionesFinal = reacciones != null
                ? reacciones
                : buildResumen(mensaje.getReacciones(), usuarioId);

        String contenido;
        if (eliminadoParaTodos) {
            contenido = null;
        } else if (mensaje.getEliminadoEn() != null) {
            contenido = (usuarioId != null && mensaje.getRemitenteId().equals(usuarioId))
                    ? null
                    : mensaje.getContenido();
        } else {
            contenido = mensaje.getContenido();
        }

        MensajePayload payload = eliminadoParaTodos
            ? null
            : switch (tipo) {
                case TEXTO -> new TextoPayload(contenido);
                case UBICACION -> new UbicacionPayload(mapUbicacion(mensaje.getUbicacion()));
                case IMAGEN, AUDIO, VIDEO, DOCUMENTO, STICKER, GIF ->
                    new MultimediaPayload(mapArchivo(mensaje.getArchivo()));
            };

        ForwardResponse reenviaDe = mapForward(mensaje.getReenviaDe());

        return new MensajeResponse(
                mensaje.getId(),
                conversacionIdFinal,
                mensaje.getRemitenteId(),
                tipo,
                mensaje.getCreadoEn(),
                mensaje.getEditadoEn(),
                eliminado,
                eliminadoParaTodos,
                respuesta,
                estados,
                reaccionesFinal,
                payload,
                reenviaDe
        );
    }

    private ForwardResponse mapForward(Mensaje raiz) {
        if (raiz == null) return null;
        boolean borrado = raiz.isEliminadoParaTodos();
        String urlAcceso = null;
        String thumbnail = null;
        if (!borrado && raiz.getArchivo() != null) {
            urlAcceso = "/archivos/" + raiz.getArchivo().getObjectKey();
            thumbnail = raiz.getArchivo().getThumbnailBase64();
        }
        return new ForwardResponse(
                raiz.getId(),
                raiz.getRemitenteId(),
                raiz.getTipo(),
                borrado ? null : raiz.getContenido(),
                urlAcceso,
                thumbnail,
                borrado
        );
    }

    private RepliedMessageResponse mapRespuesta(Mensaje original) {
        if (original == null) {
            return null;
        }
        String contenido = original.isEliminadoParaTodos() ? null : original.getContenido();
        boolean eliminado = original.getEliminadoEn() != null;
        return new RepliedMessageResponse(
                original.getId(),
                original.getRemitenteId(),
                contenido,
                eliminado
        );
    }

    private ArchivoMultimediaResponse mapArchivo(ArchivoMultimedia archivo) {
        if (archivo == null) {
            return null;
        }
        return new ArchivoMultimediaResponse(
                "/archivos/" + archivo.getObjectKey(),
                archivo.getNombreOriginal(),
                archivo.getContentType(),
                archivo.getTamanioBytes(),
                archivo.getThumbnailBase64()
        );
    }

    private List<ResumenReaccionesResponse> buildResumen(List<Reaccion> reacciones, UUID usuarioId) {
        if (reacciones == null || reacciones.isEmpty()) {
            return List.of();
        }
        Map<String, Long> conteo = reacciones.stream()
                .collect(Collectors.groupingBy(Reaccion::getEmoji, Collectors.counting()));
        return conteo.entrySet().stream()
                .map(e -> new ResumenReaccionesResponse(
                        e.getKey(),
                        e.getValue(),
                        usuarioId != null && reacciones.stream().anyMatch(r ->
                                r.getId().getUsuarioId().equals(usuarioId)
                                        && r.getEmoji().equals(e.getKey()))
                ))
                .toList();
    }

    private UbicacionResponse mapUbicacion(UbicacionMensaje ubicacion) {
        if (ubicacion == null) {
            return null;
        }
        return new UbicacionResponse(
                ubicacion.getLatitud(),
                ubicacion.getLongitud(),
                ubicacion.getNombreLugar()
        );
    }
}
