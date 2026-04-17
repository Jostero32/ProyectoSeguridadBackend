package com.seguridad.Messenger.websocket.event;

import com.seguridad.Messenger.websocket.dto.ReaccionEventPayload;

/**
 * Evento de dominio publicado por {@code MensajeService} cuando una reacción es añadida o eliminada.
 * {@code tipo} es "NUEVA_REACCION" o "REACCION_ELIMINADA".
 */
public record ReaccionEvent(
        String tipo,
        ReaccionEventPayload payload
) {}
