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

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Messenger API")
                        .version("1.0")
                        .description("""
                                API REST de mensajería — autenticación, usuarios, conversaciones y mensajes.

                                ## WebSocket STOMP
                                **Endpoint:** `ws://localhost:8080/ws` (con fallback SockJS en `/ws`)
                                **Autenticación:** header `Authorization: Bearer {token}` en el frame STOMP CONNECT

                                ### Suscripciones disponibles (v1 + v2)

                                **`/topic/conversacion.{conversacionId}`** — eventos de la conversación:
                                | Tipo | Descripción |
                                |------|-------------|
                                | `NUEVO_MENSAJE` | Nuevo mensaje enviado o reenviado |
                                | `NUEVA_REACCION` | Alguien reaccionó a un mensaje |
                                | `REACCION_ELIMINADA` | Alguien quitó su reacción |

                                **`/user/queue/notificaciones`** — eventos privados del usuario autenticado:
                                | Tipo | Descripción |
                                |------|-------------|
                                | `ESTADO_ENTREGA` | Tu mensaje fue entregado o leído por alguien |

                                ### Formato de todos los eventos
                                ```json
                                { "tipo": "NUEVO_MENSAJE|NUEVA_REACCION|REACCION_ELIMINADA|ESTADO_ENTREGA",
                                  "payload": { ...según tipo } }
                                ```
                                Próximos tipos: `ESCRIBIENDO`, `PRESENCIA`
                                """))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("OpaqueToken")
                                .description("Token de sesión obtenido en POST /auth/login")));
    }
}
