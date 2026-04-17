package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Solicitud para marcar como leídos uno o varios mensajes en lote")
public record MarcarLeidoRequest(

        @NotEmpty
        @Schema(description = "Lista de IDs de mensajes a marcar como leídos",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> mensajeIds

) {}
