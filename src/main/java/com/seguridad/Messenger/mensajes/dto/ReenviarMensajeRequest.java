package com.seguridad.Messenger.mensajes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Solicitud para reenviar un mensaje a una o varias conversaciones")
public record ReenviarMensajeRequest(

        @NotEmpty
        @Size(max = 10)
        @Schema(description = "IDs de las conversaciones destino (máximo 10 por reenvío)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> conversacionIds

) {}
