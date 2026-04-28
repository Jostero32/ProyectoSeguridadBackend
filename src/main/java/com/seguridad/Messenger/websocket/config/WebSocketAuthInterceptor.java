package com.seguridad.Messenger.websocket.config;

import com.seguridad.Messenger.auth.model.DispositivoSesion;
import com.seguridad.Messenger.auth.repository.DispositivoSesionRepository;
import com.seguridad.Messenger.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Intercepta el frame STOMP CONNECT y valida el token Bearer.
 * Si el token es válido, establece el {@code Principal} de la sesión WebSocket
 * usando {@code UserPrincipal}, que Spring usa para enrutar /user/queue/... correctamente.
 * Si el token es inválido, está ausente o ha expirado (30 días sin uso),
 * lanza {@code MessagingException}, que Spring STOMP convierte en un frame ERROR
 * y cierra la conexión.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final DispositivoSesionRepository sesionRepo;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException("Token de autorización requerido");
            }

            String token = authHeader.substring(7);

            DispositivoSesion sesion = sesionRepo.findByTokenSesion(token)
                    .orElseThrow(() -> new MessagingException("Token inválido"));

            if (sesion.getUltimoAcceso().isBefore(LocalDateTime.now().minusDays(30))) {
                sesionRepo.delete(sesion);
                throw new MessagingException("Token expirado");
            }

            UserPrincipal principal = new UserPrincipal(sesion.getUsuario().getId());
            accessor.setUser(principal);
        }

        return message;
    }
}
