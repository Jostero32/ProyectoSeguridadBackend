package com.seguridad.Messenger.conversacion.dto;

import com.seguridad.Messenger.shared.enums.TipoConversacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Resumen de un chat para la pantalla principal: incluye último mensaje y contador de no leídos")
public record ChatResumenResponse(

        @Schema(description = "ID del chat")
        UUID id,

        @Schema(description = "Tipo de chat")
        TipoConversacion tipo,

        @Schema(description = "Nombre del grupo o nombre completo del otro usuario en chats individuales")
        String titulo,

        @Schema(description = "Avatar del grupo o del otro usuario en chats individuales")
        String urlAvatar,

        @Schema(description = "Fecha de creación del chat")
        LocalDateTime creadaEn,

        @Schema(description = "true si el usuario autenticado es administrador (solo relevante en grupos)")
        boolean esAdmin,

        @Schema(description = "Último mensaje del chat. Null si no hay mensajes aún.")
        UltimoMensajeResponse ultimoMensaje,

        @Schema(description = "Cantidad de mensajes no leídos para el usuario autenticado")
        int noLeidos

) {}
