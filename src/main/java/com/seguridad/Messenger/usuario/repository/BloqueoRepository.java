package com.seguridad.Messenger.usuario.repository;

import com.seguridad.Messenger.usuario.model.Bloqueo;
import com.seguridad.Messenger.usuario.model.BloqueoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BloqueoRepository extends JpaRepository<Bloqueo, BloqueoId> {

    List<Bloqueo> findByIdUsuarioId(UUID usuarioId);

    boolean existsByIdUsuarioIdAndIdBloqueadoId(UUID usuarioId, UUID bloqueadoId);
}
