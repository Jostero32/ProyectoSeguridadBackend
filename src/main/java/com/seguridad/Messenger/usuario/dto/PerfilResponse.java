package com.seguridad.Messenger.usuario.dto;

import com.seguridad.Messenger.shared.enums.PrivacidadUltimoVisto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Perfil de usuario")
public record PerfilResponse(
        @Schema(description = "ID del usuario") UUID id,
        @Schema(description = "Nombre de usuario") String username,
        @Schema(description = "Nombres") String nombres,
        @Schema(description = "Apellidos") String apellidos,
        @Schema(description = "URL del avatar") String urlAvatar,
        @Schema(description = "Biografía") String bio,
        @Schema(description = "Última conexión (null si la privacidad lo restringe)") LocalDateTime ultimoVisto,
        @Schema(description = "Configuración de privacidad del último visto") PrivacidadUltimoVisto privacidadUltimoVisto
) {}
