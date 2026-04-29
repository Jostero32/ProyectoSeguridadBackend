package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Información del archivo multimedia adjunto al mensaje")
public record ArchivoMultimediaResponse(

        @Schema(description = "Ruta al endpoint de acceso al archivo (requiere autenticación)")
        String urlAcceso,

        @Schema(description = "Nombre original del archivo")
        String nombreOriginal,

        @Schema(description = "MIME type detectado del archivo")
        String contentType,

        @Schema(description = "Tamaño en bytes")
        long tamanioBytes

) {}
