package com.seguridad.Messenger.conversacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Configuración personal del usuario para un chat")
public record ConfiguracionChatResponse(

        @Schema(description = "Indica si el chat está actualmente silenciado (silenciadoHasta > ahora)")
        boolean silenciado,

        @Schema(description = "Fecha/hora hasta la que el chat está silenciado. Null si no está silenciado.")
        LocalDateTime silenciadoHasta,

        @Schema(description = "Indica si la conversación está archivada para este usuario")
        boolean archivado

) {}
