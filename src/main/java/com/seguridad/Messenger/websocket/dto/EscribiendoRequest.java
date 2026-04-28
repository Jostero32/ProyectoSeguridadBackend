package com.seguridad.Messenger.websocket.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EscribiendoRequest(@NotNull UUID conversacionId) {}
