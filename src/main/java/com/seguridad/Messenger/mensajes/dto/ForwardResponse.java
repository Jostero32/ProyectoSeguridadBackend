package com.seguridad.Messenger.mensajes.dto;

import com.seguridad.Messenger.shared.enums.TipoMensaje;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Datos mínimos del mensaje original cuando este mensaje es un reenvío")
public record ForwardResponse(

        @Schema(description = "ID del mensaje raíz")
        UUID mensajeId,

        @Schema(description = "ID del autor original")
        UUID remitenteId,

        @Schema(description = "Tipo del mensaje original")
        TipoMensaje tipo,

        @Schema(description = "Contenido textual original. Null si fue eliminado para todos o no es TEXTO.")
        String contenido,

        @Schema(description = "Ruta /archivos/{key} para acceder al archivo original. Null si no aplica.")
        String urlAcceso,

        @Schema(description = "Thumbnail base64 del archivo original. Null si no es IMAGEN o se eliminó.")
        String thumbnailBase64,

        @Schema(description = "true si el mensaje original fue eliminado para todos")
        boolean eliminado

) {}
