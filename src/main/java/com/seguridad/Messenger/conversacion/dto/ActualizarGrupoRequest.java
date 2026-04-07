package com.seguridad.Messenger.conversacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Solicitud para actualizar nombre o avatar de un grupo (todos los campos son opcionales)")
public record ActualizarGrupoRequest(

        @Size(min = 1)
        @Schema(description = "Nuevo nombre del grupo (null para no cambiar)")
        String titulo,

        @Schema(description = "Nueva URL del avatar del grupo (null para no cambiar)")
        String urlAvatar

) {}
