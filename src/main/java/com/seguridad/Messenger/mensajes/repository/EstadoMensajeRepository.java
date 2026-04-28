package com.seguridad.Messenger.mensajes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.seguridad.Messenger.mensajes.model.EstadoMensaje;
import com.seguridad.Messenger.mensajes.model.EstadoMensajeId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EstadoMensajeRepository extends JpaRepository<EstadoMensaje, EstadoMensajeId> {

    /**
     * Todos los estados de entrega/lectura de un mensaje (para mostrar al remitente).
     */
    List<EstadoMensaje> findByIdMensajeId(UUID mensajeId);

    /**
     * IDs de mensajes no leídos de una conversación para un usuario.
     * Usado antes del bulk update para poder emitir los eventos WebSocket.
     */
    @Query("""
            SELECT e.id.mensajeId FROM EstadoMensaje e
            WHERE e.id.usuarioId = :usuarioId
              AND e.leidoEn IS NULL
              AND e.id.mensajeId IN (
                  SELECT m.id FROM Mensaje m
                  WHERE m.conversacion.id = :conversacionId
                    AND m.remitenteId != :usuarioId
                    AND m.eliminadoEn IS NULL
              )
            """)
    List<UUID> findMensajeIdsPendientesDeLectura(
            @Param("conversacionId") UUID conversacionId,
            @Param("usuarioId") UUID usuarioId);

    /**
     * Bulk update: marca como leídos todos los mensajes no leídos de una conversación para un usuario.
     */
    @Modifying
    @Query("""
            UPDATE EstadoMensaje e
            SET e.leidoEn = :ahora
            WHERE e.id.usuarioId = :usuarioId
              AND e.leidoEn IS NULL
              AND e.id.mensajeId IN (
                  SELECT m.id FROM Mensaje m
                  WHERE m.conversacion.id = :conversacionId
                    AND m.remitenteId != :usuarioId
                    AND m.eliminadoEn IS NULL
              )
            """)
    int marcarTodosLeidosPorConversacion(
            @Param("conversacionId") UUID conversacionId,
            @Param("usuarioId") UUID usuarioId,
            @Param("ahora") LocalDateTime ahora);

    /**
     * Cuenta los mensajes no leídos de una conversación para un usuario.
     * Usado en el listado de chats para mostrar el badge de no leídos.
     */
    @Query("""
            SELECT COUNT(e) FROM EstadoMensaje e
            WHERE e.id.usuarioId = :usuarioId
              AND e.leidoEn IS NULL
              AND e.id.mensajeId IN (
                  SELECT m.id FROM Mensaje m
                  WHERE m.conversacion.id = :conversacionId
                    AND m.remitenteId != :usuarioId
                    AND m.eliminadoEn IS NULL
              )
            """)
    long countNoLeidos(
            @Param("conversacionId") UUID conversacionId,
            @Param("usuarioId") UUID usuarioId);
}
