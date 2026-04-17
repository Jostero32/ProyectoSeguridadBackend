package com.seguridad.Messenger.websocket.dto;

/**
 * Envelope genérico para todos los eventos WebSocket.
 * El cliente distingue el tipo de evento por el campo {@code tipo}
 * y deserializa {@code payload} según corresponda.
 *
 * Tipos definidos a lo largo de los módulos WebSocket:
 *   NUEVO_MENSAJE   — v1: mensaje nuevo en una conversación
 *   NUEVA_REACCION  — v2: reacción añadida/reemplazada
 *   ESTADO_ENTREGA  — v2: mensaje entregado o leído
 *   ESCRIBIENDO     — v3: indicador de escritura
 *   PRESENCIA       — v3: cambio de estado de presencia
 */
public record WebSocketEvent<T>(
        String tipo,
        T payload
) {}
