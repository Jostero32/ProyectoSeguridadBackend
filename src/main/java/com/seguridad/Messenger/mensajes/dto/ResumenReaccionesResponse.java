package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumen de reacciones agrupadas por emoji")
public record ResumenReaccionesResponse(

        @Schema(description = "Emoji de la reacción", example = "👍")
        String emoji,

        @Schema(description = "Cantidad de usuarios que pusieron este emoji")
        long cantidad,

        @Schema(description = "true si el usuario autenticado puso este emoji")
        boolean reaccionaste

) {}
