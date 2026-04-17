package com.seguridad.Messenger.websocket.session;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registra y libera sesiones WebSocket activas.
 * Un usuario puede tener múltiples sesiones simultáneas (varias pestañas/dispositivos).
 * Preparado para el módulo de presencia (próximo prompt).
 */
@Component
public class WebSocketSessionRegistry implements ApplicationListener<AbstractSubProtocolEvent> {

    // usuarioId (toString) → conjunto de sessionIds activos
    private final ConcurrentHashMap<String, Set<String>> sesionesActivas = new ConcurrentHashMap<>();

    @Override
    public void onApplicationEvent(AbstractSubProtocolEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (event instanceof SessionConnectedEvent) {
            String usuarioId = getPrincipalName(accessor);
            String sessionId = accessor.getSessionId();
            if (usuarioId != null && sessionId != null) {
                sesionesActivas
                        .computeIfAbsent(usuarioId, k -> ConcurrentHashMap.newKeySet())
                        .add(sessionId);
            }
        } else if (event instanceof SessionDisconnectEvent) {
            String usuarioId = getPrincipalName(accessor);
            String sessionId = accessor.getSessionId();
            if (usuarioId != null) {
                Set<String> sesiones = sesionesActivas.get(usuarioId);
                if (sesiones != null) {
                    sesiones.remove(sessionId);
                    if (sesiones.isEmpty()) sesionesActivas.remove(usuarioId);
                }
            }
        }
    }

    public boolean estaConectado(UUID usuarioId) {
        Set<String> sesiones = sesionesActivas.get(usuarioId.toString());
        return sesiones != null && !sesiones.isEmpty();
    }

    public Set<String> getUsuariosConectados() {
        return Collections.unmodifiableSet(sesionesActivas.keySet());
    }

    private String getPrincipalName(StompHeaderAccessor accessor) {
        return accessor.getUser() != null ? accessor.getUser().getName() : null;
    }
}
