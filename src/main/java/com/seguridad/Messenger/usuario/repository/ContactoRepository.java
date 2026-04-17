package com.seguridad.Messenger.usuario.repository;

import com.seguridad.Messenger.usuario.model.Contacto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContactoRepository extends JpaRepository<Contacto, UUID> {

    /**
     * IDs de todos los contactos registrados por un usuario.
     * Usado para filtrar destinatarios de eventos de presencia (privacidad = CONTACTOS).
     */
    @Query("SELECT c.contactoId FROM Contacto c WHERE c.usuarioId = :usuarioId")
    List<UUID> findContactoIdsByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
