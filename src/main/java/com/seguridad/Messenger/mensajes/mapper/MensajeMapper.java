package com.seguridad.Messenger.mensajes.mapper;

import com.seguridad.Messenger.mensajes.dto.ArchivoMultimediaResponse;
import com.seguridad.Messenger.mensajes.dto.EstadoMensajeResponse;
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
import com.seguridad.Messenger.mensajes.model.UbicacionMensaje;
import com.seguridad.Messenger.shared.enums.TipoMensaje;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MensajeMapper {

    public MensajeResponse toResponse(Mensaje mensaje) {
        return toResponse(mensaje, null, null, null, null);
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
        List<ResumenReaccionesResponse> reaccionesFinal =
                reacciones != null ? reacciones : List.of();

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
                payload
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
