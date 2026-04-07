package com.seguridad.Messenger.conversacion.dto;

import com.seguridad.Messenger.shared.enums.TipoConversacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Información de una conversación")
public record ConversacionResponse(

        @Schema(description = "Identificador único de la conversación")
        UUID id,

        @Schema(description = "Tipo de conversación", example = "INDIVIDUAL")
        TipoConversacion tipo,

        @Schema(description = "Nombre del grupo, o nombre completo del otro usuario si es chat individual")
        String titulo,

        @Schema(description = "URL del avatar del grupo, o avatar del otro usuario si es chat individual")
        String urlAvatar,

        @Schema(description = "Fecha y hora de creación de la conversación")
        LocalDateTime creadaEn,

        @Schema(description = "Indica si el usuario autenticado es administrador (relevante solo en grupos)")
        boolean esAdmin,

        @Schema(description = "Número total de miembros en la conversación")
        int totalMiembros

) {}
