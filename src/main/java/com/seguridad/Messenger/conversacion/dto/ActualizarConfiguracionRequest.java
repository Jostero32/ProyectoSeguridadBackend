package com.seguridad.Messenger.conversacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Solicitud para actualizar la configuración personal de un chat")
public record ActualizarConfiguracionRequest(

        @Schema(description = "Silenciar notificaciones hasta esta fecha/hora. Enviar null para desactivar el silencio.")
        LocalDateTime silenciadoHasta,

        @Schema(description = "Archivar o desarchivar la conversación. Null para no modificar el estado actual.")
        Boolean archivado,

        @Schema(description = "Fijar o desfijar la conversación al inicio de la lista. Null para no modificar el estado actual.")
        Boolean fijado

) {}
