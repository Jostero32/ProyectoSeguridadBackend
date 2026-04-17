package com.seguridad.Messenger.mensajes.repository;

import com.seguridad.Messenger.mensajes.model.Reaccion;
import com.seguridad.Messenger.mensajes.model.ReaccionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ReaccionRepository extends JpaRepository<Reaccion, ReaccionId> {

    /**
     * Inserta o actualiza la reacción de un usuario en un mensaje (upsert).
     * Si ya existe una fila para (mensaje_id, usuario_id), reemplaza el emoji y actualiza creada_en.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO reaccion (mensaje_id, usuario_id, emoji, creada_en)
            VALUES (:mensajeId, :usuarioId, :emoji, :ahora)
            ON CONFLICT (mensaje_id, usuario_id)
            DO UPDATE SET emoji = :emoji, creada_en = :ahora
            """, nativeQuery = true)
    void upsert(@Param("mensajeId") UUID mensajeId,
                @Param("usuarioId") UUID usuarioId,
                @Param("emoji") String emoji,
                @Param("ahora") LocalDateTime ahora);

    /**
     * Todas las reacciones de un mensaje (sin JOIN FETCH — para uso en toResponse()).
     */
    @Query("SELECT r FROM Reaccion r WHERE r.id.mensajeId = :mensajeId")
    List<Reaccion> findByMensajeId(@Param("mensajeId") UUID mensajeId);

    /**
     * Todas las reacciones de un mensaje con usuario cargado en la misma query,
     * ordenadas por creada_en ASC — para el endpoint de detalle.
     */
    @Query("SELECT r FROM Reaccion r JOIN FETCH r.usuario WHERE r.id.mensajeId = :mensajeId ORDER BY r.creadaEn ASC")
    List<Reaccion> findByMensajeIdConUsuario(@Param("mensajeId") UUID mensajeId);

    /**
     * Comprueba si un usuario concreto puso un emoji concreto en un mensaje.
     * Usado al construir el resumen de reacciones (campo {@code reaccionaste}).
     */
    @Query("SELECT COUNT(r) > 0 FROM Reaccion r WHERE r.id.mensajeId = :mensajeId AND r.id.usuarioId = :usuarioId AND r.emoji = :emoji")
    boolean existsByIdMensajeIdAndIdUsuarioIdAndEmoji(@Param("mensajeId") UUID mensajeId,
                                                      @Param("usuarioId") UUID usuarioId,
                                                      @Param("emoji") String emoji);
}
