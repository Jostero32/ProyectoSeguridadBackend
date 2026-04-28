package com.seguridad.Messenger.websocket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload del evento ESTADO_ENTREGA.
 * Se envía a la cola personal ({@code /user/queue/notificaciones}) del remitente del mensaje.
 * {@code entregadoEn} es null si el evento solo actualiza {@code leidoEn}, y viceversa.
 */
public record EstadoEntregaEventPayload(
        UUID mensajeId,
        UUID conversacionId,
        UUID usuarioId,
        LocalDateTime entregadoEn,
        LocalDateTime leidoEn
) {}
