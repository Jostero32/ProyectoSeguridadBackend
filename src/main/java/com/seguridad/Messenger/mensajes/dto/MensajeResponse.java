package com.seguridad.Messenger.mensajes.dto;

import com.seguridad.Messenger.shared.enums.TipoMensaje;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Mensaje con metadatos y payload por tipo")
public record MensajeResponse(

        @Schema(description = "Identificador unico del mensaje")
        UUID id,

        @Schema(description = "ID de la conversacion a la que pertenece")
        UUID conversacionId,

        @Schema(description = "ID del usuario que envio el mensaje")
        UUID remitenteId,

        @Schema(description = "Tipo de mensaje", example = "TEXTO")
        TipoMensaje tipo,

        @Schema(description = "Fecha y hora de envio")
        LocalDateTime creadoEn,

        @Schema(description = "Fecha y hora de la ultima edicion. Null si no fue editado.")
        LocalDateTime editadoEn,

        @Schema(description = "Indica si el mensaje fue eliminado (para mi o para todos)")
        boolean eliminado,

        @Schema(description = "Indica si el mensaje fue eliminado para todos los participantes")
        boolean eliminadoParaTodos,

        @Schema(description = "Datos del mensaje al que se responde. Null si no es una respuesta.")
        RepliedMessageResponse respuestaMensaje,

        @Schema(description = "Estados de entrega y lectura por receptor. Solo se incluye cuando el usuario autenticado es el remitente.")
        List<EstadoMensajeResponse> estados,

        @Schema(description = "Reacciones agrupadas por emoji.")
        List<ResumenReaccionesResponse> reacciones,

        @Schema(description = "Payload especifico segun el tipo de mensaje")
        MensajePayload payload
) {}
