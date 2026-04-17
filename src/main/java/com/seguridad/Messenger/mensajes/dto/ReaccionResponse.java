package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Detalle de una reacción individual")
public record ReaccionResponse(

        @Schema(description = "ID del usuario que reaccionó")
        UUID usuarioId,

        @Schema(description = "Nombre de usuario")
        String username,

        @Schema(description = "Emoji de la reacción")
        String emoji,

        @Schema(description = "Fecha y hora en que se creó o reemplazó la reacción")
        LocalDateTime creadaEn

) {}
