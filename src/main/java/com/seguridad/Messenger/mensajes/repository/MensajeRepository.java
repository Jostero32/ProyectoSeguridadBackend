package com.seguridad.Messenger.mensajes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.seguridad.Messenger.mensajes.model.Mensaje;

import java.util.Optional;
import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {

    /**
     * Historial paginado de una conversación.
     * Excluye mensajes eliminados para todos. Ordenado por creado_en DESC.
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.conversacion.id = :conversacionId
              AND (m.eliminadoEn IS NULL OR m.eliminadoParaTodos = false)
            ORDER BY m.creadoEn DESC
            """)
    Page<Mensaje> findByConversacion(
            @Param("conversacionId") UUID conversacionId,
            Pageable pageable);

    /**
     * Último mensaje de una conversación — usado para el preview en el listado de chats.
     */
    Optional<Mensaje> findTopByConversacionIdOrderByCreadoEnDesc(UUID conversacionId);

    /**
     * Devuelve solo el remitenteId de un mensaje — usado por el broadcast de estado de entrega.
     */
    @Query("SELECT m.remitenteId FROM Mensaje m WHERE m.id = :mensajeId")
    UUID findRemitenteId(@Param("mensajeId") UUID mensajeId);
}
