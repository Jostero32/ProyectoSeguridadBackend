package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para editar el contenido de un mensaje")
public record EditarMensajeRequest(

        @NotBlank
        @Size(max = 4000)
        @Schema(description = "Nuevo contenido del mensaje (máximo 4000 caracteres)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String contenido

) {}
