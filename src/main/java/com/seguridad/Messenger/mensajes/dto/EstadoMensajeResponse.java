package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Estado de entrega y lectura de un mensaje para un receptor")
public record EstadoMensajeResponse(

        @Schema(description = "ID del usuario receptor")
        UUID usuarioId,

        @Schema(description = "Fecha y hora en que el mensaje fue entregado al dispositivo. Null si aún no fue entregado.")
        LocalDateTime entregadoEn,

        @Schema(description = "Fecha y hora en que el usuario leyó el mensaje. Null si aún no fue leído.")
        LocalDateTime leidoEn

) {}
