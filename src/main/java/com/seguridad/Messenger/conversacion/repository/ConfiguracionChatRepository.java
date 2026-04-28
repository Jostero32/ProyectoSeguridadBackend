package com.seguridad.Messenger.conversacion.repository;

import com.seguridad.Messenger.conversacion.model.ConfiguracionChat;
import com.seguridad.Messenger.conversacion.model.ConfiguracionChatId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfiguracionChatRepository extends JpaRepository<ConfiguracionChat, ConfiguracionChatId> {

    Optional<ConfiguracionChat> findByIdConversacionIdAndIdUsuarioId(UUID conversacionId, UUID usuarioId);

    List<ConfiguracionChat> findByIdUsuarioIdAndArchivadoTrue(UUID usuarioId);
}
