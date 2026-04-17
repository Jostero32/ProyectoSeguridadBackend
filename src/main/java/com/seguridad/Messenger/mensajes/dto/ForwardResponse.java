package com.seguridad.Messenger.mensajes.dto;

import com.seguridad.Messenger.shared.enums.TipoMensaje;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Datos mínimos del mensaje original al que se hace forward. " +
        "El campo contenido puede ser null si el mensaje original fue eliminado para todos.")
public record ForwardResponse(

        @Schema(description = "ID del mensaje original")
        UUID mensajeId,

        @Schema(description = "ID del remitente original")
        UUID remitenteId,

        @Schema(description = "Contenido del mensaje original. Null si fue eliminado para todos.")
        String contenido,

        @Schema(description = "Tipo del mensaje original")
        TipoMensaje tipo,

        @Schema(description = "true si el mensaje original fue eliminado")
        boolean eliminado

) {}
