package com.seguridad.Messenger.usuario.service;

import com.seguridad.Messenger.shared.enums.PrivacidadUltimoVisto;
import com.seguridad.Messenger.shared.exception.RecursoNoEncontradoException;
import com.seguridad.Messenger.usuario.dto.ActualizarPerfilRequest;
import com.seguridad.Messenger.usuario.dto.PerfilResponse;
import com.seguridad.Messenger.usuario.dto.RegistroRequest;
import com.seguridad.Messenger.usuario.model.Persona;
import com.seguridad.Messenger.usuario.model.PerfilUsuario;
import com.seguridad.Messenger.usuario.model.Usuario;
import com.seguridad.Messenger.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario registrar(RegistroRequest request) {
        Persona persona = new Persona(request.nombres(), request.apellidos(), request.fechaNacimiento());

        Usuario usuario = new Usuario(
                persona,
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        PerfilUsuario perfil = new PerfilUsuario(usuario);
        usuario.setPerfil(perfil);

        return usuarioRepository.save(usuario);
    }

    public PerfilResponse obtenerPerfilPropio(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return construirPerfilResponse(usuario, true);
    }

    public PerfilResponse obtenerPerfilPublico(UUID id) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return construirPerfilResponse(usuario, false);
    }

    @Transactional
    public PerfilResponse actualizarPerfil(UUID usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        PerfilUsuario perfil = usuario.getPerfil();

        if (request.urlAvatar() != null) {
            perfil.setUrlAvatar(request.urlAvatar());
        }
        if (request.bio() != null) {
            perfil.setBio(request.bio());
        }
        if (request.privacidadUltimoVisto() != null) {
            perfil.setPrivacidadUltimoVisto(request.privacidadUltimoVisto());
        }

        usuarioRepository.save(usuario);
        return construirPerfilResponse(usuario, true);
    }

    public List<PerfilResponse> buscarPorUsername(String query) {
        return usuarioRepository.buscarPorUsername(query).stream()
                .map(u -> construirPerfilResponse(u, false))
                .toList();
    }

    private PerfilResponse construirPerfilResponse(Usuario usuario, boolean esPropio) {
        PerfilUsuario perfil = usuario.getPerfil();

        LocalDateTime ultimoVisto = null;
        if (esPropio || perfil.getPrivacidadUltimoVisto() == PrivacidadUltimoVisto.TODOS) {
            ultimoVisto = perfil.getUltimoVisto();
        }

        return new PerfilResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPersona().getNombres(),
                usuario.getPersona().getApellidos(),
                perfil.getUrlAvatar(),
                perfil.getBio(),
                ultimoVisto,
                esPropio ? perfil.getPrivacidadUltimoVisto() : null
        );
    }
}
