package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Vista resumida del mensaje al que se está respondiendo")
public record RepliedMessageResponse(

        @Schema(description = "ID del mensaje original")
        UUID id,

        @Schema(description = "ID del remitente del mensaje original")
        UUID remitenteId,

        @Schema(description = "Contenido del mensaje original. Null si fue eliminado para todos.")
        String contenido,

        @Schema(description = "Indica si el mensaje original fue eliminado")
        boolean eliminado

) {}
