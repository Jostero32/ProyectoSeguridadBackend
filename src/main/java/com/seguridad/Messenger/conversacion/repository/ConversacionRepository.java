package com.seguridad.Messenger.conversacion.repository;

import com.seguridad.Messenger.conversacion.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversacionRepository extends JpaRepository<Conversacion, UUID> {

    Optional<Conversacion> findByCanalHash(String canalHash);

    /**
     * Carga todas las conversaciones donde el usuario es participante,
     * junto con todos sus miembros y datos de usuario, para evitar N+1.
     */
    @Query("""
            SELECT DISTINCT c FROM Conversacion c
            LEFT JOIN FETCH c.participantes p
            LEFT JOIN FETCH p.usuario u
            LEFT JOIN FETCH u.persona
            LEFT JOIN FETCH u.perfil
            WHERE c.id IN (
                SELECT p2.id.conversacionId FROM Participante p2
                WHERE p2.id.usuarioId = :usuarioId
            )
            ORDER BY c.creadaEn DESC
            """)
    List<Conversacion> findConversacionesConDetalles(@Param("usuarioId") UUID usuarioId);

    /**
     * Carga una conversación con todos sus participantes y datos de usuario.
     */
    @Query("""
            SELECT c FROM Conversacion c
            LEFT JOIN FETCH c.participantes p
            LEFT JOIN FETCH p.usuario u
            LEFT JOIN FETCH u.persona
            LEFT JOIN FETCH u.perfil
            WHERE c.id = :id
            """)
    Optional<Conversacion> findByIdConDetalles(@Param("id") UUID id);
}
