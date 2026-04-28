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
     * Devuelve solo los IDs de usuario de los participantes de una conversación, excluyendo a uno.
     * Usado por el módulo de mensajería para crear los Estado_Mensaje al enviar un mensaje.
     */
    @Query("""
            SELECT p.id.usuarioId FROM Participante p
            WHERE p.id.conversacionId = :conversacionId
              AND p.id.usuarioId != :excludeId
            """)
    List<UUID> findUsuarioIdsByConversacionExcluyendo(
            @Param("conversacionId") UUID conversacionId,
            @Param("excludeId") UUID excludeId);

    /**
     * Devuelve los IDs de usuario de todos los participantes de una conversación.
     * Usado por el broadcast WebSocket para enviar eventos individualmente a cada
     * participante en su cola personal {@code /user/{id}/queue/eventos}.
     */
    @Query("""
            SELECT p.id.usuarioId FROM Participante p
            WHERE p.id.conversacionId = :conversacionId
            """)
    List<UUID> findUsuarioIdsByConversacionId(@Param("conversacionId") UUID conversacionId);

    /**
     * Devuelve el UUID del otro participante en una conversación INDIVIDUAL
     * (la que tiene exactamente 2 miembros). Usado para validar bloqueo entre
     * los dos usuarios antes de aceptar mensajes.
     * Devuelve {@code Optional.empty()} si no hay otro participante.
     */
    @Query("""
            SELECT p.id.usuarioId FROM Participante p
            WHERE p.id.conversacionId = :conversacionId
              AND p.id.usuarioId != :usuarioId
            """)
    Optional<UUID> findOtroParticipante(
            @Param("conversacionId") UUID conversacionId,
            @Param("usuarioId") UUID usuarioId);

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

    @Query("""
            SELECT p2.id.usuarioId
            FROM Participante p1
            JOIN Participante p2 ON p2.id.conversacionId = p1.id.conversacionId
            JOIN Conversacion c ON c.id = p1.id.conversacionId
            WHERE p1.id.usuarioId = :usuarioId
              AND p2.id.usuarioId != :usuarioId
              AND c.tipo = 'INDIVIDUAL'
            """)
    List<UUID> findUsuariosConConversacionIndividual(@Param("usuarioId") UUID usuarioId);
}
