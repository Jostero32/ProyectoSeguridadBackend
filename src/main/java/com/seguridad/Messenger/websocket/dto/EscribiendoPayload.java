package com.seguridad.Messenger.websocket.dto;

import java.util.UUID;

public record EscribiendoPayload(UUID conversacionId, UUID usuarioId, String username) {}
