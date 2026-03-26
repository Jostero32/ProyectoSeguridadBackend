package com.seguridad.Messenger.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Respuesta de error estándar")
public record ErrorResponse(
        @Schema(description = "Código de error") String codigo,
        @Schema(description = "Mensaje descriptivo del error") String mensaje,
        @Schema(description = "Marca de tiempo del error") LocalDateTime timestamp
) {}
