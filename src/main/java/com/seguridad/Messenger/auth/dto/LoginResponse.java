package com.seguridad.Messenger.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Respuesta de inicio de sesión exitoso")
public record LoginResponse(
        @Schema(description = "Token de sesión para autenticación") String token,
        @Schema(description = "ID del usuario autenticado") UUID usuarioId
) {}
