package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Información del archivo multimedia adjunto al mensaje")
public record ArchivoMultimediaResponse(

        @Schema(description = "URL pública del archivo")
        String url,

        @Schema(description = "Nombre original del archivo")
        String nombreOriginal,

        @Schema(description = "MIME type detectado del archivo")
        String contentType,

        @Schema(description = "Tamaño en bytes")
        long tamanioBytes,

        @Schema(description = "Duración en segundos (solo para audio/video). Null si no aplica.")
        Integer duracionSegundos,

        @Schema(description = "Ancho en píxeles (solo para imagen/video). Null si no aplica.")
        Integer anchoPx,

        @Schema(description = "Alto en píxeles (solo para imagen/video). Null si no aplica.")
        Integer altoPx

) {}
