package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload para mensajes de texto")
public record TextoPayload(
        @Schema(description = "Contenido del mensaje")
        String contenido
) implements MensajePayload {}
