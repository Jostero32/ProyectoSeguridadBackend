package com.seguridad.Messenger.usuario.repository;

import com.seguridad.Messenger.usuario.model.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface PerfilUsuarioRepository extends JpaRepository<PerfilUsuario, UUID> {

    /**
     * Actualiza el campo ultimo_visto del perfil. Llamado de forma asíncrona
     * al desconectarse la última sesión WebSocket del usuario.
     * Nota: el PK de PerfilUsuario es el campo {@code id} (mapeado a columna usuario_id).
     */
    @Modifying
    @Transactional
    @Query("UPDATE PerfilUsuario p SET p.ultimoVisto = :ahora WHERE p.id = :usuarioId")
    void actualizarUltimoVisto(@Param("usuarioId") UUID usuarioId,
                               @Param("ahora") LocalDateTime ahora);

    @Query("SELECT p.urlAvatar FROM PerfilUsuario p WHERE p.id = :id")
    String findUrlAvatarById(@Param("id") UUID id);
}
