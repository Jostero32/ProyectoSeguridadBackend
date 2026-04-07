package com.seguridad.Messenger.conversacion.repository;

import com.seguridad.Messenger.conversacion.model.Participante;
import com.seguridad.Messenger.conversacion.model.ParticipanteId;
import com.seguridad.Messenger.shared.enums.RolParticipante;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipanteRepository extends JpaRepository<Participante, ParticipanteId> {

    Optional<Participante> findByIdConversacionIdAndIdUsuarioId(UUID conversacionId, UUID usuarioId);

    boolean existsByIdConversacionIdAndIdUsuarioId(UUID conversacionId, UUID usuarioId);

    long countByIdConversacionId(UUID conversacionId);

    long countByIdConversacionIdAndRol(UUID conversacionId, RolParticipante rol);

    /**
     * Lista todos los participantes de una conversación con sus datos de usuario cargados.
     */
    @Query("""
            SELECT p FROM Participante p
            JOIN FETCH p.usuario u
            JOIN FETCH u.persona
            LEFT JOIN FETCH u.perfil
            WHERE p.id.conversacionId = :conversacionId
            """)
    List<Participante> findByIdConversacionIdConUsuario(@Param("conversacionId") UUID conversacionId);

    /**
     * Devuelve los miembros de una conversación excluyendo a un usuario, ordenados por fecha_union ASC.
     * Usado para promover al miembro más antiguo cuando el último admin abandona.
     */
    @Query("""
            SELECT p FROM Participante p
            WHERE p.id.conversacionId = :conversacionId
              AND p.id.usuarioId != :usuarioId
            ORDER BY p.fechaUnion ASC
            """)
    List<Participante> findMasAntiguoExcluyendo(
            @Param("conversacionId") UUID conversacionId,
            @Param("usuarioId") UUID usuarioId,
            Pageable pageable);
}
