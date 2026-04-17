package com.seguridad.Messenger.mensajes.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.seguridad.Messenger.mensajes.model.Mensaje;

import java.util.List;
import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, UUID> {

    /**
     * Historial paginado de una conversación.
     * Excluye mensajes eliminados para todos; incluye los eliminados solo para sí mismo.
     * Ordenado por creado_en DESC (más recientes primero).
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
     * Filtra mensajes válidos para marcar como leídos:
     * deben pertenecer a la conversación y no haber sido enviados por el usuario.
     */
    @Query("""
            SELECT m FROM Mensaje m
            WHERE m.id IN :mensajeIds
              AND m.conversacion.id = :conversacionId
              AND m.remitenteId != :usuarioId
            """)
    List<Mensaje> findValidosParaLeer(
            @Param("conversacionId") UUID conversacionId,
            @Param("usuarioId") UUID usuarioId,
            @Param("mensajeIds") List<UUID> mensajeIds);

    /**
     * Devuelve solo el remitenteId de un mensaje — usado por el broadcast de estado de entrega
     * para saber a qué usuario enviar la notificación vía /user/queue/notificaciones.
     */
    @Query("SELECT m.remitenteId FROM Mensaje m WHERE m.id = :mensajeId")
    UUID findRemitenteId(@Param("mensajeId") UUID mensajeId);
}
