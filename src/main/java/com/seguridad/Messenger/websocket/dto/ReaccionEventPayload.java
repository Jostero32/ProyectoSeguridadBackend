package com.seguridad.Messenger.websocket.dto;

import com.seguridad.Messenger.mensajes.dto.ResumenReaccionesResponse;

import java.util.List;
import java.util.UUID;

/**
 * Payload del evento NUEVA_REACCION / REACCION_ELIMINADA.
 * {@code emoji} es null cuando el tipo de evento es REACCION_ELIMINADA.
 * {@code resumenActualizado} refleja el estado completo tras el cambio.
 */
public record ReaccionEventPayload(
        UUID mensajeId,
        UUID conversacionId,
        UUID usuarioId,
        String emoji,
        List<ResumenReaccionesResponse> resumenActualizado
) {}
