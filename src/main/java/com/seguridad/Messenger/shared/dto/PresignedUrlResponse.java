package com.seguridad.Messenger.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "URL prefirmada para descargar un archivo desde el almacenamiento.")
public record PresignedUrlResponse(

        @Schema(description = "URL completa con la firma en query params. " +
                "El cliente debe hacer GET sin headers de autenticación.")
        String url,

        @Schema(description = "Segundos hasta que la URL expire. Útil para cachearla y renovarla antes.")
        long expiresInSeconds

) {}
