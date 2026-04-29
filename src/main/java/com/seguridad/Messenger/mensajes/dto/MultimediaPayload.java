package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload para mensajes con archivo multimedia")
public record MultimediaPayload(
        @Schema(description = "Archivo adjunto")
        ArchivoMultimediaResponse archivo
) implements MensajePayload {}
