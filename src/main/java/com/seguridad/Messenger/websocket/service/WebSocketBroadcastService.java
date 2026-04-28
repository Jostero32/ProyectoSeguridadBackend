package com.seguridad.Messenger.websocket.service;

import com.seguridad.Messenger.conversacion.repository.ParticipanteRepository;
import com.seguridad.Messenger.mensajes.repository.MensajeRepository;
import com.seguridad.Messenger.websocket.dto.WebSocketEvent;
import com.seguridad.Messenger.websocket.event.EstadoEntregaEvent;
import com.seguridad.Messenger.websocket.event.MensajeEnviadoEvent;
import com.seguridad.Messenger.websocket.event.ReaccionEvent;
import com.seguridad.Messenger.websocket.session.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * Envía eventos a los destinos STOMP tras el commit de la transacción.
 *
 * Todos los listeners son best-effort: si falla el broadcast, se loguea como warning
 * y no se propaga la excepción — el cliente puede recuperar el estado via REST.
 *
 * Diseño: canal único por usuario en {@code /user/{id}/queue/eventos}. El broker no
 * valida suscripciones a topics, así que cualquier cliente con un UUID podía
 * suscribirse a {@code /topic/conversacion.{id}}. Se eliminó el topic por
 * conversación y todo se envía individualmente a cada participante conectado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastService {

    private static final String DESTINO_USUARIO = "/queue/eventos";

    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipanteRepository participanteRepo;
    private final MensajeRepository mensajeRepository;
    private final WebSocketSessionRegistry sessionRegistry;

    // ─── Envío directo a un usuario ──────────────────────────────────────────

    /**
     * Envía un evento al canal personal del usuario:
     * {@code /user/{usuarioId}/queue/eventos}.
     */
    public void enviarAUsuario(UUID usuarioId, WebSocketEvent<?> evento) {
        messagingTemplate.convertAndSendToUser(
                usuarioId.toString(),
                DESTINO_USUARIO,
                evento
        );
    }

    // ─── Broadcast a participantes de una conversación ───────────────────────

    /**
     * Envía el evento a cada participante conectado de la conversación a su
     * cola personal. Reemplaza el broadcast a {@code /topic/conversacion.{id}}.
     * Los usuarios desconectados se omiten — no acumulamos eventos en el broker.
     */
    public void broadcastAConversacion(UUID conversacionId, WebSocketEvent<?> evento) {
        List<UUID> participantes = participanteRepo.findUsuarioIdsByConversacionId(conversacionId);
        for (UUID uid : participantes) {
            if (sessionRegistry.estaConectado(uid)) {
                enviarAUsuario(uid, evento);
            }
        }
    }

    // ─── Listener: mensaje nuevo ──────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMensajeEnviado(MensajeEnviadoEvent event) {
        try {
            broadcastAConversacion(
                    event.mensaje().conversacionId(),
                    new WebSocketEvent<>("NUEVO_MENSAJE", event.mensaje())
            );
        } catch (Exception e) {
            log.warn("Error en broadcast NUEVO_MENSAJE conversacionId={}: {}",
                    event.mensaje().conversacionId(), e.getMessage());
        }
    }

    // ─── Listener: reacción añadida o eliminada ───────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReaccion(ReaccionEvent event) {
        try {
            broadcastAConversacion(
                    event.payload().conversacionId(),
                    new WebSocketEvent<>(event.tipo(), event.payload())
            );
        } catch (Exception e) {
            log.warn("Error en broadcast reacción mensajeId={}: {}",
                    event.payload().mensajeId(), e.getMessage());
        }
    }

    // ─── Listener: estado de entrega/lectura ──────────────────────────────────

    /**
     * Notifica al remitente del mensaje que su mensaje fue entregado o leído.
     * Solo se envía si el remitente está conectado — evita acumular mensajes en el broker.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEstadoEntrega(EstadoEntregaEvent event) {
        try {
            UUID remitenteId = mensajeRepository.findRemitenteId(event.payload().mensajeId());
            if (remitenteId != null && sessionRegistry.estaConectado(remitenteId)) {
                enviarAUsuario(remitenteId, new WebSocketEvent<>("ESTADO_ENTREGA", event.payload()));
            }
        } catch (Exception e) {
            log.warn("Error en broadcast ESTADO_ENTREGA mensajeId={}: {}",
                    event.payload().mensajeId(), e.getMessage());
        }
    }
}
