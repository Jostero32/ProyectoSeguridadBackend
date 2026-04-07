package com.seguridad.Messenger.conversacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Solicitud para crear o recuperar un chat individual")
public record CrearChatIndividualRequest(

        @NotNull
        @Schema(description = "ID del usuario destinatario", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID destinatarioId

) {}
