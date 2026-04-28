package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Solicitud para enviar un mensaje de texto")
public record EnviarMensajeRequest(

        @NotBlank
        @Size(max = 4000)
        @Schema(description = "Contenido del mensaje (máximo 4000 caracteres)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String contenido,

        @Schema(description = "ID del mensaje al que se responde. Null si no es una respuesta.")
        UUID respuestaMensajeId

) {}
