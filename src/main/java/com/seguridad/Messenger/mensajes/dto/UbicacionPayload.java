package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload para mensajes de ubicacion")
public record UbicacionPayload(
        @Schema(description = "Coordenadas geograficas")
        UbicacionResponse ubicacion
) implements MensajePayload {}
