package com.seguridad.Messenger.usuario.service;

import com.seguridad.Messenger.shared.exception.RecursoNoEncontradoException;
import com.seguridad.Messenger.usuario.dto.BloqueoResponse;
import com.seguridad.Messenger.usuario.model.Bloqueo;
import com.seguridad.Messenger.usuario.model.BloqueoId;
import com.seguridad.Messenger.usuario.model.Usuario;
import com.seguridad.Messenger.usuario.repository.BloqueoRepository;
import com.seguridad.Messenger.usuario.repository.PerfilUsuarioRepository;
import com.seguridad.Messenger.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BloqueoService {

    private final BloqueoRepository bloqueoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;

    public List<BloqueoResponse> listar(UUID usuarioId) {
        return bloqueoRepository.findByIdUsuarioId(usuarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void bloquear(UUID usuarioId, UUID bloqueadoId) {
        if (usuarioId.equals(bloqueadoId)) {
            throw new IllegalArgumentException("No puedes bloquearte a ti mismo");
        }
        if (!usuarioRepository.existsById(bloqueadoId)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado");
        }
        if (bloqueoRepository.existsById(new BloqueoId(usuarioId, bloqueadoId))) {
            return;
        }
        Bloqueo bloqueo = new Bloqueo();
        bloqueo.setId(new BloqueoId(usuarioId, bloqueadoId));
        bloqueo.setFechaBloqueo(LocalDateTime.now());
        bloqueoRepository.save(bloqueo);
    }

    @Transactional
    public void desbloquear(UUID usuarioId, UUID bloqueadoId) {
        BloqueoId id = new BloqueoId(usuarioId, bloqueadoId);
        if (!bloqueoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Bloqueo no encontrado");
        }
        bloqueoRepository.deleteById(id);
    }

    public boolean esBloqueado(UUID usuarioId, UUID bloqueadoId) {
        return bloqueoRepository.existsById(new BloqueoId(usuarioId, bloqueadoId));
    }

    private BloqueoResponse toResponse(Bloqueo bloqueo) {
        UUID bloqueadoId = bloqueo.getId().getBloqueadoId();
        Usuario usuario = usuarioRepository.findById(bloqueadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        String avatarKey = perfilUsuarioRepository.findUrlAvatarById(bloqueadoId);
        String urlAvatar = avatarKey != null ? "/archivos/" + avatarKey : null;
        return new BloqueoResponse(
                bloqueadoId,
                usuario.getUsername(),
                usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos(),
                urlAvatar,
                bloqueo.getFechaBloqueo()
        );
    }
}
