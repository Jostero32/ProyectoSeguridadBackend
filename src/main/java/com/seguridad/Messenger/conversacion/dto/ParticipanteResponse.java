package com.seguridad.Messenger.conversacion.dto;

import com.seguridad.Messenger.shared.enums.RolParticipante;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Información de un participante en una conversación")
public record ParticipanteResponse(

        @Schema(description = "ID del usuario")
        UUID usuarioId,

        @Schema(description = "Nombre de usuario único", example = "juanperez")
        String username,

        @Schema(description = "Nombre completo del usuario", example = "Juan Pérez")
        String nombreCompleto,

        @Schema(description = "URL del avatar del usuario")
        String urlAvatar,

        @Schema(description = "Rol del participante en la conversación", example = "MIEMBRO")
        RolParticipante rol,

        @Schema(description = "Fecha y hora en que se unió a la conversación")
        LocalDateTime fechaUnion

) {}
