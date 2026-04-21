package com.seguridad.Messenger.usuario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Usuario bloqueado")
public record BloqueoResponse(

        @Schema(description = "ID del usuario bloqueado")
        UUID usuarioId,

        @Schema(description = "Username del usuario bloqueado")
        String username,

        @Schema(description = "Nombre completo del usuario bloqueado")
        String nombreCompleto,

        @Schema(description = "URL del avatar del usuario bloqueado")
        String urlAvatar,

        @Schema(description = "Fecha en que fue bloqueado")
        LocalDateTime fechaBloqueo

) {}
