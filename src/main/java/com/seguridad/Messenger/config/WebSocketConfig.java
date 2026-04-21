package com.seguridad.Messenger.config;

import com.seguridad.Messenger.websocket.config.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker en memoria — suficiente para un solo nodo
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefijo para @MessageMapping (el cliente envía por REST en v1)
        registry.setApplicationDestinationPrefixes("/app");

        // Prefijo para destinos personales /user/queue/...
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint raw WebSocket — para clientes STOMP directos (Postman, apps nativas, wscat)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // Endpoint SockJS — fallback para navegadores sin WebSocket nativo
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Valida el token Bearer en el frame STOMP CONNECT
        registration.interceptors(authInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(65536)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(20_000);
    }
}
