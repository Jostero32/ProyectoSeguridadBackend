package com.seguridad.Messenger.conversacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

@Schema(description = "Solicitud para agregar miembros a un grupo")
public record AgregarMiembrosRequest(

        @NotEmpty
        @Schema(description = "IDs de los usuarios a agregar", requiredMode = Schema.RequiredMode.REQUIRED)
        List<UUID> usuarioIds

) {}
