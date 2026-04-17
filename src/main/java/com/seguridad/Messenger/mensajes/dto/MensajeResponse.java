package com.seguridad.Messenger.mensajes.dto;

import com.seguridad.Messenger.shared.enums.TipoMensaje;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Información completa de un mensaje")
public record MensajeResponse(

        @Schema(description = "Identificador único del mensaje")
        UUID id,

        @Schema(description = "ID de la conversación a la que pertenece")
        UUID conversacionId,

        @Schema(description = "ID del usuario que envió el mensaje")
        UUID remitenteId,

        @Schema(description = "Contenido del mensaje. Null si fue eliminado para todos o para el usuario que consulta.")
        String contenido,

        @Schema(description = "Tipo de mensaje", example = "TEXTO")
        TipoMensaje tipo,

        @Schema(description = "Fecha y hora de envío")
        LocalDateTime creadoEn,

        @Schema(description = "Fecha y hora de la última edición. Null si no fue editado.")
        LocalDateTime editadoEn,

        @Schema(description = "Indica si el mensaje fue eliminado (para mí o para todos)")
        boolean eliminado,

        @Schema(description = "Indica si el mensaje fue eliminado para todos los participantes")
        boolean eliminadoParaTodos,

        @Schema(description = "Datos del mensaje al que se responde. Null si no es una respuesta.")
        RepliedMessageResponse respuestaMensaje,

        @Schema(description = "Estados de entrega y lectura por receptor. Solo se incluye cuando el usuario autenticado es el remitente.")
        List<EstadoMensajeResponse> estados,

        @Schema(description = "Archivo multimedia adjunto. Null si el mensaje no tiene archivo.")
        ArchivoMultimediaResponse archivo,

        @Schema(description = "Ubicación geográfica. Null si el mensaje no es de tipo UBICACION.")
        UbicacionResponse ubicacion,

        @Schema(description = "Reacciones agrupadas por emoji.")
        List<ResumenReaccionesResponse> reacciones,

        @Schema(description = "Datos del mensaje original reenviado. Null si no es un forward.")
        ForwardResponse reenviaDe

) {}
