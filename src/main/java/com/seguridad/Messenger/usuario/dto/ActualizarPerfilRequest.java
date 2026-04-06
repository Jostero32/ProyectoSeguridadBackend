package com.seguridad.Messenger.usuario.dto;

import com.seguridad.Messenger.shared.enums.PrivacidadUltimoVisto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud de actualización de perfil")
public record ActualizarPerfilRequest(
        @Schema(description = "URL del avatar") String urlAvatar,
        @Schema(description = "Biografía (máximo 160 caracteres)", maxLength = 160) @Size(max = 160) String bio,
        @Schema(description = "Privacidad del último visto") PrivacidadUltimoVisto privacidadUltimoVisto
) {}
