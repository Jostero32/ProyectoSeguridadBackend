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

                                ### Suscripciones disponibles

                                **`/topic/conversacion.{conversacionId}`** — eventos de la conversación:
                                | Tipo | Descripción |
                                |------|-------------|
                                | `NUEVO_MENSAJE` | Nuevo mensaje enviado o reenviado |
                                | `NUEVA_REACCION` | Alguien reaccionó a un mensaje |
                                | `REACCION_ELIMINADA` | Alguien quitó su reacción |
                                | `ESCRIBIENDO` | Un participante empezó a escribir |
                                | `DEJO_DE_ESCRIBIR` | Un participante dejó de escribir (manual o timeout 4 s) |

                                **`/user/queue/notificaciones`** — eventos privados del usuario autenticado:
                                | Tipo | Descripción |
                                |------|-------------|
                                | `ESTADO_ENTREGA` | Tu mensaje fue entregado o leído por alguien |
                                | `PRESENCIA` | Un contacto se conectó o desconectó |

                                ### Enviar mensajes al servidor (`/app/...`)
                                | Destino | Payload | Descripción |
                                |---------|---------|-------------|
                                | `/app/escribiendo` | `{"conversacionId":"uuid"}` | Notificar que el usuario está escribiendo |
                                | `/app/dejo-de-escribir` | `{"conversacionId":"uuid"}` | Notificar que el usuario dejó de escribir |

                                ### Formato de todos los eventos
                                ```json
                                { "tipo": "NUEVO_MENSAJE|NUEVA_REACCION|REACCION_ELIMINADA|ESCRIBIENDO|DEJO_DE_ESCRIBIR|ESTADO_ENTREGA|PRESENCIA",
                                  "payload": { ...según tipo } }
                                ```

                                ### Payload de presencia
                                ```json
                                { "usuarioId": "uuid", "username": "string",
                                  "conectado": true|false, "ultimoVisto": "ISO-8601 o null" }
                                ```
                                La visibilidad de `ultimoVisto` respeta la configuración de privacidad del usuario (`TODOS`, `CONTACTOS`, `NADIE`).
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
