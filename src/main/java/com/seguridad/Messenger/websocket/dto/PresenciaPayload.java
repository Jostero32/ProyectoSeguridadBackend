package com.seguridad.Messenger.websocket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PresenciaPayload(UUID usuarioId, String username, boolean conectado, LocalDateTime ultimoVisto) {}
