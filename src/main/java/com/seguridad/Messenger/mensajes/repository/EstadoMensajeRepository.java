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
     * Todos los estados de entrega/lectura de un mensaje (para el remitente).
     */
    List<EstadoMensaje> findByIdMensajeId(UUID mensajeId);

    /**
     * Estados existentes de una lista de mensajes para un usuario concreto.
     * Usado en marcarLeido para detectar qué filas ya existen antes de crear las que faltan.
     */
    @Query("""
            SELECT e FROM EstadoMensaje e
            WHERE e.id.mensajeId IN :mensajeIds
              AND e.id.usuarioId = :usuarioId
            """)
    List<EstadoMensaje> findByMensajeIdsAndUsuarioId(
            @Param("mensajeIds") List<UUID> mensajeIds,
            @Param("usuarioId") UUID usuarioId);

    /**
     * Bulk update: marca como leídos todos los mensajes indicados para un usuario,
     * solo si aún no tienen leido_en asignado.
     */
    @Modifying
    @Query("""
            UPDATE EstadoMensaje e
            SET e.leidoEn = :ahora
            WHERE e.id.mensajeId IN :mensajeIds
              AND e.id.usuarioId = :usuarioId
              AND e.leidoEn IS NULL
            """)
    int marcarLeidoBulk(
            @Param("mensajeIds") List<UUID> mensajeIds,
            @Param("usuarioId") UUID usuarioId,
            @Param("ahora") LocalDateTime ahora);
}
