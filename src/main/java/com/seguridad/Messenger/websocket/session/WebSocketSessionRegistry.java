package com.seguridad.Messenger.websocket.session;

import com.seguridad.Messenger.conversacion.repository.ParticipanteRepository;
import com.seguridad.Messenger.shared.enums.PrivacidadUltimoVisto;
import com.seguridad.Messenger.usuario.model.PerfilUsuario;
import com.seguridad.Messenger.usuario.repository.PerfilUsuarioRepository;
import com.seguridad.Messenger.usuario.repository.UsuarioRepository;
import com.seguridad.Messenger.websocket.dto.PresenciaPayload;
import com.seguridad.Messenger.websocket.dto.WebSocketEvent;
import com.seguridad.Messenger.websocket.service.EscribiendoService;
import com.seguridad.Messenger.websocket.service.WebSocketBroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registra sesiones WebSocket activas y emite eventos de presencia.
 *
 * Un usuario puede tener múltiples sesiones simultáneas (pestañas/dispositivos).
 * La presencia se emite solo en la primera conexión y en la última desconexión.
 *
 * Ciclo de dependencia resuelto con {@code @Lazy}:
 *   WebSocketSessionRegistry → WebSocketBroadcastService → WebSocketSessionRegistry
 */
@Slf4j
@Component
public class WebSocketSessionRegistry implements ApplicationListener<AbstractSubProtocolEvent> {

    private final WebSocketBroadcastService broadcastService;
    private final EscribiendoService escribiendoService;
    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;

    // usuarioId (toString) → conjunto de sessionIds activos
    private final ConcurrentHashMap<String, Set<String>> sesionesActivas = new ConcurrentHashMap<>();

    public WebSocketSessionRegistry(
            @Lazy WebSocketBroadcastService broadcastService,
            @Lazy EscribiendoService escribiendoService,
            PerfilUsuarioRepository perfilUsuarioRepository,
            ParticipanteRepository participanteRepository,
            UsuarioRepository usuarioRepository) {
        this.broadcastService = broadcastService;
        this.escribiendoService = escribiendoService;
        this.perfilUsuarioRepository = perfilUsuarioRepository;
        this.participanteRepository = participanteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void onApplicationEvent(AbstractSubProtocolEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        if (event instanceof SessionConnectedEvent) {
            String usuarioIdStr = getPrincipalName(accessor);
            String sessionId = accessor.getSessionId();
            if (usuarioIdStr == null || sessionId == null) return;

            Set<String> sesiones = sesionesActivas
                    .computeIfAbsent(usuarioIdStr, k -> ConcurrentHashMap.newKeySet());
            boolean primeraConexion = sesiones.isEmpty();
            sesiones.add(sessionId);

            if (primeraConexion) {
                emitirPresencia(UUID.fromString(usuarioIdStr), true, null);
            }

        } else if (event instanceof SessionDisconnectEvent) {
            String usuarioIdStr = getPrincipalName(accessor);
            String sessionId = accessor.getSessionId();
            if (usuarioIdStr == null) return;

            Set<String> sesiones = sesionesActivas.get(usuarioIdStr);
            if (sesiones != null) {
                sesiones.remove(sessionId);
                if (sesiones.isEmpty()) {
                    sesionesActivas.remove(usuarioIdStr);
                    UUID usuarioId = UUID.fromString(usuarioIdStr);

                    escribiendoService.limpiarTodosLosTimeouts(usuarioId);

                    LocalDateTime ahora = LocalDateTime.now();
                    CompletableFuture.runAsync(() -> {
                        try {
                            perfilUsuarioRepository.actualizarUltimoVisto(usuarioId, ahora);
                        } catch (Exception e) {
                            log.warn("Error actualizando ultimoVisto usuario={}: {}",
                                    usuarioId, e.getMessage());
                        }
                    });

                    emitirPresencia(usuarioId, false, ahora);
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

    // ─── Presencia ────────────────────────────────────────────────────────────

    private void emitirPresencia(UUID usuarioId, boolean conectado, LocalDateTime ultimoVisto) {
        try {
            String username = usuarioRepository.findUsernameById(usuarioId);
            if (username == null) return;

            PrivacidadUltimoVisto privacidad = perfilUsuarioRepository.findById(usuarioId)
                    .map(PerfilUsuario::getPrivacidadUltimoVisto)
                    .orElse(PrivacidadUltimoVisto.TODOS);

            PresenciaPayload payload = new PresenciaPayload(usuarioId, username, conectado, ultimoVisto);
            WebSocketEvent<PresenciaPayload> evento = new WebSocketEvent<>("PRESENCIA", payload);

            switch (privacidad) {
                case TODOS ->
                    getUsuariosConectados().stream()
                            .filter(uid -> !uid.equals(usuarioId.toString()))
                            .forEach(uid -> broadcastService.enviarAUsuario(UUID.fromString(uid), evento));

                case CONTACTOS -> {
                    List<UUID> relacionados = participanteRepository
                            .findUsuariosConConversacionIndividual(usuarioId);
                    relacionados.stream()
                            .filter(this::estaConectado)
                            .forEach(uid -> broadcastService.enviarAUsuario(uid, evento));
                }

                case NADIE -> { /* El usuario no quiere que nadie sepa su presencia */ }
            }
        } catch (Exception e) {
            log.warn("Error emitiendo presencia usuario={}: {}", usuarioId, e.getMessage());
        }
    }

    private String getPrincipalName(StompHeaderAccessor accessor) {
        return accessor.getUser() != null ? accessor.getUser().getName() : null;
    }
}
