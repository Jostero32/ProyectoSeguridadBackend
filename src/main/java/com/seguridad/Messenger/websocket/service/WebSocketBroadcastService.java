package com.seguridad.Messenger.websocket.service;

import com.seguridad.Messenger.conversacion.repository.ParticipanteRepository;
import com.seguridad.Messenger.mensajes.dto.MensajeResponse;
import com.seguridad.Messenger.websocket.dto.WebSocketEvent;
import com.seguridad.Messenger.websocket.event.MensajeEnviadoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Envía eventos a los destinos STOMP.
 *
 * El listener {@code onMensajeEnviado} usa {@code @TransactionalEventListener(AFTER_COMMIT)}
 * para garantizar que el broadcast nunca ocurre si la transacción de DB hizo rollback.
 */
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipanteRepository participanteRepo;

    // ─── Listener de eventos de dominio ──────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMensajeEnviado(MensajeEnviadoEvent event) {
        broadcastMensaje(event.mensaje());
    }

    // ─── Broadcast a topic de conversación ───────────────────────────────────

    /**
     * Envía un mensaje nuevo al topic de la conversación.
     * Todos los clientes suscritos a {@code /topic/conversacion.{id}} lo reciben.
     */
    public void broadcastMensaje(MensajeResponse mensaje) {
        messagingTemplate.convertAndSend(
                "/topic/conversacion." + mensaje.conversacionId(),
                new WebSocketEvent<>("NUEVO_MENSAJE", mensaje)
        );
    }

    // ─── Cola personal por usuario ────────────────────────────────────────────

    /**
     * Envía un evento privado al usuario autenticado.
     * El destino resuelto es {@code /user/{usuarioId}/queue/notificaciones}.
     * Requiere que el cliente se haya conectado con un {@code Principal} válido.
     */
    public void enviarAUsuario(UUID usuarioId, WebSocketEvent<?> evento) {
        messagingTemplate.convertAndSendToUser(
                usuarioId.toString(),
                "/queue/notificaciones",
                evento
        );
    }
}
