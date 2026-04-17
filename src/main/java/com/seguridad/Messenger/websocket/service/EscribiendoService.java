package com.seguridad.Messenger.websocket.service;

import com.seguridad.Messenger.websocket.dto.EscribiendoPayload;
import com.seguridad.Messenger.websocket.dto.WebSocketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Gestiona el indicador de escritura por conversación y usuario.
 *
 * Cada llamada a {@link #usuarioEscribiendo} reinicia un timer de 4 segundos.
 * Si el timer expira sin que el usuario llame a {@link #usuarioDejoDeEscribir},
 * se emite automáticamente {@code DEJO_DE_ESCRIBIR}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscribiendoService {

    private final SimpMessagingTemplate messagingTemplate;

    // Timer daemon para expiración automática de timeouts de escritura
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "escribiendo-scheduler");
        t.setDaemon(true);
        return t;
    });

    // "conversacionId:usuarioId" → ScheduledFuture del DEJO_DE_ESCRIBIR automático
    private final ConcurrentHashMap<String, ScheduledFuture<?>> timeouts = new ConcurrentHashMap<>();

    /**
     * El usuario empezó a escribir. Cancela el timeout anterior (si existe),
     * emite {@code ESCRIBIENDO} y programa {@code DEJO_DE_ESCRIBIR} en 4 segundos.
     */
    public void usuarioEscribiendo(UUID conversacionId, UUID usuarioId, String username) {
        String key = conversacionId + ":" + usuarioId;

        ScheduledFuture<?> prev = timeouts.remove(key);
        if (prev != null) prev.cancel(false);

        emitir("ESCRIBIENDO", conversacionId, usuarioId, username);

        ScheduledFuture<?> futuro = scheduler.schedule(() -> {
            timeouts.remove(key);
            emitir("DEJO_DE_ESCRIBIR", conversacionId, usuarioId, username);
        }, 4, TimeUnit.SECONDS);

        timeouts.put(key, futuro);
    }

    /**
     * El usuario dejó de escribir explícitamente. Cancela el timeout y emite
     * {@code DEJO_DE_ESCRIBIR} de inmediato.
     */
    public void usuarioDejoDeEscribir(UUID conversacionId, UUID usuarioId, String username) {
        String key = conversacionId + ":" + usuarioId;
        ScheduledFuture<?> prev = timeouts.remove(key);
        if (prev != null) prev.cancel(false);
        emitir("DEJO_DE_ESCRIBIR", conversacionId, usuarioId, username);
    }

    /**
     * Cancela todos los timeouts activos de un usuario. Llamado en desconexión
     * para evitar que emita {@code DEJO_DE_ESCRIBIR} en conversaciones abiertas
     * después de que el usuario se haya ido.
     */
    public void limpiarTodosLosTimeouts(UUID usuarioId) {
        String sufijo = ":" + usuarioId;
        timeouts.entrySet().removeIf(entry -> {
            if (entry.getKey().endsWith(sufijo)) {
                entry.getValue().cancel(false);
                return true;
            }
            return false;
        });
    }

    private void emitir(String tipo, UUID conversacionId, UUID usuarioId, String username) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/conversacion." + conversacionId,
                    new WebSocketEvent<>(tipo, new EscribiendoPayload(conversacionId, usuarioId, username))
            );
        } catch (Exception e) {
            log.warn("Error broadcasting {} usuario={} conversacion={}: {}",
                    tipo, usuarioId, conversacionId, e.getMessage());
        }
    }
}
