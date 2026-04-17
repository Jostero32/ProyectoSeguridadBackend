package com.seguridad.Messenger.websocket.service;

import com.seguridad.Messenger.conversacion.repository.ParticipanteRepository;
import com.seguridad.Messenger.mensajes.dto.MensajeResponse;
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

import java.util.UUID;

/**
 * Envía eventos a los destinos STOMP tras el commit de la transacción.
 *
 * Todos los listeners son best-effort: si falla el broadcast, se loguea como warning
 * y no se propaga la excepción — el cliente puede recuperar el estado via REST.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipanteRepository participanteRepo;
    private final MensajeRepository mensajeRepository;
    private final WebSocketSessionRegistry sessionRegistry;

    // ─── Listener: mensaje nuevo ──────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMensajeEnviado(MensajeEnviadoEvent event) {
        try {
            broadcastMensaje(event.mensaje());
        } catch (Exception e) {
            log.warn("Error en broadcast de mensaje nuevo conversacionId={}: {}",
                    event.mensaje().conversacionId(), e.getMessage());
        }
    }

    // ─── Listener: reacción añadida o eliminada ───────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReaccion(ReaccionEvent event) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/conversacion." + event.payload().conversacionId(),
                    new WebSocketEvent<>(event.tipo(), event.payload())
            );
        } catch (Exception e) {
            log.warn("Error en broadcast de reacción mensajeId={}: {}",
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
            log.warn("Error en broadcast de estado de entrega mensajeId={}: {}",
                    event.payload().mensajeId(), e.getMessage());
        }
    }

    // ─── Broadcast a topic de conversación ───────────────────────────────────

    public void broadcastMensaje(MensajeResponse mensaje) {
        messagingTemplate.convertAndSend(
                "/topic/conversacion." + mensaje.conversacionId(),
                new WebSocketEvent<>("NUEVO_MENSAJE", mensaje)
        );
    }

    // ─── Cola personal por usuario ────────────────────────────────────────────

    /**
     * Envía un evento privado al usuario. El destino resuelto es
     * {@code /user/{usuarioId}/queue/notificaciones}.
     */
    public void enviarAUsuario(UUID usuarioId, WebSocketEvent<?> evento) {
        messagingTemplate.convertAndSendToUser(
                usuarioId.toString(),
                "/queue/notificaciones",
                evento
        );
    }
}
