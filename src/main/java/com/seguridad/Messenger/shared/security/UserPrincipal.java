package com.seguridad.Messenger.shared.security;

import java.security.Principal;
import java.util.UUID;

public record UserPrincipal(UUID usuarioId) implements Principal {

    @Override
    public String getName() {
        // Spring STOMP usa getName() para enrutar /user/queue/... a cada usuario
        return usuarioId.toString();
    }
}
