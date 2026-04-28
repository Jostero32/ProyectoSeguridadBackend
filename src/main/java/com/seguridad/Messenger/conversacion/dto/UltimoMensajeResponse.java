package com.seguridad.Messenger.conversacion.dto;

import com.seguridad.Messenger.shared.enums.TipoMensaje;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Vista resumida del último mensaje de un chat")
public record UltimoMensajeResponse(

        @Schema(description = "ID del mensaje")
        UUID id,

        @Schema(description = "ID del remitente")
        UUID remitenteId,

        @Schema(description = "Username del remitente")
        String remitenteUsername,

        @Schema(description = "Tipo de mensaje")
        TipoMensaje tipo,

        @Schema(description = "Preview del contenido (máx. 60 chars para texto, o descripción del tipo)")
        String preview,

        @Schema(description = "Fecha y hora de envío")
        LocalDateTime creadoEn,

        @Schema(description = "true si el mensaje fue eliminado para todos")
        boolean eliminado

) {}
