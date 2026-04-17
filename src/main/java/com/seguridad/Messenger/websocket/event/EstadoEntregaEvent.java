package com.seguridad.Messenger.websocket.event;

import com.seguridad.Messenger.websocket.dto.EstadoEntregaEventPayload;

/**
 * Evento de dominio publicado por {@code MensajeService} cuando un mensaje cambia de estado
 * (entregado o leído). El listener en {@code WebSocketBroadcastService} lo envía a la
 * cola personal del remitente del mensaje.
 */
public record EstadoEntregaEvent(EstadoEntregaEventPayload payload) {}
