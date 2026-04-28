package com.seguridad.Messenger.websocket.event;

import com.seguridad.Messenger.mensajes.dto.MensajeResponse;

/**
 * Evento de dominio publicado por {@code MensajeService} después de persistir un mensaje.
 * {@code WebSocketBroadcastService} lo escucha con {@code @TransactionalEventListener(AFTER_COMMIT)}
 * para garantizar que el broadcast solo ocurre si la transacción de DB se completó correctamente.
 */
public record MensajeEnviadoEvent(MensajeResponse mensaje) {}
