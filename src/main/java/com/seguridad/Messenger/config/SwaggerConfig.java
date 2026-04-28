package com.seguridad.Messenger.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String WEBSOCKET_DESCRIPTION = """
            ## WebSocket STOMP

            Endpoint: `ws://localhost:8080/ws` (raw) o `/ws-sockjs` (fallback SockJS).
            Header de conexión: `Authorization: Bearer {token}` (token de sesión).
            El token expira a los 30 días sin uso; la conexión se rechaza con frame ERROR.

            ### Suscripción única necesaria
            - `/user/queue/eventos` → TODOS los eventos del usuario autenticado.

            El cliente distingue cada evento por el campo `tipo` del envelope
            `WebSocketEvent { tipo, payload }`.

            ### Tipos de eventos recibidos
            - `NUEVO_MENSAJE`      → `MensajeResponse` completo (incluye `conversacionId`)
            - `NUEVA_REACCION`     → `{ mensajeId, conversacionId, usuarioId, emoji, resumenActualizado }`
            - `REACCION_ELIMINADA` → `{ mensajeId, conversacionId, usuarioId, emoji: null, resumenActualizado }`
            - `ESTADO_ENTREGA`     → `{ mensajeId, conversacionId, usuarioId, entregadoEn, leidoEn }` (solo al remitente)
            - `ESCRIBIENDO`        → `{ conversacionId, usuarioId, username }`
            - `DEJO_DE_ESCRIBIR`   → `{ conversacionId, usuarioId, username }`
            - `PRESENCIA`          → `{ usuarioId, username, conectado, ultimoVisto }`

            ### Envíos del cliente al servidor
            - `/app/escribiendo`       payload: `{ "conversacionId": "..." }`
            - `/app/dejo-de-escribir`  payload: `{ "conversacionId": "..." }`
            """;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Messenger API")
                        .version("1.0")
                        .description("API REST de mensajería — autenticación, usuarios, conversaciones y mensajes.\n\n"
                                + WEBSOCKET_DESCRIPTION))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("OpaqueToken")
                                .description("Token de sesión obtenido en POST /auth/login")));
    }
}
