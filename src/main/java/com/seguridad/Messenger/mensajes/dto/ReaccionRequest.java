package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para reaccionar a un mensaje")
public record ReaccionRequest(

        @NotBlank
        @Size(max = 8)
        @Schema(description = "Emoji de la reacción (máximo 8 caracteres para cubrir emojis compuestos)",
                example = "👍")
        String emoji

) {}
