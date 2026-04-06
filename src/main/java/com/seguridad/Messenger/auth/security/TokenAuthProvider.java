package com.seguridad.Messenger.auth.security;

import com.seguridad.Messenger.auth.model.DispositivoSesion;
import com.seguridad.Messenger.auth.repository.DispositivoSesionRepository;
import com.seguridad.Messenger.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;

@RequiredArgsConstructor
@Component
public class TokenAuthProvider implements AuthenticationProvider {

    private final DispositivoSesionRepository sesionRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();

        DispositivoSesion sesion = sesionRepository.findByTokenSesion(token)
                .orElseThrow(() -> new BadCredentialsException("Token inválido"));

        if (sesion.getUltimoAcceso().isBefore(LocalDateTime.now().minusDays(30))) {
            sesionRepository.delete(sesion);
            throw new BadCredentialsException("Sesión expirada");
        }

        sesionRepository.actualizarUltimoAcceso(token, LocalDateTime.now());

        UserPrincipal principal = new UserPrincipal(sesion.getUsuario().getId());
        return new UsernamePasswordAuthenticationToken(principal, token, Collections.emptyList());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
