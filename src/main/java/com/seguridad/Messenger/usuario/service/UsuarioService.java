package com.seguridad.Messenger.usuario.service;

import com.seguridad.Messenger.shared.enums.PrivacidadUltimoVisto;
import com.seguridad.Messenger.shared.exception.RecursoNoEncontradoException;
import com.seguridad.Messenger.shared.service.StorageService;
import com.seguridad.Messenger.usuario.dto.PerfilResponse;
import com.seguridad.Messenger.usuario.dto.RegistroRequest;
import com.seguridad.Messenger.usuario.model.Persona;
import com.seguridad.Messenger.usuario.model.PerfilUsuario;
import com.seguridad.Messenger.usuario.model.Usuario;
import com.seguridad.Messenger.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

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
    public PerfilResponse actualizarPerfil(UUID usuarioId, MultipartFile avatar,
                                           String bio, String privacidadUltimoVistoStr) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        PerfilUsuario perfil = usuario.getPerfil();

        String avatarKeyViejo = null;
        if (avatar != null && !avatar.isEmpty()) {
            avatarKeyViejo = perfil.getAvatarKey();
            perfil.setAvatarKey(storageService.subirAvatar(avatar));
        }

        if (bio != null) {
            perfil.setBio(bio.isBlank() ? null : bio);
        }

        if (privacidadUltimoVistoStr != null && !privacidadUltimoVistoStr.isBlank()) {
            try {
                perfil.setPrivacidadUltimoVisto(
                        PrivacidadUltimoVisto.valueOf(privacidadUltimoVistoStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Valor inválido para privacidadUltimoVisto: " + privacidadUltimoVistoStr);
            }
        }

        usuarioRepository.save(usuario);

        if (avatarKeyViejo != null) {
            storageService.eliminar(avatarKeyViejo);
        }

        return construirPerfilResponse(usuario, true);
    }

    public Page<PerfilResponse> buscar(String query, int page, int size) {
        return usuarioRepository.buscar(query, PageRequest.of(page, size))
                .map(u -> construirPerfilResponse(u, false));
    }

    private PerfilResponse construirPerfilResponse(Usuario usuario, boolean esPropio) {
        PerfilUsuario perfil = usuario.getPerfil();

        LocalDateTime ultimoVisto = null;
        if (esPropio || perfil.getPrivacidadUltimoVisto() == PrivacidadUltimoVisto.TODOS) {
            ultimoVisto = perfil.getUltimoVisto();
        }

        String urlAvatar = perfil.getAvatarKey() != null ? "/archivos/" + perfil.getAvatarKey() : null;

        return new PerfilResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPersona().getNombres(),
                usuario.getPersona().getApellidos(),
                urlAvatar,
                perfil.getBio(),
                ultimoVisto,
                esPropio ? perfil.getPrivacidadUltimoVisto() : null
        );
    }
}
