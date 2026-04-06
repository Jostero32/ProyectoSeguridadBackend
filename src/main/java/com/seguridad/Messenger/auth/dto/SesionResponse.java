package com.seguridad.Messenger.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Información de sesión/dispositivo activo")
public record SesionResponse(
        @Schema(description = "ID de la sesión") UUID id,
        @Schema(description = "Información del dispositivo (User-Agent)") String infoDispositivo,
        @Schema(description = "Último acceso registrado") LocalDateTime ultimoAcceso,
        @Schema(description = "Plataforma de notificaciones push") String plataformaPush
) {}
