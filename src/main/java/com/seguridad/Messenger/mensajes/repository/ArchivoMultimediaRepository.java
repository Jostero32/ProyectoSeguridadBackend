package com.seguridad.Messenger.mensajes.repository;

import com.seguridad.Messenger.mensajes.model.ArchivoMultimedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArchivoMultimediaRepository extends JpaRepository<ArchivoMultimedia, UUID> {
}
