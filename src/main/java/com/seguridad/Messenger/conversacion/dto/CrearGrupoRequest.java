package com.seguridad.Messenger.conversacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Solicitud para crear un grupo")
public record CrearGrupoRequest(

        @NotBlank
        @Schema(description = "Nombre del grupo", requiredMode = Schema.RequiredMode.REQUIRED)
        String titulo,

        @Schema(description = "URL del avatar del grupo")
        String urlAvatar,

        @NotEmpty
        @Size(min = 1)
        @Schema(description = "IDs de los miembros a agregar (además del creador, que se agrega automáticamente como admin)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> miembrosIds

) {}
