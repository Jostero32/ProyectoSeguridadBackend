package com.seguridad.Messenger.shared.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class TokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generarToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
