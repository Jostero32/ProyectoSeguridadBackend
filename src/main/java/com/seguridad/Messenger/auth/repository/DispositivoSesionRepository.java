package com.seguridad.Messenger.auth.repository;

import com.seguridad.Messenger.auth.model.DispositivoSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispositivoSesionRepository extends JpaRepository<DispositivoSesion, UUID> {

    Optional<DispositivoSesion> findByTokenSesion(String tokenSesion);

    List<DispositivoSesion> findByUsuarioId(UUID usuarioId);

    @Async
    @Modifying
    @Transactional
    @Query("UPDATE DispositivoSesion d SET d.ultimoAcceso = :ahora WHERE d.tokenSesion = :token")
    void actualizarUltimoAcceso(@Param("token") String token, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Transactional
    @Query("DELETE FROM DispositivoSesion d WHERE d.tokenSesion = :token")
    void eliminarPorToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM DispositivoSesion d WHERE d.usuario.id = :usuarioId")
    void eliminarPorUsuarioId(@Param("usuarioId") UUID usuarioId);
}
